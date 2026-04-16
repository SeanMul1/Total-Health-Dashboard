package com.totalhealthdashboard.data.remote;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface FitbitApiService {

    @FormUrlEncoded
    @POST("oauth2/token")
    Call<JsonObject> exchangeToken(
            @Header("Authorization") String basicAuth,
            @Field("code") String code,
            @Field("grant_type") String grantType,
            @Field("redirect_uri") String redirectUri
    );

    @FormUrlEncoded
    @POST("oauth2/token")
    Call<JsonObject> refreshToken(
            @Header("Authorization") String basicAuth,
            @Field("refresh_token") String refreshToken,
            @Field("grant_type") String grantType
    );

    @GET("1/user/-/activities/date/{date}.json")
    Call<JsonObject> getTodayActivity(
            @Header("Authorization") String bearerToken,
            @Path("date") String date
    );

    @GET("1/user/-/activities/heart/date/{date}/1d.json")
    Call<JsonObject> getTodayHeartRate(
            @Header("Authorization") String bearerToken,
            @Path("date") String date
    );

    @GET("1.2/user/-/sleep/date/{date}.json")
    Call<JsonObject> getTodaySleep(
            @Header("Authorization") String bearerToken,
            @Path("date") String date
    );
}