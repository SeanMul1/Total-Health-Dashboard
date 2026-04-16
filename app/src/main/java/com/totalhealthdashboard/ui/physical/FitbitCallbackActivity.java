package com.totalhealthdashboard.ui.physical;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.JsonObject;
import com.totalhealthdashboard.repository.HealthRepository;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.totalhealthdashboard.data.remote.FitbitApiService;

public class FitbitCallbackActivity extends AppCompatActivity {

    private static final String TAG = "FITBIT";
    private FitbitAuthManager authManager;
    private FitbitApiService fitbitApi;
    private FitbitApiService tokenApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setGravity(android.view.Gravity.CENTER);
        layout.setBackgroundColor(0xFFFAFAFA);

        TextView tvStatus = new TextView(this);
        tvStatus.setText("Connecting to Fitbit...");
        tvStatus.setTextSize(18);
        tvStatus.setTextColor(0xFF1A1A1A);
        tvStatus.setGravity(android.view.Gravity.CENTER);
        layout.addView(tvStatus);

        setContentView(layout);

        authManager = new FitbitAuthManager(this);

        tokenApi = new Retrofit.Builder()
                .baseUrl("https://api.fitbit.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(FitbitApiService.class);

        fitbitApi = tokenApi;

        Uri data = getIntent().getData();
        if (data != null && data.toString().startsWith("totalhealthdashboard://callback")) {
            String code  = data.getQueryParameter("code");
            String error = data.getQueryParameter("error");

            Log.d(TAG, "Callback received. code=" + (code != null ? code.substring(0, 10) + "..." : "null")
                    + " error=" + error);

            if (error != null) {
                tvStatus.setText("Fitbit connection cancelled");
                finishAfterDelay(2000);
                return;
            }

            if (code != null) {
                tvStatus.setText("Authorising...");
                exchangeCodeForToken(code, tvStatus);
            }
        } else {
            Log.w(TAG, "No callback data found — finishing");
            finish();
        }
    }

    private void exchangeCodeForToken(String code, TextView tvStatus) {
        Log.d(TAG, "Exchanging code for token...");

        tokenApi.exchangeToken(
                authManager.getBasicAuthHeader(),
                code,
                "authorization_code",
                authManager.getRedirectUri()
        ).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                Log.d(TAG, "Token response code: " + response.code());

                if (!response.isSuccessful()) {
                    try {
                        String errorBody = response.errorBody() != null
                                ? response.errorBody().string() : "null";
                        Log.e(TAG, "Token error body: " + errorBody);
                    } catch (Exception e) {
                        Log.e(TAG, "Could not read error body: " + e.getMessage());
                    }
                    tvStatus.setText("Authorisation failed (code " + response.code() + ")");
                    finishAfterDelay(2000);
                    return;
                }

                if (response.body() == null) {
                    Log.e(TAG, "Token response body is null");
                    tvStatus.setText("Authorisation failed — empty response");
                    finishAfterDelay(2000);
                    return;
                }

                try {
                    JsonObject body   = response.body();
                    String accessToken  = body.get("access_token").getAsString();
                    String refreshToken = body.get("refresh_token").getAsString();
                    long expiresIn      = body.get("expires_in").getAsLong();

                    Log.d(TAG, "Token exchange success. Token starts: "
                            + accessToken.substring(0, 10) + "...");

                    authManager.saveTokens(accessToken, refreshToken, expiresIn);
                    tvStatus.setText("Fetching your Fitbit data...");
                    fetchFitbitData(accessToken, tvStatus);
                } catch (Exception e) {
                    Log.e(TAG, "Token parse error: " + e.getMessage());
                    tvStatus.setText("Failed to read token response");
                    finishAfterDelay(2000);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Token exchange network failure: " + t.getMessage());
                tvStatus.setText("Connection error: " + t.getMessage());
                finishAfterDelay(2000);
            }
        });
    }

    private void fetchFitbitData(String accessToken, TextView tvStatus) {
        String bearer = "Bearer " + accessToken;

        // Format today's date as YYYY-MM-DD for Fitbit API
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date());

        Log.d(TAG, "Fetching Fitbit data for date: " + today);

        final int[] steps          = {0};
        final double[] distanceKm  = {0};
        final int[] caloriesBurned = {0};
        final int[] activeMinutes  = {0};
        final int[] heartRate      = {0};
        final double[] sleepHours  = {0};
        final int[] callsCompleted = {0};
        final int totalCalls       = 3;

        Runnable checkDone = () -> {
            callsCompleted[0]++;
            Log.d(TAG, "Call completed " + callsCompleted[0] + "/" + totalCalls);
            if (callsCompleted[0] == totalCalls) {
                Log.d(TAG, "All calls done. steps=" + steps[0]
                        + " cals=" + caloriesBurned[0]
                        + " active=" + activeMinutes[0]
                        + " hr=" + heartRate[0]
                        + " sleep=" + sleepHours[0]);
                saveAndFinish(steps[0], distanceKm[0], caloriesBurned[0],
                        activeMinutes[0], heartRate[0], sleepHours[0], tvStatus);
            }
        };

        // 1 — Activity
        fitbitApi.getTodayActivity(bearer, today).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                Log.d(TAG, "Activity response code: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject summary = response.body().getAsJsonObject("summary");
                        steps[0]          = summary.get("steps").getAsInt();
                        caloriesBurned[0] = summary.get("caloriesOut").getAsInt();
                        activeMinutes[0]  = summary.get("fairlyActiveMinutes").getAsInt()
                                + summary.get("veryActiveMinutes").getAsInt();
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
                        Log.d(TAG, "Activity parsed: steps=" + steps[0]
                                + " cals=" + caloriesBurned[0]
                                + " active=" + activeMinutes[0]);
                    } catch (Exception e) {
                        Log.e(TAG, "Activity parse error: " + e.getMessage());
                    }
                } else {
                    try {
                        Log.e(TAG, "Activity error body: "
                                + (response.errorBody() != null
                                ? response.errorBody().string() : "null"));
                    } catch (Exception e) { Log.e(TAG, "Could not read activity error body"); }
                }
                checkDone.run();
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Activity call failed: " + t.getMessage());
                checkDone.run();
            }
        });

        // 2 — Heart rate
        fitbitApi.getTodayHeartRate(bearer, today).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                Log.d(TAG, "Heart rate response code: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        com.google.gson.JsonArray arr =
                                response.body().getAsJsonArray("activities-heart");
                        if (arr != null && arr.size() > 0) {
                            JsonObject hrData = arr.get(0).getAsJsonObject()
                                    .getAsJsonObject("value");
                            if (hrData.has("restingHeartRate")) {
                                heartRate[0] = hrData.get("restingHeartRate").getAsInt();
                                Log.d(TAG, "Heart rate parsed: " + heartRate[0]);
                            } else {
                                Log.w(TAG, "No restingHeartRate in response");
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Heart rate parse error: " + e.getMessage());
                    }
                } else {
                    try {
                        Log.e(TAG, "Heart rate error body: "
                                + (response.errorBody() != null
                                ? response.errorBody().string() : "null"));
                    } catch (Exception e) { Log.e(TAG, "Could not read HR error body"); }
                }
                checkDone.run();
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Heart rate call failed: " + t.getMessage());
                checkDone.run();
            }
        });

        // 3 — Sleep
        fitbitApi.getTodaySleep(bearer, today).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                Log.d(TAG, "Sleep response code: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject summary = response.body().getAsJsonObject("summary");
                        if (summary != null && summary.has("totalMinutesAsleep")) {
                            int totalMinutes = summary.get("totalMinutesAsleep").getAsInt();
                            sleepHours[0] = Math.round((totalMinutes / 60.0) * 10.0) / 10.0;
                            Log.d(TAG, "Sleep parsed: " + sleepHours[0] + " hours");
                        } else {
                            Log.w(TAG, "No totalMinutesAsleep in sleep summary");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Sleep parse error: " + e.getMessage());
                    }
                } else {
                    try {
                        Log.e(TAG, "Sleep error body: "
                                + (response.errorBody() != null
                                ? response.errorBody().string() : "null"));
                    } catch (Exception e) { Log.e(TAG, "Could not read sleep error body"); }
                }
                checkDone.run();
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Sleep call failed: " + t.getMessage());
                checkDone.run();
            }
        });
    }

    private void saveAndFinish(int steps, double distanceKm, int caloriesBurned,
                               int activeMinutes, int heartRate, double sleepHours,
                               TextView tvStatus) {
        runOnUiThread(() -> {
            Log.d(TAG, "Saving to Room...");
            HealthRepository repo = HealthRepository.getInstance();
            repo.init(this);

            com.totalhealthdashboard.data.local.PhysicalEntry entry =
                    new com.totalhealthdashboard.data.local.PhysicalEntry();
            entry.steps          = steps;
            entry.distanceKm     = distanceKm;
            entry.caloriesBurned = caloriesBurned;
            entry.activeMinutes  = activeMinutes;
            entry.heartRate      = heartRate;
            entry.sleepHours     = sleepHours;
            entry.sleepScore     = 0;
            entry.stressScore    = 0;
            entry.timestamp      = System.currentTimeMillis();

            repo.saveManualPhysicalData(entry);
            Log.d(TAG, "Saved successfully");

            tvStatus.setText("Fitbit synced successfully!");
            Toast.makeText(this, "Fitbit data synced", Toast.LENGTH_LONG).show();
            finishAfterDelay(1500);
        });
    }

    private void finishAfterDelay(long delayMs) {
        new android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed(() -> {
                    Intent intent = new Intent(this,
                            com.totalhealthdashboard.ui.MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                }, delayMs);
    }
}