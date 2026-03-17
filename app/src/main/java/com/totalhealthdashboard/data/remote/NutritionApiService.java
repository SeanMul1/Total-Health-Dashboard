package com.totalhealthdashboard.data.remote;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface NutritionApiService {
    @GET("api/v0/product/{barcode}.json")
    Call<NutritionResponse> getProductByBarcode(@Path("barcode") String barcode);

    @GET("cgi/search.pl?action=process&json=true&page_size=10&fields=product_name,nutriments,image_url&sort_by=unique_scans_n&action_type=simple")
    Call<JsonObject> searchProduct(@Query("search_terms") String query);
}