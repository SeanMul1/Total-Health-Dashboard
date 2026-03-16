package com.totalhealthdashboard.data.remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface NutritionApiService {
    @GET("api/v0/product/{barcode}.json")
    Call<NutritionResponse> getProductByBarcode(@Path("barcode") String barcode);
}