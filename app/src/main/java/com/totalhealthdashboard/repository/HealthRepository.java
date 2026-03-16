package com.totalhealthdashboard.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.totalhealthdashboard.data.local.AppDatabase;
import com.totalhealthdashboard.data.local.JournalEntry;
import com.totalhealthdashboard.data.local.JournalDao;
import com.totalhealthdashboard.data.models.FitbitData;
import com.totalhealthdashboard.data.models.NutritionData;
import com.totalhealthdashboard.data.remote.NutritionApiService;
import com.totalhealthdashboard.data.remote.NutritionResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import retrofit2.http.GET;

public class HealthRepository {
    private static HealthRepository instance;
    private JournalDao journalDao;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private NutritionApiService nutritionApi;
    private QuoteApiService quoteApi;

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

    public void init(Context context) {
        if (journalDao == null) {
            journalDao = AppDatabase.getInstance(context).journalDao();
        }
    }

    public void saveJournalEntry(String content, int moodScore) {
        executor.execute(() -> {
            JournalEntry entry = new JournalEntry(
                content,
                System.currentTimeMillis(),
                moodScore
            );
            journalDao.insert(entry);
        });
    }

    public LiveData<Integer> getEntryCountThisWeek() {
        long weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L);
        return journalDao.getEntryCountThisWeek(weekAgo);
    }

    public LiveData<Float> getAverageMoodThisWeek() {
        long weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L);
        return journalDao.getAverageMoodThisWeek(weekAgo);
    }

    public LiveData<JournalEntry> getLatestEntry() {
        return journalDao.getLatestEntry();
    }

    public LiveData<NutritionData> searchFood(String query) {
        MutableLiveData<NutritionData> data = new MutableLiveData<>();
        if (query.equalsIgnoreCase("apple")) {
            data.setValue(new NutritionData("Apple", 52, 0.3, 14, 0.2));
        } else if (query.equalsIgnoreCase("chicken")) {
            data.setValue(new NutritionData("Chicken Breast", 165, 31, 0, 3.6));
        } else {
            nutritionApi.getProductByBarcode(query).enqueue(new Callback<NutritionResponse>() {
                @Override
                public void onResponse(Call<NutritionResponse> call, Response<NutritionResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().getProduct() != null) {
                        NutritionResponse.Product p = response.body().getProduct();
                        NutritionResponse.Nutriments n = p.getNutriments();
                        data.setValue(new NutritionData(
                            p.getProductName(),
                            n.getCalories(),
                            n.getProtein(),
                            n.getCarbs(),
                            n.getFat()
                        ));
                    } else {
                        data.setValue(null);
                    }
                }
                @Override
                public void onFailure(Call<NutritionResponse> call, Throwable t) {
                    data.setValue(null);
                }
            });
        }
        return data;
    }

    public LiveData<FitbitData> getFitbitData() {
        MutableLiveData<FitbitData> data = new MutableLiveData<>();
        data.setValue(new FitbitData(7542, 72, 420, 38, 7.3));
        return data;
    }

    public LiveData<String> getWellnessQuote() {
        MutableLiveData<String> quoteData = new MutableLiveData<>();
        quoteApi.getTodayQuote().enqueue(new Callback<JsonArray>() {
            @Override
            public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
                if (response.isSuccessful() && response.body() != null && response.body().size() > 0) {
                    JsonObject firstQuote = response.body().get(0).getAsJsonObject();
                    String q = firstQuote.get("q").getAsString();
                    String a = firstQuote.get("a").getAsString();
                    quoteData.setValue("\"" + q + "\" — " + a);
                } else {
                    quoteData.setValue("The only way to do great work is to love what you do. — Steve Jobs");
                }
            }
            @Override
            public void onFailure(Call<JsonArray> call, Throwable t) {
                quoteData.setValue("Believe you can and you're halfway there. — Theodore Roosevelt");
            }
        });
        return quoteData;
    }

    private interface QuoteApiService {
        @GET("api/today")
        Call<JsonArray> getTodayQuote();
    }
}