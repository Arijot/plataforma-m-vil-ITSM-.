package com.example.mobile_project.shared.clients;

import com.example.mobile_project.ticket.TicketDetailResponse.Mensaje;
import com.google.gson.Gson;
import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.PrivateChannelEventListener;
import com.pusher.client.channel.PusherEvent;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.ConnectionStateChange;
import com.pusher.client.util.HttpChannelAuthorizer;

import java.util.HashMap;
import java.util.Map;

/** Cliente WebSocket (protocolo Pusher) contra Laravel Reverb. */
public class RealtimeClient {

    // ================== CONFIGURA ESTOS VALORES ==================
    // ================== CONFIGURA ESTOS VALORES ==================
    private static final String REVERB_HOST = "192.168.18.34";
    private static final int REVERB_PORT = 8080;
    private static final String REVERB_KEY = "soporte-key";
    private static final String AUTH_URL = "http://192.168.18.34:8000/api/broadcasting/auth";
    private static final boolean USE_TLS = false;
    // =============================================================

    public interface Listener {
        void onMensaje(Mensaje m);        // llega en hilo de fondo
        void onTicketActualizado();       // cambió estado o técnico
        void onConectado();               // (re)conexión: conviene sincronizar mensajes perdidos
    }

    public static class MensajeEvent { public Mensaje mensaje; }

    private final Gson gson = new Gson();
    private Pusher pusher;
    private String codigoActual;

    /** Idempotente: si ya está conectado a ese ticket no hace nada. */
    public synchronized void conectar(String bearer, String codigo, boolean esStaff, final Listener l) {
        if (pusher != null && codigo.equals(codigoActual)) return;
        desconectar();
        codigoActual = codigo;

        HttpChannelAuthorizer authorizer = new HttpChannelAuthorizer(AUTH_URL);
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", bearer);          // "Bearer xxx"
        headers.put("Accept", "application/json");
        authorizer.setHeaders(headers);

        PusherOptions options = new PusherOptions();
        options.setHost(REVERB_HOST);
        options.setWsPort(REVERB_PORT);
        options.setWssPort(REVERB_PORT);
        options.setUseTLS(USE_TLS);
        options.setChannelAuthorizer(authorizer);

        pusher = new Pusher(REVERB_KEY, options);
        pusher.connect(new ConnectionEventListener() {
            @Override public void onConnectionStateChange(ConnectionStateChange change) {
                if (change.getCurrentState() == ConnectionState.CONNECTED) l.onConectado();
            }
            @Override public void onError(String message, String code, Exception e) { }
        }, ConnectionState.ALL);

        PrivateChannelEventListener canal = new PrivateChannelEventListener() {
            @Override public void onAuthenticationFailure(String message, Exception e) { }
            @Override public void onSubscriptionSucceeded(String channelName) { }
            @Override public void onEvent(PusherEvent event) { manejar(event, l); }
        };

        // Mensajes públicos + cambios de estado/técnico
        pusher.subscribePrivate("private-ticket." + codigo, canal, "mensaje.nuevo", "ticket.actualizado");
        // Notas internas: solo técnico/admin (el servidor rechazaría al usuario final)
        if (esStaff) {
            pusher.subscribePrivate("private-ticket." + codigo + ".interno", canal, "mensaje.nuevo");
        }
    }

    private void manejar(PusherEvent event, Listener l) {
        String nombre = event.getEventName();
        if ("mensaje.nuevo".equals(nombre)) {
            MensajeEvent me = gson.fromJson(event.getData(), MensajeEvent.class);
            if (me != null && me.mensaje != null) l.onMensaje(me.mensaje);
        } else if ("ticket.actualizado".equals(nombre)) {
            l.onTicketActualizado();
        }
    }

    public synchronized void desconectar() {
        if (pusher != null) {
            try { pusher.disconnect(); } catch (Exception ignored) { }
            pusher = null;
        }
        codigoActual = null;
    }
}
