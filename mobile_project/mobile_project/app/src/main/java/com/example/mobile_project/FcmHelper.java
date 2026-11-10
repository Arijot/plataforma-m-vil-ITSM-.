package com.example.mobile_project;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.example.mobile_project.shared.clients.ApiClient;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FcmHelper {

    private static final String PREFS_NAME = "SoporteClinicaPrefs";

    /** Llamar después del login y al abrir TicketsActivity (si ya registras el token en el login, no hace falta) */
    public static void registrarToken(Context context) {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token ->
                enviarTokenAlServidor(context.getApplicationContext(), token));
    }

    public static void enviarTokenAlServidor(Context context, String fcmToken) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String access = prefs.getString("access_token", null);
        if (access == null || fcmToken == null) return;   // sin sesión: se enviará en el próximo login

        Map<String, Object> body = new HashMap<>();
        body.put("token", fcmToken);
        body.put("modelo", Build.MANUFACTURER + " " + Build.MODEL);

        ApiClient.getService().guardarTokenFcm("Bearer " + access, body).enqueue(new Callback<ResponseBody>() {
            @Override public void onResponse(Call<ResponseBody> c, Response<ResponseBody> r) { }
            @Override public void onFailure(Call<ResponseBody> c, Throwable t) { }
        });
    }
}