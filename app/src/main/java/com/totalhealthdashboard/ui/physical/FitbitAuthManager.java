package com.totalhealthdashboard.ui.physical;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

public class FitbitAuthManager {

    private static final String PREFS_NAME    = "fitbit_prefs";
    private static final String KEY_TOKEN     = "access_token";
    private static final String KEY_REFRESH   = "refresh_token";
    private static final String KEY_EXPIRES   = "expires_at";
    private static final String KEY_USER_ID   = "fitbit_user_id";

    private final SharedPreferences prefs;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public FitbitAuthManager(Context context) {
        prefs        = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        clientId     = context.getString(com.totalhealthdashboard.R.string.fitbit_client_id);
        clientSecret = context.getString(com.totalhealthdashboard.R.string.fitbit_client_secret);
        redirectUri  = context.getString(com.totalhealthdashboard.R.string.fitbit_redirect_uri);
    }

    public String getAuthUrl() {
        return "https://www.fitbit.com/oauth2/authorize"
                + "?response_type=code"
                + "&client_id=" + clientId
                + "&redirect_uri=" + android.net.Uri.encode(redirectUri)
                + "&scope=activity+heartrate+sleep+profile"
                + "&expires_in=604800";
    }

    public String getBasicAuthHeader() {
        String credentials = clientId + ":" + clientSecret;
        return "Basic " + Base64.encodeToString(
                credentials.getBytes(), Base64.NO_WRAP);
    }

    public String getRedirectUri()  { return redirectUri; }
    public String getClientId()     { return clientId; }

    public void saveTokens(String accessToken, String refreshToken, long expiresIn) {
        prefs.edit()
                .putString(KEY_TOKEN, accessToken)
                .putString(KEY_REFRESH, refreshToken)
                .putLong(KEY_EXPIRES, System.currentTimeMillis() + (expiresIn * 1000))
                .apply();
    }

    public String getAccessToken()  { return prefs.getString(KEY_TOKEN, null); }
    public String getRefreshToken() { return prefs.getString(KEY_REFRESH, null); }

    public boolean isConnected() {
        return getAccessToken() != null;
    }

    public boolean isTokenExpired() {
        long expiresAt = prefs.getLong(KEY_EXPIRES, 0);
        return System.currentTimeMillis() > expiresAt;
    }

    public void disconnect() {
        prefs.edit().clear().apply();
    }
}