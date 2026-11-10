package com.example.mobile_project.shared.session;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.example.mobile_project.MainActivity;
import com.example.mobile_project.shared.clients.ApiClient;
import com.example.mobile_project.user.User;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SessionManager {

    public static final String PREFS_NAME = "SoporteClinicaPrefs";
    public static final String KEY_TOKEN = "access_token";
    public static final String KEY_USER = "user_data";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    /** "Bearer xxx" o null si no hay sesión (Retrofit omite el header si es null). */
    public String getBearer() {
        String t = getToken();
        return (t == null || t.isEmpty()) ? null : "Bearer " + t;
    }

    public boolean haySesion() {
        return getBearer() != null;
    }

    public User getUser() {
        String json = prefs.getString(KEY_USER, null);
        if (json == null) return null;
        try {
            return new Gson().fromJson(json, User.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

    /** Cierra sesión en el backend y SIEMPRE localmente (aunque falle la red). */
    public void cerrarSesion(Activity activity) {
        String bearer = getBearer();   // capturar antes de limpiar
        if (bearer == null) {
            salirLocal(activity, "Sesión cerrada correctamente");
            return;
        }
        ApiClient.getService().logout(bearer).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> response) {
                salirLocal(activity, "Sesión cerrada correctamente");
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {
                salirLocal(activity, "Sesión cerrada correctamente");
            }
        });
    }

    /** Token vencido/revocado (401): limpia y manda al login. */
    public static void expirar(Context context) {
        new SessionManager(context).salirLocal(context, "Tu sesión expiró. Inicia sesión nuevamente");
    }

    private void salirLocal(Context context, String mensaje) {
        clear();
        Toast.makeText(context.getApplicationContext(), mensaje, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
        if (context instanceof Activity) ((Activity) context).finish();
    }
}
