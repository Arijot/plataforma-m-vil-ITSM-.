package com.example.mobile_project;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.example.mobile_project.shared.clients.ApiClient;

import java.util.Map;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FooterHelper {

    private static final String PREFS_NAME = "SoporteClinicaPrefs";
    private static final String KEY_ROL_ID = "rol_id";
    private static final String KEY_TOKEN = "access_token";

    private static final int ROL_USUARIO = 1;
    private static final int ROL_TECNICO = 2;
    private static final int ROL_ADMIN_TI = 3;

    public static void setupFooter(Activity activity, View rootView) {
        Context context = rootView.getContext();

        // Vincular los contenedores
        LinearLayout btnNavNotifications = rootView.findViewById(R.id.btnNavNotifications);
        LinearLayout btnNavNewTicket = rootView.findViewById(R.id.btnNavNewTicket);
        LinearLayout btnNavSlaDashboard = rootView.findViewById(R.id.btnNavSlaDashboard);
        LinearLayout btnNavUsers = rootView.findViewById(R.id.btnNavUsers);
        LinearLayout btnNavProfile = rootView.findViewById(R.id.btnNavProfile);
        LinearLayout btnNavTickets = rootView.findViewById(R.id.btnNavTickets);

        // Ocultar todos por defecto
        if (btnNavNotifications != null) btnNavNotifications.setVisibility(View.GONE);
        if (btnNavNewTicket != null) btnNavNewTicket.setVisibility(View.GONE);
        if (btnNavSlaDashboard != null) btnNavSlaDashboard.setVisibility(View.GONE);
        if (btnNavUsers != null) btnNavUsers.setVisibility(View.GONE);
        if (btnNavProfile != null) btnNavProfile.setVisibility(View.GONE);
        if (btnNavTickets != null) btnNavTickets.setVisibility(View.GONE);

        // Leer SharedPreferences
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int rolId = preferences.getInt(KEY_ROL_ID, -1);

        // Mostrar según rol
        switch (rolId) {
            case ROL_USUARIO:
                if (btnNavNotifications != null) btnNavNotifications.setVisibility(View.VISIBLE);
                if (btnNavNewTicket != null) btnNavNewTicket.setVisibility(View.VISIBLE);
                if (btnNavTickets != null) btnNavTickets.setVisibility(View.VISIBLE);
                if (btnNavProfile != null) btnNavProfile.setVisibility(View.VISIBLE);
                break;
            case ROL_TECNICO:
                if (btnNavNotifications != null) btnNavNotifications.setVisibility(View.VISIBLE);
                if (btnNavTickets != null) btnNavTickets.setVisibility(View.VISIBLE);
                if (btnNavProfile != null) btnNavProfile.setVisibility(View.VISIBLE);
                break;
            case ROL_ADMIN_TI:
                if (btnNavSlaDashboard != null) btnNavSlaDashboard.setVisibility(View.VISIBLE);
                if (btnNavUsers != null) btnNavUsers.setVisibility(View.VISIBLE);
                if (btnNavProfile != null) btnNavProfile.setVisibility(View.VISIBLE);
                if (btnNavTickets != null) btnNavTickets.setVisibility(View.VISIBLE);
                break;
            default:
                if (btnNavTickets != null) btnNavTickets.setVisibility(View.VISIBLE);
                if (btnNavProfile != null) btnNavProfile.setVisibility(View.VISIBLE);
                break;
        }

        // ==========================================
        // CONFIGURAR LOS CLICKS GLOBALES DEL FOOTER
        // ==========================================

        if (btnNavNewTicket != null) {
            btnNavNewTicket.setOnClickListener(v -> {
                // Evitar abrir la Activity si ya estamos en ella (opcional)
                if (!(activity instanceof RegisterTicketActivity)) {
                    Intent intent = new Intent(activity, RegisterTicketActivity.class);
                    activity.startActivity(intent);
                }
            });
        }

        if (btnNavTickets != null) {
            btnNavTickets.setOnClickListener(v -> {
                if (!(activity instanceof TicketsActivity)) {
                    Intent intent = new Intent(activity, TicketsActivity.class);
                    activity.startActivity(intent);
                }
            });
        }

        if (btnNavProfile != null) {
            btnNavProfile.setOnClickListener(v -> {
                // Aquí puedes agregar tu lógica de validación de rol para ir a PerfilUsuario o PerfilTi
                Intent intent;
                if (rolId == 1) {
                    intent = new Intent(activity, PerfilUsuarioActivity.class);
                } else {
                    intent = new Intent(activity, PerfilTiActivity.class);
                }

                activity.getClass();
                Objects.requireNonNull(intent.getComponent()).getClassName();
                activity.startActivity(intent);
            });
        }

        if (btnNavUsers != null) {
            btnNavUsers.setOnClickListener(v -> {
                if (!(activity instanceof UsuarioActivity)) {
                    activity.startActivity(new Intent(activity, UsuarioActivity.class));
                }
            });
        }

        if (btnNavNotifications != null) {
            btnNavNotifications.setOnClickListener(v -> {
                if (!(activity instanceof NotificacionesActivity)) {
                    activity.startActivity(new Intent(activity, NotificacionesActivity.class));
                }
            });
        }

        if (btnNavSlaDashboard != null) {
            btnNavSlaDashboard.setOnClickListener(v -> {
                if (!(activity instanceof DashboardSlaActivity)) {
                    activity.startActivity(new Intent(activity, DashboardSlaActivity.class));
                }
            });
        }

        // Indicador de no leídas: se refresca al volver a la pantalla y cuando llega un aviso (FCM)
        if (activity instanceof LifecycleOwner) {
            final BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override public void onReceive(Context c, Intent i) { refrescarBadge(rootView); }
            };
            ((LifecycleOwner) activity).getLifecycle().addObserver(new DefaultLifecycleObserver() {
                @Override public void onStart(@NonNull LifecycleOwner o) {
                    ContextCompat.registerReceiver(activity, receiver,
                            new IntentFilter(TicketsActivity.ACTION_REFRESH), ContextCompat.RECEIVER_NOT_EXPORTED);
                }
                @Override public void onResume(@NonNull LifecycleOwner o) { refrescarBadge(rootView); }
                @Override public void onStop(@NonNull LifecycleOwner o) {
                    try { activity.unregisterReceiver(receiver); } catch (IllegalArgumentException ignored) { }
                }
            });
        }

        // Agrega el resto de onClicks (Usuarios, SLA) siguiendo el mismo patrón...
    }

    /** Consulta cuántas notificaciones sin leer hay y muestra/oculta el indicador de la campana */
    public static void refrescarBadge(View footerRoot) {
        final TextView badge = footerRoot.findViewById(R.id.txtNotifBadge);
        if (badge == null) return;
        SharedPreferences prefs = footerRoot.getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String token = prefs.getString(KEY_TOKEN, null);
        if (token == null || token.isEmpty()) return;

        ApiClient.getService().getNotifNoLeidas("Bearer " + token).enqueue(new Callback<Map<String, Integer>>() {
            @Override
            public void onResponse(Call<Map<String, Integer>> call, Response<Map<String, Integer>> r) {
                if (!r.isSuccessful() || r.body() == null) return;
                Integer n = r.body().get("count");
                int cuantas = n != null ? n : 0;
                badge.setText(cuantas > 99 ? "99+" : String.valueOf(cuantas));
                badge.setVisibility(cuantas > 0 ? View.VISIBLE : View.GONE);
            }
            @Override public void onFailure(Call<Map<String, Integer>> call, Throwable t) { }
        });
    }
}