package com.totalhealthdashboard.repository;

import com.totalhealthdashboard.data.remote.FitbitApiService;
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
import com.totalhealthdashboard.data.local.PhysicalHistoryDao;
import com.totalhealthdashboard.data.local.PhysicalHistoryEntry;

public class HealthRepository {

    private static HealthRepository instance;
    private FitbitApiService fitbitApi;
    private JournalDao journalDao;
    private NutritionDao nutritionDao;
    private PhysicalDao physicalDao;
    private PhysicalHistoryDao physicalHistoryDao;
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

    private final MutableLiveData<String> cachedQuote = new MutableLiveData<>();

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

        Retrofit fitbitRetrofit = new Retrofit.Builder()
                .baseUrl("https://api.fitbit.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        fitbitApi = fitbitRetrofit.create(FitbitApiService.class);
    }

    public static synchronized HealthRepository getInstance() {
        if (instance == null) {
            instance = new HealthRepository();
        }
        return instance;
    }

    private String uid() {
        com.google.firebase.auth.FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : "anonymous";
    }

    public void init(Context context) {
        totalCalories.postValue(0);
        totalProtein.postValue(0.0);
        totalCarbs.postValue(0.0);
        totalFat.postValue(0.0);

        AppDatabase db = AppDatabase.getInstance(context);
        journalDao   = db.journalDao();
        nutritionDao = db.nutritionDao();
        physicalDao  = db.physicalDao();
        physicalHistoryDao = db.physicalHistoryDao();
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


    public LiveData<List<NutritionEntry>> getNutritionHistoryForDays(long sevenDaysAgo) {
        return nutritionDao.getEntriesFrom(uid(), sevenDaysAgo);
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

    public LiveData<List<PhysicalHistoryEntry>> getPhysicalHistory() {
        return physicalHistoryDao.getLast7Days(uid());
    }

    public void savePhysicalSnapshot(PhysicalEntry current) {
        executor.execute(() -> {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long startOfDay = cal.getTimeInMillis();
            long weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L);

            PhysicalHistoryEntry existing =
                    physicalHistoryDao.getEntryForDay(uid(), startOfDay);

            // Get goals synchronously
            UserGoals g = userGoalsDao.getGoalsSync(uid());
            if (g == null) g = new UserGoals();

            // Get diet data synchronously
            int caloriesConsumed = nutritionDao.getTotalCaloriesTodaySync(uid(), startOfDay);
            double proteinConsumed = nutritionDao.getTotalProteinTodaySync(uid(), startOfDay);
            double carbsConsumed   = nutritionDao.getTotalCarbsTodaySync(uid(), startOfDay);
            double fatConsumed     = nutritionDao.getTotalFatTodaySync(uid(), startOfDay);

            // Get mental data synchronously
            float moodAvg = journalDao.getAverageMoodSync(uid(), weekAgo);
            int journalCount = journalDao.getEntryCountSync(uid(), weekAgo);

            // Calculate physical score
            int physScore = calculateSnapshotScore(current, g);

            // Calculate diet score
            int dietScore = calculateDietSnapshotScore(
                    caloriesConsumed, proteinConsumed, carbsConsumed, fatConsumed, g);

            // Calculate mental score
            int mentalScore = calculateMentalSnapshotScore(moodAvg, journalCount, g);

            // Overall = average of three categories
            int overallScore = (physScore + dietScore + mentalScore) / 3;

            PhysicalHistoryEntry snapshot = new PhysicalHistoryEntry();
            snapshot.userId          = uid();
            snapshot.date            = startOfDay;
            snapshot.steps           = current.steps;
            snapshot.distanceKm      = current.distanceKm;
            snapshot.caloriesBurned  = current.caloriesBurned;
            snapshot.activeMinutes   = current.activeMinutes;
            snapshot.floors          = current.floors;
            snapshot.heartRate       = current.heartRate;
            snapshot.sleepHours      = current.sleepHours;
            snapshot.sleepScore      = current.sleepScore;
            snapshot.stressScore     = current.stressScore;
            snapshot.dailyScore      = physScore;
            snapshot.overallScore    = overallScore;
            snapshot.caloriesConsumed = caloriesConsumed;
            snapshot.moodScore       = moodAvg;
            snapshot.journalCount    = journalCount;
            snapshot.timestamp       = System.currentTimeMillis();

            if (existing != null) snapshot.id = existing.id;
            physicalHistoryDao.insert(snapshot);
        });
    }

    private int calculateDietSnapshotScore(int calories, double protein,
                                           double carbs, double fat, UserGoals g) {
        int total = 0;
        int count = 0;

        if (g.caloriesEnabled && g.caloriesGoal > 0 && calories > 0) {
            double ratio = (double) calories / g.caloriesGoal;
            int s;
            if (ratio >= 0.9 && ratio <= 1.1) s = 100;
            else if (ratio < 0.9) s = (int)(ratio / 0.9 * 100);
            else s = Math.max(0, (int)(100 - (ratio - 1.1) * 200));
            total += s;
            count++;
        }
        if (g.proteinEnabled && g.proteinGoal > 0 && protein > 0) {
            total += (int) Math.min((protein / g.proteinGoal) * 100, 100);
            count++;
        }
        if (g.carbsEnabled && g.carbsGoal > 0 && carbs > 0) {
            double ratio = carbs / g.carbsGoal;
            int s = ratio >= 0.9 && ratio <= 1.1 ? 100
                    : ratio < 0.9 ? (int)(ratio / 0.9 * 100)
                    : Math.max(0, (int)(100 - (ratio - 1.1) * 200));
            total += s;
            count++;
        }
        if (g.fatEnabled && g.fatGoal > 0 && fat > 0) {
            double ratio = fat / g.fatGoal;
            int s = ratio >= 0.9 && ratio <= 1.1 ? 100
                    : ratio < 0.9 ? (int)(ratio / 0.9 * 100)
                    : Math.max(0, (int)(100 - (ratio - 1.1) * 200));
            total += s;
            count++;
        }

        return count == 0 ? 0 : total / count;
    }

    private int calculateMentalSnapshotScore(float moodAvg, int journalCount, UserGoals g) {
        int total = 0;
        int count = 0;

        if (g.moodEnabled && g.moodGoal > 0 && moodAvg > 0) {
            total += (int) Math.min((moodAvg / g.moodGoal) * 100, 100);
            count++;
        }
        if (g.journalDaysEnabled && g.journalDaysGoal > 0) {
            total += Math.min((journalCount * 100) / g.journalDaysGoal, 100);
            count++;
        }

        return count == 0 ? 0 : total / count;
    }

    private int calculateSnapshotScore(PhysicalEntry e, UserGoals g) {
        int total = 0;
        int count = 0;

        if (g.stepsEnabled && e.steps > 0) {
            int goal = g.stepsGoal > 0 ? g.stepsGoal : 10000;
            total += Math.min((e.steps * 100) / goal, 100);
            count++;
        }
        if (g.caloriesEnabled && e.caloriesBurned > 0) {
            int goal = g.caloriesGoal > 0 ? g.caloriesGoal : 2000;
            total += (int) Math.min((double) e.caloriesBurned / goal * 100, 100);
            count++;
        }
        if (g.activeMinutesEnabled && e.activeMinutes > 0) {
            int goal = g.activeMinutesGoal > 0 ? g.activeMinutesGoal : 30;
            total += Math.min((e.activeMinutes * 100) / goal, 100);
            count++;
        }
        if (g.sleepHoursEnabled && e.sleepHours > 0) {
            double goal = g.sleepHoursGoal > 0 ? g.sleepHoursGoal : 8.0;
            total += (int) Math.min((e.sleepHours / goal) * 100, 100);
            count++;
        }
        if (g.sleepScoreEnabled && e.sleepScore > 0) {
            int goal = g.sleepScoreGoal > 0 ? g.sleepScoreGoal : 8;
            total += Math.min((e.sleepScore * 100) / goal, 100);
            count++;
        }
        if (g.stressEnabled && e.stressScore > 0) {
            total += Math.min((e.stressScore * 100) / 10, 100);
            count++;
        }
        if (g.heartRateEnabled && e.heartRate > 0) {
            int goal = g.heartRateGoal > 0 ? g.heartRateGoal : 70;
            int hrScore = e.heartRate <= goal ? 100
                    : Math.max(0, 100 - (e.heartRate - goal) * 5);
            total += hrScore;
            count++;
        }

        return count == 0 ? 0 : total / count;
    }

    // ─── Fitbit background sync ───────────────────────────────────────────────
    public void syncFitbitInBackground(String accessToken, Runnable onComplete) {
        String bearer = "Bearer " + accessToken;
        String today  = new java.text.SimpleDateFormat("yyyy-MM-dd",
                java.util.Locale.getDefault()).format(new java.util.Date());

        final int[]    steps          = {0};
        final double[] distanceKm     = {0};
        final int[]    caloriesBurned = {0};
        final int[]    activeMinutes  = {0};
        final int[]    floors         = {0};
        final int[]    heartRate      = {0};
        final double[] sleepHours     = {0};
        final int[]    done           = {0};
        final int      total          = 3;

        Runnable checkDone = () -> {
            done[0]++;
            if (done[0] == total) {
                executor.execute(() -> {
                    PhysicalEntry existing = physicalDao.getPhysicalEntrySync(uid());

                    // Check if stored entry is from a previous day
                    // If so, reset all override flags so Fitbit takes over fresh
                    boolean isNewDay = false;
                    if (existing != null && existing.timestamp > 0) {
                        Calendar storedCal = Calendar.getInstance();
                        storedCal.setTimeInMillis(existing.timestamp);
                        Calendar todayCal = Calendar.getInstance();
                        isNewDay = storedCal.get(Calendar.DAY_OF_YEAR)
                                != todayCal.get(Calendar.DAY_OF_YEAR)
                                || storedCal.get(Calendar.YEAR)
                                != todayCal.get(Calendar.YEAR);
                    }

                    // If new day treat as no existing overrides
                    PhysicalEntry ref = (isNewDay || existing == null) ? null : existing;

                    PhysicalEntry entry    = new PhysicalEntry();

                    // Only use Fitbit data if user hasn't overridden that field today
                    entry.steps          = (ref != null && ref.overrideSteps)
                            ? ref.steps : steps[0];
                    entry.distanceKm     = (ref != null && ref.overrideDistance)
                            ? ref.distanceKm : distanceKm[0];
                    entry.caloriesBurned = (ref != null && ref.overrideCalories)
                            ? ref.caloriesBurned : caloriesBurned[0];
                    entry.activeMinutes  = (ref != null && ref.overrideActiveMinutes)
                            ? ref.activeMinutes : activeMinutes[0];
                    entry.heartRate      = (ref != null && ref.overrideHeartRate)
                            ? ref.heartRate : heartRate[0];
                    entry.sleepHours     = (ref != null && ref.overrideSleepHours)
                            ? ref.sleepHours : sleepHours[0];
                    entry.floors         = floors[0];

                    // Always preserve manual-only fields
                    entry.sleepScore     = existing != null ? existing.sleepScore : 0;
                    entry.stressScore    = existing != null ? existing.stressScore : 0;
                    entry.currentHeartRate = 0;

                    // Preserve override flags (cleared if new day)
                    entry.overrideSteps         = ref != null && ref.overrideSteps;
                    entry.overrideDistance      = ref != null && ref.overrideDistance;
                    entry.overrideCalories      = ref != null && ref.overrideCalories;
                    entry.overrideActiveMinutes = ref != null && ref.overrideActiveMinutes;
                    entry.overrideHeartRate     = ref != null && ref.overrideHeartRate;
                    entry.overrideSleepHours    = ref != null && ref.overrideSleepHours;

                    entry.timestamp = System.currentTimeMillis();
                    saveManualPhysicalData(entry);
                    savePhysicalSnapshot(entry);
                    if (onComplete != null) onComplete.run();
                });
            }
        };

        // Activity
        fitbitApi.getTodayActivity(bearer, today).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject summary = response.body().getAsJsonObject("summary");
                        steps[0]          = summary.get("steps").getAsInt();
                        caloriesBurned[0] = summary.get("caloriesOut").getAsInt();
                        floors[0]         = summary.has("floors")
                                ? summary.get("floors").getAsInt() : 0;
                        if (summary.has("activeZoneMinutes")) {
                            JsonObject azm = summary.getAsJsonObject("activeZoneMinutes");
                            activeMinutes[0] = azm.has("totalMinutes")
                                    ? azm.get("totalMinutes").getAsInt() : 0;
                        }
                        if (activeMinutes[0] == 0) {
                            activeMinutes[0] = summary.get("fairlyActiveMinutes").getAsInt()
                                    + summary.get("veryActiveMinutes").getAsInt();
                        }
                        if (summary.has("distances")) {
                            com.google.gson.JsonArray distances =
                                    summary.getAsJsonArray("distances");
                            for (int i = 0; i < distances.size(); i++) {
                                JsonObject d = distances.get(i).getAsJsonObject();
                                if ("total".equals(d.get("activity").getAsString())) {
                                    distanceKm[0] = d.get("distance").getAsDouble();
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e("FITBIT_BG", "Activity error: " + e.getMessage());
                    }
                }
                checkDone.run();
            }
            @Override public void onFailure(Call<JsonObject> call, Throwable t) {
                android.util.Log.e("FITBIT_BG", "Activity failed: " + t.getMessage());
                checkDone.run();
            }
        });

        // Heart rate
        fitbitApi.getTodayHeartRate(bearer, today).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        com.google.gson.JsonArray arr =
                                response.body().getAsJsonArray("activities-heart");
                        if (arr != null && arr.size() > 0) {
                            JsonObject value = arr.get(0).getAsJsonObject()
                                    .getAsJsonObject("value");
                            if (value.has("restingHeartRate")) {
                                heartRate[0] = value.get("restingHeartRate").getAsInt();
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e("FITBIT_BG", "HR error: " + e.getMessage());
                    }
                }
                checkDone.run();
            }
            @Override public void onFailure(Call<JsonObject> call, Throwable t) {
                android.util.Log.e("FITBIT_BG", "HR failed: " + t.getMessage());
                checkDone.run();
            }
        });

        // Sleep
        fitbitApi.getTodaySleep(bearer, today).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject summary = response.body().getAsJsonObject("summary");
                        if (summary != null && summary.has("totalMinutesAsleep")) {
                            int mins = summary.get("totalMinutesAsleep").getAsInt();
                            sleepHours[0] = Math.round((mins / 60.0) * 10.0) / 10.0;
                        }
                    } catch (Exception e) {
                        android.util.Log.e("FITBIT_BG", "Sleep error: " + e.getMessage());
                    }
                }
                checkDone.run();
            }
            @Override public void onFailure(Call<JsonObject> call, Throwable t) {
                android.util.Log.e("FITBIT_BG", "Sleep failed: " + t.getMessage());
                checkDone.run();
            }
        });
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
        if (cachedQuote.getValue() != null) return cachedQuote;

        quoteApi.getTodayQuote().enqueue(new Callback<JsonArray>() {
            @Override
            public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().size() > 0) {
                    JsonObject q = response.body().get(0).getAsJsonObject();
                    cachedQuote.postValue("\"" + q.get("q").getAsString()
                            + "\" — " + q.get("a").getAsString());
                } else {
                    cachedQuote.postValue("Take care of your body. It's the only place you have to live.");
                }
            }
            @Override
            public void onFailure(Call<JsonArray> call, Throwable t) {
                cachedQuote.postValue("Take care of your body. It's the only place you have to live.");
            }
        });
        return cachedQuote;
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