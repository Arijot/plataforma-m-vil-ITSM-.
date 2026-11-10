package com.example.mobile_project;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "tickets_channel";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        FcmHelper.enviarTokenAlServidor(getApplicationContext(), token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        Map<String, String> d = message.getData();
        if (d.isEmpty()) return;

        String codigo = d.get("ticket_codigo");
        String tipo = d.get("tipo");

        // Asignaciones, cambios de estado, etc.: si la lista de tickets está abierta, se refresca sola
        if (!"NUEVO_MENSAJE".equals(tipo)) {
            sendBroadcast(new Intent(TicketsActivity.ACTION_REFRESH).setPackage(getPackageName()));
        }

        // Si el usuario ya está dentro del chat de ese ticket, el polling lo muestra: no duplicar
        if ("NUEVO_MENSAJE".equals(tipo) && codigo != null && codigo.equals(TicketDetailActivity.activeTicketCode)) {
            return;
        }

        crearCanal();

        Intent intent = new Intent(this, TicketDetailActivity.class);
        intent.putExtra("TICKET_CODE", codigo);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int requestCode = codigo != null ? codigo.hashCode() : 0;
        PendingIntent pi = PendingIntent.getActivity(this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_chat)   // cámbialo por tu propio ícono
                .setContentTitle(d.get("titulo"))
                .setContentText(d.get("cuerpo"))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(d.get("cuerpo")))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);

        if (Build.VERSION.SDK_INT >= 33 &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManagerCompat.from(this).notify(requestCode, b.build());
    }

    private void crearCanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Tickets y mensajes",
                    NotificationManager.IMPORTANCE_HIGH);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }
}
