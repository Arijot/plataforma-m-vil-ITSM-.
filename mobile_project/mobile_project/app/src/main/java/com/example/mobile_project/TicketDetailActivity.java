package com.example.mobile_project;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_project.estados_ticket.EstadosCache;
import com.example.mobile_project.shared.clients.ApiClient;
import com.example.mobile_project.shared.clients.RealtimeClient;
import com.example.mobile_project.ticket.EstadoTicket;
import com.example.mobile_project.ticket.PlazoHelper;
import com.example.mobile_project.ticket.TicketAcciones;
import com.example.mobile_project.ticket.TicketDetail;
import com.example.mobile_project.ticket.TicketDetailResponse;
import com.example.mobile_project.ticket.TicketDetailResponse.Archivo;
import com.example.mobile_project.ticket.TicketDetailResponse.Estado;
import com.example.mobile_project.ticket.TicketDetailResponse.Historial;
import com.example.mobile_project.ticket.TicketDetailResponse.MensajesResponse;
import com.example.mobile_project.ticket.TicketDetailResponse.Tecnico;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TicketDetailActivity extends AppCompatActivity {

    /** Código del ticket que se está viendo (el servicio FCM lo usa para no duplicar avisos) */
    public static volatile String activeTicketCode = null;

    private static final String PREFS_NAME = "SoporteClinicaPrefs";
    private static final String KEY_TOKEN = "access_token";
    private static final String KEY_ROL_ID = "rol_id";
    private static final String KEY_SELECTED_TICKET = "selected_ticket_code";

    // Complejidad (código que viaja a la API, etiqueta que ve el admin). La posición 0 es "sin cambio".
    private static final String[] COMPLEJIDAD_CODIGOS = {"BAJA", "MEDIA", "ALTA"};
    private static final String[] COMPLEJIDAD_ETIQUETAS = {"Baja", "Media", "Alta"};

    // Vistas
    private NestedScrollView svContent;
    private TextView txtDetailCode, txtDetailStatus, txtDetailTitle, txtDetailService, txtDetailPriority,
            txtDetailTecnico, txtDetailSolicitante, txtDetailDescription, txtDetailSolucion,
            txtAttachmentMessage, txtEncuesta, txtHistorial, txtChatVacio;
    private LinearLayout llAttachments, layoutSolucion, layoutMessageBar;
    private View cardAsignar, cardEstado, cardEncuesta, cardHistorial;
    private Spinner spinnerEstado, spinnerTecnico, spinnerComplejidad;
    private EditText edtSolucion, edtMensaje, edtPlazoDias, edtComentarioEncuesta;
    private CheckBox chkInterno;
    private Button btnActualizarEstado, btnAsignar, btnEnviar, btnGuardarGestion, btnReabrir, btnEnviarEncuesta;
    private TextView txtDetailGestion, txtReabrirInfo;
    private View cardGestion, cardReabrir;
    private LinearLayout layoutEncuestaForm;
    private RatingBar ratingEncuesta;
    private RecyclerView rvMensajes;

    // Estado
    private ChatAdapter chatAdapter;
    private String authHeader, ticketCode, estadoActual, estadoActualNombre;
    private Long tecnicoActualId;
    private String complejidadActual;          // para enviar solo lo que el admin realmente cambió
    private Integer plazoDiasActual;
    private int rolId;
    private boolean detalleCargado = false, primeraCarga = true;
    private final List<String> codigosEstado = new ArrayList<>();
    private final List<Long> idsTecnicos = new ArrayList<>();
    private List<Estado> ultimosPermitidos = new ArrayList<>();   // transiciones que devolvió el backend

    private final RealtimeClient realtime = new RealtimeClient();

    // ------------------------------------------------------------------ ciclo de vida

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_detail);

        View footer = findViewById(R.id.includeFooter);
        if (footer != null) FooterHelper.setupFooter(this, footer);

        enlazarVistas();

        chatAdapter = new ChatAdapter();
        rvMensajes.setLayoutManager(new LinearLayoutManager(this));
        rvMensajes.setAdapter(chatAdapter);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        rolId = prefs.getInt(KEY_ROL_ID, 1);
        String tokenPref = prefs.getString(KEY_TOKEN, null);
        ticketCode = resolverCodigo(getIntent());

        if (ticketCode == null || ticketCode.trim().isEmpty()) {
            msg("No se especificó el ticket a consultar");
            finish();
            return;
        }
        if (tokenPref == null || tokenPref.isEmpty()) {
            msg("Sesión no válida");
            finish();
            return;
        }
        authHeader = "Bearer " + tokenPref;

        configurarListeners();
        cargarDetalle();
        // Catálogo de estados (getEstadosTicket). Si ya estaba en memoria no se vuelve a pedir.
        if (EstadosCache.vacio()) EstadosCache.cargar(authHeader, this::refrescarEstadoDesdeCatalogo);
        if (rolId == 3) cargarTecnicos();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String nuevo = resolverCodigo(intent);
        if (nuevo != null && !nuevo.equals(ticketCode)) {   // tocaron una notificación de otro ticket
            ticketCode = nuevo;
            activeTicketCode = nuevo;
            detalleCargado = false;
            primeraCarga = true;
            realtime.desconectar();
            chatAdapter.clear();
            cargarDetalle();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        activeTicketCode = ticketCode;
        if (detalleCargado) conectarRealtime();
    }

    @Override
    protected void onPause() {
        super.onPause();
        activeTicketCode = null;
        realtime.desconectar();
    }

    // ------------------------------------------------------------------ inicialización

    private void enlazarVistas() {
        svContent = findViewById(R.id.svContent);
        txtDetailCode = findViewById(R.id.txtDetailCode);
        txtDetailStatus = findViewById(R.id.txtDetailStatus);
        txtDetailTitle = findViewById(R.id.txtDetailTitle);
        txtDetailService = findViewById(R.id.txtDetailService);
        txtDetailPriority = findViewById(R.id.txtDetailPriority);
        txtDetailTecnico = findViewById(R.id.txtDetailTecnico);
        txtDetailSolicitante = findViewById(R.id.txtDetailSolicitante);
        txtDetailDescription = findViewById(R.id.txtDetailDescription);
        txtDetailSolucion = findViewById(R.id.txtDetailSolucion);
        txtAttachmentMessage = findViewById(R.id.txtAttachmentMessage);
        llAttachments = findViewById(R.id.llAttachments);
        cardAsignar = findViewById(R.id.cardAsignar);
        cardEstado = findViewById(R.id.cardEstado);
        cardEncuesta = findViewById(R.id.cardEncuesta);
        txtEncuesta = findViewById(R.id.txtEncuesta);
        txtHistorial = findViewById(R.id.txtHistorial);
        cardHistorial = findViewById(R.id.cardHistorial);   // OJO: agrega este id a la card del historial en activity_ticket_detail.xml
        spinnerEstado = findViewById(R.id.spinnerEstado);
        spinnerTecnico = findViewById(R.id.spinnerTecnico);
        layoutSolucion = findViewById(R.id.layoutSolucion);
        edtSolucion = findViewById(R.id.edtSolucion);
        btnActualizarEstado = findViewById(R.id.btnActualizarEstado);
        btnAsignar = findViewById(R.id.btnAsignar);
        rvMensajes = findViewById(R.id.rvMensajes);
        txtChatVacio = findViewById(R.id.txtChatVacio);
        layoutMessageBar = findViewById(R.id.layoutMessageBar);
        chkInterno = findViewById(R.id.chkInterno);
        edtMensaje = findViewById(R.id.edtMensaje);
        btnEnviar = findViewById(R.id.btnEnviar);

        // Nuevos: complejidad/plazo (admin), reabrir (usuario), encuesta con estrellas (usuario)
        txtDetailGestion = findViewById(R.id.txtDetailGestion);
        cardGestion = findViewById(R.id.cardGestion);
        spinnerComplejidad = findViewById(R.id.spinnerComplejidad);
        edtPlazoDias = findViewById(R.id.edtPlazoDias);
        btnGuardarGestion = findViewById(R.id.btnGuardarGestion);
        cardReabrir = findViewById(R.id.cardReabrir);
        txtReabrirInfo = findViewById(R.id.txtReabrirInfo);
        btnReabrir = findViewById(R.id.btnReabrir);
        layoutEncuestaForm = findViewById(R.id.layoutEncuestaForm);
        ratingEncuesta = findViewById(R.id.ratingEncuesta);
        edtComentarioEncuesta = findViewById(R.id.edtComentarioEncuesta);
        btnEnviarEncuesta = findViewById(R.id.btnEnviarEncuesta);

        List<String> opcionesComplejidad = new ArrayList<>();
        opcionesComplejidad.add("Sin cambio");
        for (String e : COMPLEJIDAD_ETIQUETAS) opcionesComplejidad.add(e);
        ArrayAdapter<String> adC = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, opcionesComplejidad);
        adC.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerComplejidad.setAdapter(adC);
    }

    private void configurarListeners() {
        btnEnviar.setOnClickListener(v -> enviarMensaje());
        btnActualizarEstado.setOnClickListener(v -> cambiarEstado());
        btnAsignar.setOnClickListener(v -> asignarTecnico());
        btnGuardarGestion.setOnClickListener(v -> guardarGestion());
        btnReabrir.setOnClickListener(v -> reabrirTicket());
        btnEnviarEncuesta.setOnClickListener(v -> enviarEncuesta());

        // Solo se pide la solución cuando se elige RESUELTO
        spinnerEstado.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                boolean resuelto = pos >= 0 && pos < codigosEstado.size() && "RESUELTO".equals(codigosEstado.get(pos));
                layoutSolucion.setVisibility(resuelto ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> p) { }
        });
    }

    /** Prioridad: extra del Intent (notificación) -> se guarda en prefs; si no, lo que guardó el Adapter */
    private String resolverCodigo(Intent intent) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String code = intent != null ? intent.getStringExtra("TICKET_CODE") : null;
        if (code != null && !code.trim().isEmpty()) {
            prefs.edit().putString(KEY_SELECTED_TICKET, code).apply();
            return code;
        }
        return prefs.getString(KEY_SELECTED_TICKET, null);
    }

    // ------------------------------------------------------------------ carga de datos

    private void cargarDetalle() {
        ApiClient.getService().getTicketDetail(authHeader, ticketCode).enqueue(new Callback<TicketDetailResponse>() {
            @Override
            public void onResponse(Call<TicketDetailResponse> call, Response<TicketDetailResponse> r) {
                if (r.isSuccessful() && r.body() != null && r.body().getTicket() != null) {
                    pintar(r.body());
                } else {
                    msg(extraerMensaje(r));
                    if (r.code() == 403 || r.code() == 404) finish();
                }
            }
            @Override
            public void onFailure(Call<TicketDetailResponse> call, Throwable t) {
                msg("Error de conexión: " + t.getMessage());
            }
        });
    }

    private void pintar(TicketDetailResponse data) {
        TicketDetail t = data.getTicket();
        estadoActual = t.getEstado();                       // código (ABIERTO, EN_PROCESO...) -> lógica
        estadoActualNombre = nombreEstado(estadoActual, t.getEstadoNombre());   // nombre según getEstadosTicket
        tecnicoActualId = t.getTecnicoId();

        txtDetailCode.setText(t.getCodigo());
        txtDetailStatus.setText(estadoActualNombre);
        txtDetailTitle.setText(t.getTitulo());
        txtDetailService.setText("Servicio afectado: " + t.getServicio());
        txtDetailPriority.setText("Prioridad: " + t.getPrioridad());
        txtDetailDescription.setText(t.getDescripcion());

        // Técnico asignado (lo ven todos los roles)
        txtDetailTecnico.setText(t.getTecnico() != null && !t.getTecnico().trim().isEmpty()
                ? "Técnico asignado: " + t.getTecnico()
                : "Técnico: aún sin asignar");

        // Datos del solicitante (solo técnico / admin)
        if (rolId != 1) {
            StringBuilder sb = new StringBuilder("Solicitante: ").append(t.getSolicitante());
            if (t.getAreaSolicitante() != null) sb.append(" (").append(t.getAreaSolicitante()).append(")");
            if (t.getUbicacion() != null) sb.append("\nUbicación: ").append(t.getUbicacion());
            txtDetailSolicitante.setText(sb.toString());
            txtDetailSolicitante.setVisibility(View.VISIBLE);
        }

        // Solución aplicada
        if (t.getSolucionAplicada() != null && !t.getSolucionAplicada().trim().isEmpty()) {
            txtDetailSolucion.setText("Solución aplicada: " + t.getSolucionAplicada());
            txtDetailSolucion.setVisibility(View.VISIBLE);
        } else {
            txtDetailSolucion.setVisibility(View.GONE);
        }

        pintarAdjuntos(data.getAdjuntos());
        // El historial lo ve SOLO el administrador de TI
        boolean esUsuarioAdmin = rolId == 3 || rolId == 1;
        if (cardHistorial != null) cardHistorial.setVisibility(esUsuarioAdmin ? View.VISIBLE : View.GONE);
        txtHistorial.setVisibility(esUsuarioAdmin ? View.VISIBLE : View.GONE);
        if (esUsuarioAdmin) pintarHistorial(data.getHistorial());
        pintarEncuesta(data.getEncuesta(), data.getAcciones());
        pintarAcciones(data.getAcciones());

        // Chat
        chatAdapter.setMiId(data.getMiUsuarioId());
        chatAdapter.setAll(data.getMensajes());
        txtChatVacio.setVisibility(chatAdapter.isEmpty() ? View.VISIBLE : View.GONE);

        // Permisos de UI según rol y estado
        boolean finalizado = "CERRADO".equals(estadoActual) || "CANCELADO".equals(estadoActual);
        layoutMessageBar.setVisibility(finalizado ? View.GONE : View.VISIBLE);
        chkInterno.setVisibility(rolId != 1 ? View.VISIBLE : View.GONE);
        cardAsignar.setVisibility(rolId == 3 && !finalizado ? View.VISIBLE : View.GONE);
        pintarEstados(data.getEstadosPermitidos());

        detalleCargado = true;
        conectarRealtime();

        if (primeraCarga) { primeraCarga = false; scrollAbajo(); }
    }

    private void pintarAdjuntos(TicketDetailResponse.Adjuntos adj) {
        llAttachments.removeAllViews();
        boolean tiene = adj != null && adj.isTieneAdjuntos() && adj.getArchivos() != null;
        if (!tiene) {
            txtAttachmentMessage.setVisibility(View.VISIBLE);
            txtAttachmentMessage.setText(adj != null && adj.getMensaje() != null
                    ? adj.getMensaje() : "El usuario no dejo archivos adjuntos en el ticket");
            return;
        }
        txtAttachmentMessage.setVisibility(View.GONE);
        int n = 1;
        for (Archivo a : adj.getArchivos()) {
            // En tu API 'nombre_archivo' guarda la URL completa del adjunto
            final String url = a.getNombreArchivo();
            TextView tv = new TextView(this);
            tv.setText("📎 Ver adjunto " + n++);
            tv.setTextColor(0xFF7E57C2);
            tv.setTextSize(14);
            tv.setPadding(0, 8, 0, 8);
            tv.setOnClickListener(v -> {
                if (url != null && url.startsWith("http")) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } else {
                    msg("No se pudo abrir el adjunto");
                }
            });
            llAttachments.addView(tv);
        }
    }

    private void pintarHistorial(List<Historial> lista) {
        if (lista == null || lista.isEmpty()) { txtHistorial.setText("Sin eventos registrados"); return; }
        StringBuilder sb = new StringBuilder();
        for (Historial h : lista) {
            sb.append("• ").append(corta(h.getCreadoEn())).append(" — ").append(h.getDetalle());
            if (h.getRealizadoPor() != null) sb.append(" (").append(h.getRealizadoPor()).append(")");
            sb.append("\n");
        }
        txtHistorial.setText(sb.toString().trim());
    }

    /**
     * Encuesta de satisfacción:
     *  - Usuario final con el ticket resuelto y sin responder -> formulario (estrellas + comentario).
     *  - Una vez enviada, al usuario ya no le aparece nada (el servidor deja de marcar puede_encuestar).
     *  - Técnico y admin ven la calificación recibida, solo lectura.
     */
    private void pintarEncuesta(TicketDetailResponse.Encuesta e, TicketAcciones a) {
        boolean formulario = a != null && a.isPuedeEncuestar();
        boolean resultado = e != null && rolId != 1;

        cardEncuesta.setVisibility(formulario || resultado ? View.VISIBLE : View.GONE);
        layoutEncuestaForm.setVisibility(formulario ? View.VISIBLE : View.GONE);

        if (resultado) {
            int n = 0;
            try { n = Integer.parseInt(String.valueOf(e.getCalificacion())); } catch (NumberFormatException ignored) { }
            n = Math.max(0, Math.min(5, n));
            StringBuilder estrellas = new StringBuilder();
            for (int i = 0; i < 5; i++) estrellas.append(i < n ? "★" : "☆");

            String texto = "Calificación: " + estrellas + "  (" + n + "/5)";
            if (e.getComentario() != null && !e.getComentario().isEmpty()) texto += "\n" + e.getComentario();
            txtEncuesta.setText(texto);
            txtEncuesta.setVisibility(View.VISIBLE);
        } else {
            txtEncuesta.setVisibility(View.GONE);
        }
    }

    /** Reabrir (usuario), complejidad/plazo (texto para técnico y admin; edición solo admin) */
    private void pintarAcciones(TicketAcciones a) {
        // --- Reabrir
        boolean reabrir = a != null && a.isPuedeReabrir();
        cardReabrir.setVisibility(reabrir ? View.VISIBLE : View.GONE);
        if (reabrir && a.getReabrirHasta() != null) {
            txtReabrirInfo.setText("Puedes reabrirlo hasta el " + corta(a.getReabrirHasta())
                    + ". Volverá a la cola de atención.");
        }

        // --- Resumen de complejidad y plazo (técnico y admin)
        if (rolId != 1 && a != null) {
            StringBuilder sb = new StringBuilder("Complejidad: ").append(etiquetaComplejidad(a.getComplejidad()));
            Long min = a.getMinutosRestantesTecnico();
            if (a.getPlazoDias() != null) {
                sb.append("\nPlazo: ").append(a.getPlazoDias()).append(a.getPlazoDias() == 1 ? " día" : " días");
                if (min != null) sb.append(" · ").append(PlazoHelper.texto(min));
            } else {
                sb.append("\nPlazo: sin definir");
            }
            txtDetailGestion.setText(sb.toString());
            txtDetailGestion.setTextColor(min != null ? PlazoHelper.color(min) : 0xFF555555);
            txtDetailGestion.setVisibility(View.VISIBLE);
        } else {
            txtDetailGestion.setVisibility(View.GONE);
        }

        // --- Edición: SOLO administrador de TI
        boolean editar = rolId == 3 && a != null && a.isPuedeEditarGestion();
        cardGestion.setVisibility(editar ? View.VISIBLE : View.GONE);
        if (editar) {
            complejidadActual = a.getComplejidad();
            plazoDiasActual = a.getPlazoDias();
            spinnerComplejidad.setSelection(0);   // "Sin cambio"; el valor vigente se ve en el resumen de arriba
            if (!edtPlazoDias.hasFocus()) {       // no pisar lo que el admin está escribiendo
                edtPlazoDias.setText(a.getPlazoDias() != null ? String.valueOf(a.getPlazoDias()) : "");
            }
        }
    }

    private String etiquetaComplejidad(String codigo) {
        if (codigo == null) return "sin definir";
        for (int i = 0; i < COMPLEJIDAD_CODIGOS.length; i++) {
            if (COMPLEJIDAD_CODIGOS[i].equals(codigo)) return COMPLEJIDAD_ETIQUETAS[i];
        }
        return codigo;
    }

    /** Nombre del estado: catálogo de getEstadosTicket -> nombre que trae el ticket -> código */
    private String nombreEstado(String codigo, String respaldo) {
        String n = EstadosCache.nombre(codigo);
        if (n != null) return n;
        return respaldo != null && !respaldo.isEmpty() ? respaldo : codigo;
    }

    /** Cuando llega el catálogo después del detalle: repinta el estado y las opciones del dropdown */
    private void refrescarEstadoDesdeCatalogo() {
        if (!detalleCargado) return;
        estadoActualNombre = nombreEstado(estadoActual, estadoActualNombre);
        txtDetailStatus.setText(estadoActualNombre);
        pintarEstados(ultimosPermitidos);
    }

    /**
     * Dropdown de actualizar estado. Las opciones y sus nombres salen del catálogo (getEstadosTicket, ordenado por "orden"):
     * el estado actual (preseleccionado) + los destinos que el backend permite según transiciones_estado y el rol.
     */
    private void pintarEstados(List<Estado> permitidos) {
        ultimosPermitidos = permitidos != null ? permitidos : new ArrayList<>();
        boolean puede = rolId == 2 && !ultimosPermitidos.isEmpty();
        cardEstado.setVisibility(puede ? View.VISIBLE : View.GONE);
        if (!puede) return;

        Set<String> permitidosCod = new LinkedHashSet<>();
        for (Estado e : ultimosPermitidos) permitidosCod.add(e.getCodigo());

        codigosEstado.clear();
        List<String> nombres = new ArrayList<>();
        int idxActual = -1;

        for (EstadoTicket e : EstadosCache.get()) {
            if (e.getCodigo().equals(estadoActual)) {
                idxActual = codigosEstado.size();
                codigosEstado.add(e.getCodigo());
                nombres.add(e.getNombre() + " (actual)");
            } else if (permitidosCod.contains(e.getCodigo())) {
                codigosEstado.add(e.getCodigo());
                nombres.add(e.getNombre());
            }
        }
        // Respaldo si el catálogo aún no cargó (o falta algún código): se completa con lo que dijo el backend
        for (Estado e : ultimosPermitidos) {
            if (!codigosEstado.contains(e.getCodigo())) { codigosEstado.add(e.getCodigo()); nombres.add(e.getNombre()); }
        }
        if (idxActual < 0) {
            codigosEstado.add(0, estadoActual);
            nombres.add(0, estadoActualNombre + " (actual)");
            idxActual = 0;
        }

        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, nombres);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEstado.setAdapter(ad);
        spinnerEstado.setSelection(idxActual);     // el estado guardado queda pintado en el dropdown
        layoutSolucion.setVisibility(View.GONE);
        edtSolucion.setText("");
    }

    private void cargarTecnicos() {
        ApiClient.getService().getTecnicos(authHeader).enqueue(new Callback<List<Tecnico>>() {
            @Override
            public void onResponse(Call<List<Tecnico>> call, Response<List<Tecnico>> r) {
                if (!r.isSuccessful() || r.body() == null) return;
                idsTecnicos.clear();
                List<String> nombres = new ArrayList<>();
                for (Tecnico t : r.body()) { idsTecnicos.add(t.getId()); nombres.add(t.getNombre()); }
                ArrayAdapter<String> ad = new ArrayAdapter<>(TicketDetailActivity.this,
                        android.R.layout.simple_spinner_item, nombres);
                ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerTecnico.setAdapter(ad);
            }
            @Override public void onFailure(Call<List<Tecnico>> call, Throwable t) { }
        });
    }

    // ------------------------------------------------------------------ chat en tiempo real (WebSocket + FCM)

    private void conectarRealtime() {
        realtime.conectar(authHeader, ticketCode, rolId != 1, new RealtimeClient.Listener() {
            @Override public void onMensaje(TicketDetailResponse.Mensaje m) {
                runOnUiThread(() -> {
                    if (chatAdapter.addNuevos(java.util.Collections.singletonList(m)) > 0) {
                        txtChatVacio.setVisibility(View.GONE);
                        scrollAbajo();
                    }
                });
            }
            @Override public void onTicketActualizado() { runOnUiThread(() -> cargarDetalle()); }
            // Al (re)conectar se piden los mensajes que pudieron perderse mientras no había conexión
            @Override public void onConectado() { runOnUiThread(() -> cargarMensajesNuevos()); }
        });
    }

    /** Solo sincroniza mensajes perdidos (al reconectar) y tras enviar; ya no hay polling periódico */

    private void cargarMensajesNuevos() {
        if (!detalleCargado || authHeader == null) return;

        ApiClient.getService().getMensajes(authHeader, ticketCode, chatAdapter.getLastId())
                .enqueue(new Callback<MensajesResponse>() {
                    @Override
                    public void onResponse(Call<MensajesResponse> call, Response<MensajesResponse> r) {
                        if (!r.isSuccessful() || r.body() == null) return;
                        MensajesResponse b = r.body();

                        if (b.getMensajes() != null && !b.getMensajes().isEmpty()
                                && chatAdapter.addNuevos(b.getMensajes()) > 0) {
                            txtChatVacio.setVisibility(View.GONE);
                            scrollAbajo();
                        }

                        // Si cambió el estado o el técnico, se recarga todo el detalle
                        boolean cambioEstado = b.getEstado() != null && !b.getEstado().equals(estadoActual);
                        boolean cambioTecnico = !Objects.equals(b.getTecnicoId(), tecnicoActualId);
                        if (cambioEstado || cambioTecnico) cargarDetalle();
                    }
                    @Override public void onFailure(Call<MensajesResponse> call, Throwable t) { }
                });
    }

    private void enviarMensaje() {
        String texto = edtMensaje.getText().toString().trim();
        if (texto.isEmpty()) return;

        btnEnviar.setEnabled(false);
        Map<String, Object> body = new HashMap<>();
        body.put("contenido", texto);
        body.put("es_interno", chkInterno.getVisibility() == View.VISIBLE && chkInterno.isChecked());

        ApiClient.getService().enviarMensaje(authHeader, ticketCode, body).enqueue(new ApiCallback() {
            @Override void ok() {
                edtMensaje.setText("");
                chkInterno.setChecked(false);
                cargarMensajesNuevos();
            }
            @Override void fin() { btnEnviar.setEnabled(true); }
        });
    }

    // ------------------------------------------------------------------ acciones de técnico / admin

    private void cambiarEstado() {
        int pos = spinnerEstado.getSelectedItemPosition();
        if (pos < 0 || pos >= codigosEstado.size()) return;

        String nuevo = codigosEstado.get(pos);
        if (nuevo.equals(estadoActual)) { msg("Elige un estado distinto al actual"); return; }
        String solucion = edtSolucion.getText().toString().trim();
        if ("RESUELTO".equals(nuevo) && solucion.isEmpty()) {
            edtSolucion.setError("Registra la solución aplicada");
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("estado", nuevo);
        if (!solucion.isEmpty()) body.put("solucion", solucion);

        btnActualizarEstado.setEnabled(false);
        ApiClient.getService().cambiarEstado(authHeader, ticketCode, body).enqueue(new ApiCallback() {
            @Override void ok() { msg("Estado actualizado"); cargarDetalle(); }
            @Override void fin() { btnActualizarEstado.setEnabled(true); }
        });
    }

    private void asignarTecnico() {
        int pos = spinnerTecnico.getSelectedItemPosition();
        if (pos < 0 || pos >= idsTecnicos.size()) { msg("No hay técnicos disponibles"); return; }

        Map<String, Object> body = new HashMap<>();
        body.put("tecnico_id", idsTecnicos.get(pos));

        btnAsignar.setEnabled(false);
        ApiClient.getService().asignarTecnico(authHeader, ticketCode, body).enqueue(new ApiCallback() {
            @Override void ok() { msg("Técnico asignado"); cargarDetalle(); }
            @Override void fin() { btnAsignar.setEnabled(true); }
        });
    }

    // ------------------------------------------------------------------ complejidad / plazo (admin)

    private void guardarGestion() {
        Map<String, Object> body = new HashMap<>();

        int pos = spinnerComplejidad.getSelectedItemPosition();
        if (pos > 0 && pos <= COMPLEJIDAD_CODIGOS.length) {
            String nueva = COMPLEJIDAD_CODIGOS[pos - 1];
            if (!nueva.equals(complejidadActual)) body.put("complejidad", nueva);
        }

        String txt = edtPlazoDias.getText().toString().trim();
        if (!txt.isEmpty()) {
            int dias;
            try { dias = Integer.parseInt(txt); } catch (NumberFormatException e) { edtPlazoDias.setError("Número inválido"); return; }
            if (dias < 0 || dias > 90) { edtPlazoDias.setError("Entre 0 y 90 días"); return; }
            // solo se envía si cambió: reenviar el mismo plazo reiniciaría el conteo
            if (plazoDiasActual == null || dias != plazoDiasActual) body.put("plazo_dias", dias);
        }

        if (body.isEmpty()) { msg("No hay cambios para guardar"); return; }

        btnGuardarGestion.setEnabled(false);
        ApiClient.getService().actualizarGestion(authHeader, ticketCode, body).enqueue(new ApiCallback() {
            @Override void ok() { msg("Cambios guardados"); edtPlazoDias.clearFocus(); cargarDetalle(); }
            @Override void fin() { btnGuardarGestion.setEnabled(true); }
        });
    }

    // ------------------------------------------------------------------ reabrir (usuario final)

    private void reabrirTicket() {
        final EditText input = new EditText(this);
        input.setHint("Cuéntanos qué sigue fallando");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setMinLines(3);
        input.setGravity(Gravity.TOP | Gravity.START);

        int m = (int) (20 * getResources().getDisplayMetrics().density);
        FrameLayout contenedor = new FrameLayout(this);
        contenedor.setPadding(m, m / 2, m, 0);
        contenedor.addView(input);

        final AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle("Reabrir ticket")
                .setMessage("El ticket volverá al estado inicial y se avisará al equipo de TI.")
                .setView(contenedor)
                .setPositiveButton("Reabrir", null)      // se sobrescribe abajo para validar sin cerrar
                .setNegativeButton("Cancelar", null)
                .create();
        dialogo.show();

        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String motivo = input.getText().toString().trim();
            if (motivo.length() < 5) { input.setError("Escribe al menos 5 caracteres"); return; }
            dialogo.dismiss();
            enviarReapertura(motivo);
        });
    }

    private void enviarReapertura(String motivo) {
        Map<String, Object> body = new HashMap<>();
        body.put("motivo", motivo);

        btnReabrir.setEnabled(false);
        ApiClient.getService().reabrirTicket(authHeader, ticketCode, body).enqueue(new ApiCallback() {
            @Override void ok() { msg("Ticket reabierto"); cargarDetalle(); }
            @Override void fin() { btnReabrir.setEnabled(true); }
        });
    }

    // ------------------------------------------------------------------ encuesta (usuario final)

    private void enviarEncuesta() {
        int nota = (int) ratingEncuesta.getRating();
        if (nota < 1) { msg("Elige de 1 a 5 estrellas"); return; }

        Map<String, Object> body = new HashMap<>();
        body.put("calificacion", nota);
        String comentario = edtComentarioEncuesta.getText().toString().trim();
        if (!comentario.isEmpty()) body.put("comentario", comentario);

        btnEnviarEncuesta.setEnabled(false);
        ApiClient.getService().enviarEncuesta(authHeader, ticketCode, body).enqueue(new ApiCallback() {
            @Override void ok() {
                msg("¡Gracias por tu opinión!");
                ratingEncuesta.setRating(0);
                edtComentarioEncuesta.setText("");
                cargarDetalle();      // al recargar, puede_encuestar = false y la tarjeta desaparece
            }
            @Override void fin() { btnEnviarEncuesta.setEnabled(true); }
        });
    }

    // ------------------------------------------------------------------ utilidades

    private abstract class ApiCallback implements Callback<ResponseBody> {
        abstract void ok();
        void fin() { }

        @Override public void onResponse(Call<ResponseBody> c, Response<ResponseBody> r) {
            if (r.isSuccessful()) ok(); else msg(extraerMensaje(r));
            fin();
        }
        @Override public void onFailure(Call<ResponseBody> c, Throwable t) {
            msg("Error de conexión");
            fin();
        }
    }

    /** Lee {"message": "..."} del cuerpo de error de Laravel */
    private String extraerMensaje(Response<?> r) {
        try {
            if (r.errorBody() != null) {
                JSONObject o = new JSONObject(r.errorBody().string());
                if (o.has("message")) return o.getString("message");
            }
        } catch (Exception ignored) { }
        return "No se pudo completar la operación (código " + r.code() + ")";
    }

    private String corta(String fecha) {
        return (fecha != null && fecha.length() >= 16) ? fecha.substring(0, 16) : "";
    }

    private void scrollAbajo() { svContent.post(() -> svContent.fullScroll(View.FOCUS_DOWN)); }

    private void msg(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }
}