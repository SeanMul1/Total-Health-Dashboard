package com.totalhealthdashboard.data.remote;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface UsdaApiService {
    @GET("fdc/v1/foods/search")
    Call<JsonObject> searchFoods(
        @Query("api_key") String apiKey,
        @Query("query") String query,
        @Query("pageSize") int pageSize,
        @Query("dataType") String dataType
    );
}