package com.example.mobile_project;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_project.notificaciones.NotificacionesPageResponse;
import com.example.mobile_project.shared.clients.ApiClient;

import com.example.mobile_project.notificaciones.NotificacionesPageResponse.Item;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Lista de notificaciones paginada (scroll infinito), más recientes primero */
public class NotificacionesActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "SoporteClinicaPrefs";

    private final List<NotificacionesPageResponse.Item> items = new ArrayList<>();
    private Adapter adapter;
    private RecyclerView rv;
    private TextView txtEmpty;
    private View footer;
    private String auth;
    private int pagina = 0, ultimaPagina = 1, seq = 0;
    private boolean cargando = false, yaCargo = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notificaciones);

        footer = findViewById(R.id.includeFooter);
        if (footer != null) FooterHelper.setupFooter(this, footer);

        String token = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("access_token", null);
        if (token == null || token.isEmpty()) { finish(); return; }
        auth = "Bearer " + token;

        txtEmpty = findViewById(R.id.txtEmpty);
        rv = findViewById(R.id.rvNotificaciones);
        adapter = new Adapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(@NonNull RecyclerView r, int dx, int dy) {
                if (dy > 0 && !r.canScrollVertically(1)) cargarSiguiente();   // llegó al final -> siguiente página
            }
        });

        findViewById(R.id.btnLeerTodas).setOnClickListener(v ->
                ApiClient.getService().leerTodasNotif(auth).enqueue(new Callback<ResponseBody>() {
                    @Override public void onResponse(Call<ResponseBody> c, Response<ResponseBody> r) {
                        if (r.isSuccessful()) { for (NotificacionesPageResponse.Item i : items) i.setLeida(true); adapter.notifyDataSetChanged(); actualizarFooter(); }
                    }
                    @Override public void onFailure(Call<ResponseBody> c, Throwable t) { aviso("Error de conexión"); }
                }));

        recargar();
    }

    /** Al volver del detalle se recarga para reflejar lo ya leído */
    @Override
    protected void onResume() {
        super.onResume();
        if (yaCargo) recargar(); else yaCargo = true;
    }

    private void recargar() {
        seq++;
        pagina = 0;
        ultimaPagina = 1;
        cargando = false;
        cargarSiguiente();
    }

    private void cargarSiguiente() {
        if (cargando || pagina >= ultimaPagina) return;
        cargando = true;
        final int miSeq = seq;

        ApiClient.getService().getNotificaciones(auth, pagina + 1).enqueue(new Callback<NotificacionesPageResponse>() {
            @Override
            public void onResponse(Call<NotificacionesPageResponse> c, Response<NotificacionesPageResponse> r) {
                if (miSeq != seq) return;
                cargando = false;
                if (!r.isSuccessful() || r.body() == null) { aviso("Error al obtener las notificaciones"); return; }

                NotificacionesPageResponse b = r.body();
                pagina = b.getCurrentPage();
                ultimaPagina = Math.max(1, b.getLastPage());
                if (pagina == 1) items.clear();
                if (b.getData() != null) items.addAll(b.getData());
                adapter.notifyDataSetChanged();
                txtEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(Call<NotificacionesPageResponse> c, Throwable t) {
                if (miSeq != seq) return;
                cargando = false;
                aviso("Error de conexión con el servidor");
            }
        });
    }

    private void abrir(NotificacionesPageResponse.Item n, int pos) {
        if (!n.isLeida()) {
            n.setLeida(true);
            adapter.notifyItemChanged(pos);
            ApiClient.getService().marcarNotifLeida(auth, n.getId()).enqueue(new Callback<ResponseBody>() {
                @Override public void onResponse(Call<ResponseBody> c, Response<ResponseBody> r) { actualizarFooter(); }
                @Override public void onFailure(Call<ResponseBody> c, Throwable t) { }
            });
        }
        if (n.getTicketCodigo() != null) {
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                    .putString("selected_ticket_code", n.getTicketCodigo()).apply();
            startActivity(new Intent(this, TicketDetailActivity.class));
        }
    }

    private void actualizarFooter() { if (footer != null) FooterHelper.refrescarBadge(footer); }

    private void aviso(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }

    private static String icono(String tipo) {
        if (tipo == null) return "🔔";
        switch (tipo) {
            case "NUEVO_MENSAJE": return "💬";
            case "CAMBIO_ESTADO": return "🔄";
            case "TICKET_ASIGNADO": return "👤";
            default: return "🔔";
        }
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.VH> {
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_notificacion, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            final NotificacionesPageResponse.Item n = items.get(position);
            h.titulo.setText(icono(n.getTipo()) + " " + n.getTitulo());
            h.titulo.setTypeface(null, n.isLeida() ? Typeface.NORMAL : Typeface.BOLD);   // no leída = negrita + punto
            h.cuerpo.setText(n.getCuerpo() != null ? n.getCuerpo() : "");
            String f = n.getCreadoEn();
            h.fecha.setText(f != null && f.length() >= 16 ? f.substring(0, 16).replace('T', ' ') : "");
            h.punto.setVisibility(n.isLeida() ? View.INVISIBLE : View.VISIBLE);
            h.itemView.setOnClickListener(v -> abrir(n, h.getBindingAdapterPosition()));
        }

        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            final TextView titulo, cuerpo, fecha;
            final View punto;
            VH(@NonNull View v) {
                super(v);
                titulo = v.findViewById(R.id.txtNotifTitulo);
                cuerpo = v.findViewById(R.id.txtNotifCuerpo);
                fecha = v.findViewById(R.id.txtNotifFecha);
                punto = v.findViewById(R.id.viewPunto);
            }
        }
    }
}