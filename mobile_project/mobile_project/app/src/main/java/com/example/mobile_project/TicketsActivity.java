package com.example.mobile_project;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_project.estados_ticket.EstadosCache;
import com.example.mobile_project.shared.clients.ApiClient;
import com.example.mobile_project.ticket.EstadoTicket;
import com.example.mobile_project.ticket.Ticket;
import com.example.mobile_project.ticket.TicketsPageResponse;
import com.example.mobile_project.user.User;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TicketsActivity extends AppCompatActivity {

    /** Broadcast interno: lo envía el servicio FCM cuando llega un aviso que cambia la lista */
    public static final String ACTION_REFRESH = "com.example.mobile_project.REFRESH_TICKETS";

    private static final String PREFS_NAME = "SoporteClinicaPrefs";
    private static final String KEY_TOKEN = "access_token";
    private static final String KEY_ROL_ID = "rol_id";

    // Filtros fijos (agrupaciones propias de cada rol). Los filtros por estado se agregan
    // dinámicamente con lo que llega de la tabla estados_ticket (campo "estados" del API).
    private static final String[][] FILTROS_BASE_USUARIO_TECNICO = {{"Todos", "todos"}, {"Activos", "activos"}};
    private static final String[][] FILTROS_BASE_ADMIN = {{"Todos", "todos"}, {"Sin asignar", "sin_asignar"}, {"Asignados", "asignados"}};

    private ImageView imgProfile;
    private TextView txtGreeting, txtListTitle, txtEmpty, txtPageInfo;
    private Button btnPrev, btnNext;
    private LinearLayout layoutFiltros;
    private RecyclerView rvTickets;
    private TicketAdapter ticketAdapter;
    private final List<Ticket> ticketList = new ArrayList<>();

    private int rolId = 1;
    private String filtroActual = "todos";
    private int paginaActual = 1, ultimaPagina = 1, totalTickets = 0;
    private int requestSeq = 0;          // descarta respuestas viejas si el usuario cambia rápido de filtro/página
    private boolean yaCargo = false;
    private boolean chipsEstadoAgregados = false;
    private final List<TextView> chips = new ArrayList<>();
    private final List<String> chipValores = new ArrayList<>();

    private final BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { cargarTickets(); }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tickets);

        View footer = findViewById(R.id.includeFooter);
        if (footer != null) FooterHelper.setupFooter(this, footer);

        // Permiso de notificaciones (Android 13+) y registro del token FCM
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
        }
        FcmHelper.registrarToken(this);

        imgProfile = findViewById(R.id.imgProfile);
        txtGreeting = findViewById(R.id.txtGreeting);
        txtListTitle = findViewById(R.id.txtListTitle);
        txtEmpty = findViewById(R.id.txtEmpty);
        txtPageInfo = findViewById(R.id.txtPageInfo);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        layoutFiltros = findViewById(R.id.layoutFiltros);
        rvTickets = findViewById(R.id.rvTickets);

        rolId = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_ROL_ID, 1);

        rvTickets.setLayoutManager(new LinearLayoutManager(this));
        ticketAdapter = new TicketAdapter(ticketList, rolId);
        rvTickets.setAdapter(ticketAdapter);

        configurarTituloSegunRol();
        construirFiltros();
        cargarInfoUsuario();

        btnPrev.setOnClickListener(v -> { if (paginaActual > 1) { paginaActual--; cargarTickets(); } });
        btnNext.setOnClickListener(v -> { if (paginaActual < ultimaPagina) { paginaActual++; cargarTickets(); } });
        imgProfile.setOnClickListener(v -> redireccionarSegunRol());

        cargarEstados();
        cargarTickets();
    }

    @Override
    protected void onStart() {
        super.onStart();
        ContextCompat.registerReceiver(this, refreshReceiver, new IntentFilter(ACTION_REFRESH),
                ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(refreshReceiver);
    }

    /** Al volver del detalle (p. ej. el admin asignó un técnico) la lista se refresca sola */
    @Override
    protected void onResume() {
        super.onResume();
        if (yaCargo) cargarTickets(); else yaCargo = true;
    }

    // ------------------------------------------------------------------ UI

    private void configurarTituloSegunRol() {
        if (rolId == 3) txtListTitle.setText("Todos los tickets");
        else if (rolId == 2) txtListTitle.setText("Tickets asignados");
        else txtListTitle.setText("Mis Tickets");
    }

    private void construirFiltros() {
        String[][] base = rolId == 3 ? FILTROS_BASE_ADMIN : FILTROS_BASE_USUARIO_TECNICO;
        for (String[] f : base) agregarChip(f[0], f[1]);
        pintarChips();
    }

    /** Un chip por cada estado de estados_ticket (nombre y orden vienen de la BD). Se agregan una sola vez. */
    private void agregarChipsEstados(List<EstadoTicket> estados) {
        if (chipsEstadoAgregados || estados == null || estados.isEmpty()) return;
        chipsEstadoAgregados = true;
        for (EstadoTicket e : estados) {
            agregarChip(e.getNombre(), e.getCodigo().toLowerCase(Locale.ROOT));   // ej. EN_PROCESO -> en_proceso
        }
        pintarChips();
    }

    private void agregarChip(String texto, final String valor) {
        float d = getResources().getDisplayMetrics().density;
        final TextView chip = new TextView(this);
        chip.setText(texto);
        chip.setTextSize(13);
        chip.setPadding((int) (16 * d), (int) (8 * d), (int) (16 * d), (int) (8 * d));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd((int) (8 * d));
        chip.setLayoutParams(lp);

        chip.setOnClickListener(v -> {
            if (valor.equals(filtroActual)) return;
            filtroActual = valor;
            paginaActual = 1;
            pintarChips();
            cargarTickets();
        });

        layoutFiltros.addView(chip);
        chips.add(chip);
        chipValores.add(valor);
    }

    private void pintarChips() {
        for (int i = 0; i < chips.size(); i++) {
            boolean activo = chipValores.get(i).equals(filtroActual);
            chips.get(i).setBackgroundResource(activo ? R.drawable.bg_chip_on : R.drawable.bg_chip_off);
            chips.get(i).setTextColor(activo ? 0xFFFFFFFF : 0xFF333333);
        }
    }

    private void actualizarPaginacion() {
        txtPageInfo.setText("Página " + paginaActual + " de " + ultimaPagina + " · " + totalTickets
                + (totalTickets == 1 ? " ticket" : " tickets"));
        btnPrev.setEnabled(paginaActual > 1);
        btnNext.setEnabled(paginaActual < ultimaPagina);
        btnPrev.setAlpha(btnPrev.isEnabled() ? 1f : 0.4f);
        btnNext.setAlpha(btnNext.isEnabled() ? 1f : 0.4f);
    }

    // ------------------------------------------------------------------ datos

    private void cargarTickets() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String token = prefs.getString(KEY_TOKEN, null);
        if (token == null || token.isEmpty()) return;

        final int seq = ++requestSeq;
        btnPrev.setEnabled(false);
        btnNext.setEnabled(false);

        ApiClient.getService().getMyTickets("Bearer " + token, paginaActual, filtroActual)
                .enqueue(new Callback<TicketsPageResponse>() {
                    @Override
                    public void onResponse(Call<TicketsPageResponse> call, Response<TicketsPageResponse> r) {
                        if (seq != requestSeq) return;   // respuesta vieja

                        if (!r.isSuccessful() || r.body() == null) {
                            actualizarPaginacion();
                            Toast.makeText(TicketsActivity.this, "Error al obtener los tickets", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        TicketsPageResponse page = r.body();
                        if (EstadosCache.vacio()) EstadosCache.set(page.getEstados());   // respaldo si getEstadosTicket falló
                        agregarChipsEstados(EstadosCache.get());
                        ultimaPagina = Math.max(1, page.getLastPage());
                        totalTickets = page.getTotal();

                        // Si la página ya no existe (p. ej. se terminaron tickets), retrocede y recarga
                        if ((page.getData() == null || page.getData().isEmpty()) && paginaActual > ultimaPagina) {
                            paginaActual = ultimaPagina;
                            cargarTickets();
                            return;
                        }

                        paginaActual = page.getCurrentPage();
                        ticketList.clear();
                        if (page.getData() != null) ticketList.addAll(page.getData());
                        ticketAdapter.notifyDataSetChanged();
                        rvTickets.scrollToPosition(0);

                        txtEmpty.setVisibility(ticketList.isEmpty() ? View.VISIBLE : View.GONE);
                        actualizarPaginacion();
                    }

                    @Override
                    public void onFailure(Call<TicketsPageResponse> call, Throwable t) {
                        if (seq != requestSeq) return;
                        actualizarPaginacion();
                        Toast.makeText(TicketsActivity.this, "Error de conexión con el servidor", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** Trae el catálogo de estados (getEstadosTicket), arma los chips y repinta los badges de la lista */
    private void cargarEstados() {
        String token = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_TOKEN, null);
        if (token == null || token.isEmpty()) return;

        EstadosCache.cargar("Bearer " + token, () -> {
            agregarChipsEstados(EstadosCache.get());
            ticketAdapter.notifyDataSetChanged();
        });
    }

    private void cargarInfoUsuario() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String userJson = preferences.getString("user_data", null);

        if (userJson != null) {
            User user = new Gson().fromJson(userJson, User.class);
            if (user != null && user.getNombres() != null) {
                txtGreeting.setText("Hola, " + user.getNombres());

                // Foto del header según el género (1 = mujer, 2 = hombre)
                if (user.getGenero() != null) {
                    if (user.getGenero() == 1) imgProfile.setImageResource(R.drawable.hombre_foto);
                    else if (user.getGenero() == 2) imgProfile.setImageResource(R.drawable.mujer_foto);
                }
            }
        }
    }

    private void redireccionarSegunRol() {
        int rol = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_ROL_ID, 1);
        if (rol == 1) {
            startActivity(new Intent(this, PerfilUsuarioActivity.class));
        } else if (rol == 2 || rol == 3) {
            startActivity(new Intent(this, PerfilTiActivity.class));
        } else {
            Toast.makeText(this, "Rol de usuario no reconocido", Toast.LENGTH_SHORT).show();
        }
    }
}