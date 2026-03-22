package com.totalhealthdashboard.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.LruCache;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.totalhealthdashboard.data.local.AppDatabase;
import com.totalhealthdashboard.data.local.JournalDao;
import com.totalhealthdashboard.data.local.JournalEntry;
import com.totalhealthdashboard.data.local.NutritionDao;
import com.totalhealthdashboard.data.local.NutritionEntry;
import com.totalhealthdashboard.data.local.PhysicalDao;
import com.totalhealthdashboard.data.local.PhysicalEntry;
import com.totalhealthdashboard.data.local.UserGoals;
import com.totalhealthdashboard.data.local.UserGoalsDao;
import com.totalhealthdashboard.data.models.FitbitData;
import com.totalhealthdashboard.data.models.NutritionData;
import com.totalhealthdashboard.data.remote.NutritionApiService;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;

public class HealthRepository {

    private static HealthRepository instance;
    private JournalDao journalDao;
    private NutritionDao nutritionDao;
    private PhysicalDao physicalDao;
    private UserGoalsDao userGoalsDao;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private NutritionApiService nutritionApi;
    private QuoteApiService quoteApi;

    private final LruCache<String, NutritionData> searchCache = new LruCache<>(10);
    private final MutableLiveData<Integer> totalCalories = new MutableLiveData<>(0);
    private final MutableLiveData<Double> totalProtein   = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> totalCarbs     = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> totalFat       = new MutableLiveData<>(0.0);
    private final MutableLiveData<List<NutritionData>> frequentFoods =
            new MutableLiveData<>(new ArrayList<>());

    private SharedPreferences prefs;
    private final Gson gson = new Gson();

    private HealthRepository() {
        Retrofit nutritionRetrofit = new Retrofit.Builder()
                .baseUrl("https://world.openfoodfacts.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        nutritionApi = nutritionRetrofit.create(NutritionApiService.class);

        Retrofit quoteRetrofit = new Retrofit.Builder()
                .baseUrl("https://zenquotes.io/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        quoteApi = quoteRetrofit.create(QuoteApiService.class);
    }

    public static synchronized HealthRepository getInstance() {
        if (instance == null) {
            instance = new HealthRepository();
        }
        return instance;
    }

    // Returns current Firebase UID — never null safe fallback to "anonymous"
    private String uid() {
        com.google.firebase.auth.FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : "anonymous";
    }

    public void init(Context context) {
        // Reset in-memory totals for this user session
        totalCalories.postValue(0);
        totalProtein.postValue(0.0);
        totalCarbs.postValue(0.0);
        totalFat.postValue(0.0);

        AppDatabase db = AppDatabase.getInstance(context);
        journalDao   = db.journalDao();
        nutritionDao = db.nutritionDao();
        physicalDao  = db.physicalDao();
        userGoalsDao = db.userGoalsDao();
        prefs = context.getSharedPreferences("health_prefs_" + uid(),
                Context.MODE_PRIVATE);
        loadFrequentFoods();
    }

    // ─── Nutrition totals ─────────────────────────────────────────────────────
    public LiveData<Integer> getTotalCalories() { return totalCalories; }
    public LiveData<Double> getTotalProtein()   { return totalProtein; }
    public LiveData<Double> getTotalCarbs()     { return totalCarbs; }
    public LiveData<Double> getTotalFat()       { return totalFat; }
    public LiveData<List<NutritionData>> getFrequentFoods() { return frequentFoods; }

    public LiveData<Double> getTotalProteinToday() {
        return nutritionDao.getTotalProteinToday(uid(), getStartOfDay());
    }

    public LiveData<Double> getTotalCarbsToday() {
        return nutritionDao.getTotalCarbsToday(uid(), getStartOfDay());
    }

    public LiveData<Double> getTotalFatToday() {
        return nutritionDao.getTotalFatToday(uid(), getStartOfDay());
    }

    // ─── Food log ─────────────────────────────────────────────────────────────
    public void addFoodToLog(NutritionData data) {
        String userId = uid();
        executor.execute(() -> {
            NutritionEntry entry = new NutritionEntry(
                    userId, data.getFoodName(), data.getCalories(),
                    data.getProtein(), data.getCarbs(), data.getFat(),
                    System.currentTimeMillis()
            );
            nutritionDao.insert(entry);
            totalCalories.postValue((totalCalories.getValue() != null
                    ? totalCalories.getValue() : 0) + data.getCalories());
            totalProtein.postValue((totalProtein.getValue() != null
                    ? totalProtein.getValue() : 0.0) + data.getProtein());
            totalCarbs.postValue((totalCarbs.getValue() != null
                    ? totalCarbs.getValue() : 0.0) + data.getCarbs());
            totalFat.postValue((totalFat.getValue() != null
                    ? totalFat.getValue() : 0.0) + data.getFat());
            saveToFrequent(data);
        });
    }

    public void removeFoodFromLog(NutritionEntry entry) {
        executor.execute(() -> {
            nutritionDao.delete(entry);
            totalCalories.postValue(Math.max(0,
                    (totalCalories.getValue() != null ? totalCalories.getValue() : 0)
                            - entry.calories));
            totalProtein.postValue(Math.max(0.0,
                    (totalProtein.getValue() != null ? totalProtein.getValue() : 0.0)
                            - entry.protein));
            totalCarbs.postValue(Math.max(0.0,
                    (totalCarbs.getValue() != null ? totalCarbs.getValue() : 0.0)
                            - entry.carbs));
            totalFat.postValue(Math.max(0.0,
                    (totalFat.getValue() != null ? totalFat.getValue() : 0.0)
                            - entry.fat));
        });
    }

    private void saveToFrequent(NutritionData data) {
        String json = prefs.getString("frequent_foods", "[]");
        Type type = new TypeToken<List<NutritionData>>(){}.getType();
        List<NutritionData> list = gson.fromJson(json, type);
        list.removeIf(item -> item.getFoodName().equalsIgnoreCase(data.getFoodName()));
        list.add(0, data);
        if (list.size() > 10) list = new ArrayList<>(list.subList(0, 10));
        prefs.edit().putString("frequent_foods", gson.toJson(list)).apply();
        frequentFoods.postValue(new ArrayList<>(list.subList(0, Math.min(list.size(), 5))));
    }

    private void loadFrequentFoods() {
        String json = prefs.getString("frequent_foods", "[]");
        Type type = new TypeToken<List<NutritionData>>(){}.getType();
        List<NutritionData> list = gson.fromJson(json, type);
        frequentFoods.postValue(new ArrayList<>(list.subList(0, Math.min(list.size(), 5))));
    }

    // ─── Food search ──────────────────────────────────────────────────────────
    public LiveData<NutritionData> searchFood(String query) {
        MutableLiveData<NutritionData> data = new MutableLiveData<>();
        String cleanQuery = query.toLowerCase().trim();

        NutritionData cached = searchCache.get(cleanQuery);
        if (cached != null) {
            data.setValue(cached);
            return data;
        }

        nutritionApi.searchProduct(query).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        com.google.gson.JsonArray products =
                                response.body().getAsJsonArray("products");
                        if (products != null && products.size() > 0) {
                            for (int i = 0; i < products.size(); i++) {
                                JsonObject p = products.get(i).getAsJsonObject();
                                if (!p.has("nutriments")) continue;
                                JsonObject n = p.getAsJsonObject("nutriments");
                                if (!n.has("energy-kcal_100g") && !n.has("energy_100g")) continue;
                                String name = p.has("product_name")
                                        ? p.get("product_name").getAsString() : query;
                                int cal = n.has("energy-kcal_100g")
                                        ? n.get("energy-kcal_100g").getAsInt()
                                        : (int)(n.get("energy_100g").getAsDouble() / 4.184);
                                double prot = n.has("proteins_100g")
                                        ? n.get("proteins_100g").getAsDouble() : 0;
                                double carb = n.has("carbohydrates_100g")
                                        ? n.get("carbohydrates_100g").getAsDouble() : 0;
                                double fat = n.has("fat_100g")
                                        ? n.get("fat_100g").getAsDouble() : 0;
                                NutritionData result = new NutritionData(name, cal, prot, carb, fat);
                                searchCache.put(cleanQuery, result);
                                data.postValue(result);
                                return;
                            }
                        }
                    }
                    data.postValue(getFallbackNutrition(query));
                } catch (Exception e) {
                    data.postValue(getFallbackNutrition(query));
                }
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                data.postValue(getFallbackNutrition(query));
            }
        });
        return data;
    }

    private NutritionData getFallbackNutrition(String query) {
        String q = query.toLowerCase().trim();
        if (q.contains("pasta"))   return new NutritionData(query, 131, 5, 25, 1);
        if (q.contains("chicken")) return new NutritionData(query, 165, 31, 0, 4);
        if (q.contains("apple"))   return new NutritionData(query, 52, 0, 14, 0);
        return new NutritionData(query + " (est.)", 150, 8, 20, 5);
    }

    // ─── Journal ──────────────────────────────────────────────────────────────
    public void saveJournalEntry(String content, int moodScore) {
        String userId = uid();
        executor.execute(() -> {
            JournalEntry entry = new JournalEntry(
                    userId, content, System.currentTimeMillis(), moodScore);
            journalDao.insert(entry);
        });
    }

    public LiveData<Integer> getEntryCountThisWeek() {
        return journalDao.getEntryCountThisWeek(uid(),
                System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L));
    }

    public LiveData<Float> getAverageMoodThisWeek() {
        return journalDao.getAverageMoodThisWeek(uid(),
                System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L));
    }

    public LiveData<JournalEntry> getLatestEntry() {
        return journalDao.getLatestEntry(uid());
    }

    public void deleteJournalEntry(JournalEntry entry) {
        executor.execute(() -> journalDao.delete(entry));
    }

    public LiveData<List<JournalEntry>> getAllJournalEntries() {
        return journalDao.getRecentEntries(uid(), 5);
    }

    // ─── Nutrition entries ────────────────────────────────────────────────────
    public LiveData<List<NutritionEntry>> getNutritionEntriesForToday() {
        return nutritionDao.getEntriesForToday(uid(), getStartOfDay());
    }

    public LiveData<Integer> getTotalCaloriesToday() {
        return nutritionDao.getTotalCaloriesToday(uid(), getStartOfDay());
    }

    // ─── Physical ─────────────────────────────────────────────────────────────
    public LiveData<FitbitData> getFitbitData() {
        MutableLiveData<FitbitData> data = new MutableLiveData<>();
        physicalDao.getPhysicalEntry(uid()).observeForever(entry -> {
            if (entry != null) {
                data.postValue(new FitbitData(entry));
            } else {
                data.postValue(new FitbitData());
            }
        });
        return data;
    }

    public LiveData<PhysicalEntry> getPhysicalEntry() {
        return physicalDao.getPhysicalEntry(uid());
    }

    public void saveManualPhysicalData(PhysicalEntry entry) {
        entry.userId = uid();
        executor.execute(() -> physicalDao.insertOrUpdate(entry));
    }

    // ─── Goals ────────────────────────────────────────────────────────────────
    public LiveData<UserGoals> getUserGoals() {
        return userGoalsDao.getGoals(uid());
    }

    public void saveGoals(UserGoals goals) {
        goals.userId = uid();
        executor.execute(() -> userGoalsDao.insertOrUpdate(goals));
    }

    public boolean hasGoalsBeenSet(Context context) {
        return AppDatabase.getInstance(context)
                .userGoalsDao().getGoalsSync(uid()) != null;
    }

    // ─── Wellness quote ───────────────────────────────────────────────────────
    public LiveData<String> getWellnessQuote() {
        MutableLiveData<String> quoteData = new MutableLiveData<>();
        quoteApi.getTodayQuote().enqueue(new Callback<JsonArray>() {
            @Override
            public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().size() > 0) {
                    JsonObject q = response.body().get(0).getAsJsonObject();
                    quoteData.setValue("\"" + q.get("q").getAsString()
                            + "\" — " + q.get("a").getAsString());
                }
            }
            @Override public void onFailure(Call<JsonArray> call, Throwable t) {}
        });
        return quoteData;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    private long getStartOfDay() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private interface QuoteApiService {
        @GET("api/today")
        Call<JsonArray> getTodayQuote();
    }
}