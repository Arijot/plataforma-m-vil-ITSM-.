package com.example.mobile_project;

import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.mobile_project.dashboard.DashboardResponse;
import com.example.mobile_project.dashboard.DashboardResponse.Kpis;
import com.example.mobile_project.dashboard.DashboardResponse.Opcion;
import com.example.mobile_project.dashboard.DashboardResponse.Prioridad;
import com.example.mobile_project.dashboard.DashboardResponse.Servicio;
import com.example.mobile_project.dashboard.DonutChartView;
import com.example.mobile_project.shared.clients.ApiClient;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Dashboard de SLA (solo ADMINISTRADOR_TI). Datos: GET api/dashboard/sla  ·  Excel: GET api/dashboard/sla/exportar */
public class DashboardSlaActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "SoporteClinicaPrefs";
    private static final String KEY_TOKEN = "access_token";
    private static final String KEY_ROL_ID = "rol_id";
    private static final String XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final String[] PERIODOS = {"semana", "mes", "anio"};

    private static final int VERDE = 0xFF00C853, VERDE_OSCURO = 0xFF2E7D32,
            NARANJA = 0xFFFFA726, ROJO = 0xFFE53935;

    // vistas
    private TextView segSemana, segMes, segAnio;
    private Spinner spinnerServicio, spinnerPrioridad;
    private TextView txtVariacion, txtCumplimiento, txtMeta, txtAtendidos, txtMttr, txtFuera, txtFueraDetalle,
            txtLimite, txtServiciosVacio;
    private ProgressBar progCumplimiento, progAtendidos, progressCarga;
    private DonutChartView donut;
    private LinearLayout layoutChipsPrioridad, layoutServicios;
    private Button btnExportar;

    // estado
    private String authHeader;
    private String periodo = "mes";
    private Integer servicioId = null, prioridadId = null;
    private boolean filtrosListos = false;
    private int requestSeq = 0;           // descarta respuestas viejas si el admin cambia rápido de filtro
    private final List<Integer> idsServicio = new ArrayList<>();
    private final List<Integer> idsPrioridad = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_sla);

        View footer = findViewById(R.id.includeFooter);
        if (footer != null) FooterHelper.setupFooter(this, footer);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String token = prefs.getString(KEY_TOKEN, null);
        if (prefs.getInt(KEY_ROL_ID, -1) != 3) {
            msg("Solo el administrador de TI puede ver el dashboard");
            finish();
            return;
        }
        if (token == null || token.isEmpty()) {
            msg("Sesión no válida");
            finish();
            return;
        }
        authHeader = "Bearer " + token;

        enlazarVistas();
        configurarListeners();
        pintarSegmentos();
        cargar();
    }

    private void enlazarVistas() {
        segSemana = findViewById(R.id.segSemana);
        segMes = findViewById(R.id.segMes);
        segAnio = findViewById(R.id.segAnio);
        spinnerServicio = findViewById(R.id.spinnerServicio);
        spinnerPrioridad = findViewById(R.id.spinnerPrioridad);
        txtVariacion = findViewById(R.id.txtVariacion);
        txtCumplimiento = findViewById(R.id.txtCumplimiento);
        txtMeta = findViewById(R.id.txtMeta);
        txtAtendidos = findViewById(R.id.txtAtendidos);
        txtMttr = findViewById(R.id.txtMttr);
        txtFuera = findViewById(R.id.txtFuera);
        txtFueraDetalle = findViewById(R.id.txtFueraDetalle);
        txtLimite = findViewById(R.id.txtLimite);
        txtServiciosVacio = findViewById(R.id.txtServiciosVacio);
        progCumplimiento = findViewById(R.id.progCumplimiento);
        progAtendidos = findViewById(R.id.progAtendidos);
        progressCarga = findViewById(R.id.progressCarga);
        donut = findViewById(R.id.donutPrioridad);
        layoutChipsPrioridad = findViewById(R.id.layoutChipsPrioridad);
        layoutServicios = findViewById(R.id.layoutServicios);
        btnExportar = findViewById(R.id.btnExportar);
    }

    private void configurarListeners() {
        TextView[] segs = {segSemana, segMes, segAnio};
        for (int i = 0; i < segs.length; i++) {
            final String p = PERIODOS[i];
            segs[i].setOnClickListener(v -> {
                if (p.equals(periodo)) return;
                periodo = p;
                pintarSegmentos();
                cargar();
            });
        }

        spinnerServicio.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                Integer nuevo = pos > 0 && pos <= idsServicio.size() ? idsServicio.get(pos - 1) : null;
                if (java.util.Objects.equals(nuevo, servicioId)) return;   // evita recargar por el evento inicial
                servicioId = nuevo;
                cargar();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        spinnerPrioridad.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                Integer nuevo = pos > 0 && pos <= idsPrioridad.size() ? idsPrioridad.get(pos - 1) : null;
                if (java.util.Objects.equals(nuevo, prioridadId)) return;
                prioridadId = nuevo;
                cargar();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        btnExportar.setOnClickListener(v -> exportar());
    }

    private void pintarSegmentos() {
        TextView[] segs = {segSemana, segMes, segAnio};
        for (int i = 0; i < segs.length; i++) {
            boolean on = PERIODOS[i].equals(periodo);
            segs[i].setBackgroundResource(on ? R.drawable.bg_seg_on : R.drawable.bg_seg_off);
            segs[i].setTextColor(on ? Color.WHITE : 0xFF333333);
            segs[i].setTypeface(null, on ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
    }

    // ------------------------------------------------------------------ datos

    private void cargar() {
        final int seq = ++requestSeq;
        progressCarga.setVisibility(View.VISIBLE);

        ApiClient.getService().getDashboardSla(authHeader, periodo, servicioId, prioridadId)
                .enqueue(new Callback<DashboardResponse>() {
                    @Override
                    public void onResponse(Call<DashboardResponse> call, Response<DashboardResponse> r) {
                        if (seq != requestSeq) return;
                        progressCarga.setVisibility(View.INVISIBLE);
                        if (!r.isSuccessful() || r.body() == null) {
                            msg(extraerMensaje(r));
                            return;
                        }
                        pintar(r.body());
                    }

                    @Override
                    public void onFailure(Call<DashboardResponse> call, Throwable t) {
                        if (seq != requestSeq) return;
                        progressCarga.setVisibility(View.INVISIBLE);
                        msg("Error de conexión con el servidor");
                    }
                });
    }

    private void pintar(DashboardResponse d) {
        if (!filtrosListos && d.getFiltros() != null) {
            llenarSpinner(spinnerServicio, "Todos los servicios", d.getFiltros().getServicios(), idsServicio);
            llenarSpinner(spinnerPrioridad, "Todas las prioridades", d.getFiltros().getPrioridades(), idsPrioridad);
            filtrosListos = true;
        }

        final int meta = d.getMeta();
        Kpis k = d.getKpis();

        // ---- Cumplimiento global
        Double cum = k.getCumplimiento();
        if (cum == null) {
            txtCumplimiento.setText("—");
            txtMeta.setText("Sin tickets evaluados");
            txtMeta.setTextColor(0xFF777777);
            progCumplimiento.setProgress(0);
        } else {
            boolean cumple = cum >= meta;
            int color = cumple ? VERDE_OSCURO : ROJO;
            txtCumplimiento.setText(String.format(Locale.US, "%.1f%%", cum));
            txtMeta.setText(cumple ? "Meta ITIL (" + meta + "%) OK" : "Bajo la meta ITIL (" + meta + "%)");
            txtMeta.setTextColor(color);
            progCumplimiento.setProgress((int) Math.round(cum * 10));
            progCumplimiento.setProgressTintList(ColorStateList.valueOf(cumple ? VERDE : ROJO));
        }

        // ---- Variación vs periodo anterior
        Double var = k.getVariacion();
        if (var == null) {
            txtVariacion.setVisibility(View.GONE);
        } else {
            txtVariacion.setText(String.format(Locale.US, "%s %.1f pts", var >= 0 ? "▲" : "▼", Math.abs(var)));
            txtVariacion.setBackgroundTintList(ColorStateList.valueOf(var >= 0 ? VERDE : ROJO));
            txtVariacion.setVisibility(View.VISIBLE);
        }

        // ---- Atendidos a tiempo
        txtAtendidos.setText(k.getEvaluados() > 0 ? k.getDentroSla() + "/" + k.getEvaluados() : "—");
        progAtendidos.setProgress(k.getEvaluados() > 0 ? k.getDentroSla() * 1000 / k.getEvaluados() : 0);

        // ---- MTTR
        txtMttr.setText(formatoDuracion(k.getMttrMin()));

        // ---- Fuera de SLA
        txtFuera.setText(k.getFueraSla() + (k.getFueraSla() == 1 ? " Ticket" : " Tickets"));
        txtFueraDetalle.setText(k.getFueraSla() > 0
                ? k.getResueltosTarde() + " resueltos tarde · " + k.getAbiertosVencidos() + " abiertos vencidos"
                : "");

        pintarPrioridades(d.getPorPrioridad());
        pintarServicios(d.getPorServicio(), meta);
        txtLimite.setText("Límite: " + meta + "%");
    }

    private void llenarSpinner(Spinner sp, String primero, List<Opcion> opciones, List<Integer> ids) {
        ids.clear();
        List<String> nombres = new ArrayList<>();
        nombres.add(primero);
        if (opciones != null) {
            for (Opcion o : opciones) { ids.add(o.getId()); nombres.add(o.getNombre()); }
        }
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, nombres);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(ad);
    }

    private void pintarPrioridades(List<Prioridad> lista) {
        layoutChipsPrioridad.removeAllViews();
        List<Float> valores = new ArrayList<>();
        List<Integer> colores = new ArrayList<>();
        int total = 0;

        if (lista != null) {
            for (Prioridad p : lista) {
                int color = parsearColor(p.getColorHex());
                valores.add((float) p.getTotal());
                colores.add(color);
                total += p.getTotal();
                layoutChipsPrioridad.addView(crearChip(p, color));
            }
        }
        donut.setDatos(valores, colores, total > 0 ? String.valueOf(total) : "0");
    }

    private TextView crearChip(Prioridad p, int color) {
        TextView chip = new TextView(this);
        String pct = p.getCumplimiento() != null ? String.format(Locale.US, "%.1f%%", p.getCumplimiento()) : "—";
        chip.setText(p.getNombre() + "\n" + pct + " SLA");
        chip.setTextSize(11);
        chip.setTextColor(color);
        chip.setTypeface(null, android.graphics.Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(8), dp(4), dp(8), dp(4));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setStroke(dp(2), color);
        bg.setCornerRadius(dp(8));
        chip.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(6);
        chip.setLayoutParams(lp);
        return chip;
    }

    private void pintarServicios(List<Servicio> lista, int meta) {
        layoutServicios.removeAllViews();
        boolean vacio = lista == null || lista.isEmpty();
        txtServiciosVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);
        if (vacio) return;

        for (Servicio s : lista) {
            Double pct = s.getCumplimiento();
            int color = pct == null ? 0xFFBDBDBD : pct >= meta ? VERDE : pct >= meta - 10 ? NARANJA : ROJO;

            LinearLayout fila = new LinearLayout(this);
            fila.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams lpFila = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lpFila.bottomMargin = dp(10);
            fila.setLayoutParams(lpFila);

            // nombre + porcentaje
            RelativeLayout cab = new RelativeLayout(this);
            TextView nombre = new TextView(this);
            nombre.setText(s.getNombre() + "  (" + s.getTotal() + ")");
            nombre.setTextSize(12);
            nombre.setTextColor(0xFF222222);
            nombre.setTypeface(null, android.graphics.Typeface.BOLD);
            cab.addView(nombre);

            TextView txtPct = new TextView(this);
            txtPct.setText(pct != null ? String.format(Locale.US, "%.1f%%", pct) : "—");
            txtPct.setTextSize(11);
            txtPct.setTextColor(color == VERDE ? VERDE_OSCURO : color);
            txtPct.setTypeface(null, android.graphics.Typeface.BOLD);
            RelativeLayout.LayoutParams lpPct = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            lpPct.addRule(RelativeLayout.ALIGN_PARENT_END);
            cab.addView(txtPct, lpPct);
            fila.addView(cab);

            // barra: pista gris + relleno proporcional (weightSum = 100)
            LinearLayout pista = new LinearLayout(this);
            pista.setOrientation(LinearLayout.HORIZONTAL);
            pista.setWeightSum(100f);
            GradientDrawable fondo = new GradientDrawable();
            fondo.setColor(0xFFE0E0E0);
            fondo.setCornerRadius(dp(6));
            pista.setBackground(fondo);
            LinearLayout.LayoutParams lpPista = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(10));
            lpPista.topMargin = dp(3);
            pista.setLayoutParams(lpPista);

            View relleno = new View(this);
            GradientDrawable rb = new GradientDrawable();
            rb.setColor(color);
            rb.setCornerRadius(dp(6));
            relleno.setBackground(rb);
            float peso = pct == null ? 0f : (float) Math.max(0, Math.min(100, pct));
            pista.addView(relleno, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, peso));
            fila.addView(pista);

            layoutServicios.addView(fila);
        }
    }

    // ------------------------------------------------------------------ exportar a Excel

    private void exportar() {
        btnExportar.setEnabled(false);
        msg("Generando reporte…");

        ApiClient.getService().exportarDashboardSla(authHeader, periodo, servicioId, prioridadId)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> r) {
                        if (!r.isSuccessful() || r.body() == null) {
                            msg(extraerMensaje(r));
                            btnExportar.setEnabled(true);
                            return;
                        }
                        final ResponseBody body = r.body();
                        // el archivo se escribe fuera del hilo principal
                        new Thread(() -> {
                            try {
                                final Uri uri = guardarExcel(body);
                                runOnUiThread(() -> { btnExportar.setEnabled(true); avisarGuardado(uri); });
                            } catch (Exception e) {
                                runOnUiThread(() -> { btnExportar.setEnabled(true); msg("No se pudo guardar el archivo"); });
                            }
                        }).start();
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        btnExportar.setEnabled(true);
                        msg("Error de conexión al generar el reporte");
                    }
                });
    }

    /** Android 10+: carpeta Descargas (MediaStore, sin permisos). Antes: carpeta propia de la app + FileProvider. */
    private Uri guardarExcel(ResponseBody body) throws Exception {
        String nombre = "reporte_sla_" + periodo + "_"
                + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".xlsx";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues v = new ContentValues();
            v.put(MediaStore.MediaColumns.DISPLAY_NAME, nombre);
            v.put(MediaStore.MediaColumns.MIME_TYPE, XLSX_MIME);
            v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
            if (uri == null) throw new IllegalStateException("MediaStore no devolvió Uri");
            try (OutputStream os = getContentResolver().openOutputStream(uri); InputStream is = body.byteStream()) {
                copiar(is, os);
            }
            return uri;
        }

        File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (dir != null && !dir.exists()) dir.mkdirs();
        File f = new File(dir, nombre);
        try (OutputStream os = new FileOutputStream(f); InputStream is = body.byteStream()) {
            copiar(is, os);
        }
        return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", f);
    }

    private void copiar(InputStream is, OutputStream os) throws Exception {
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) > 0) os.write(buf, 0, n);
        os.flush();
    }

    private void avisarGuardado(final Uri uri) {
        String donde = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? "en tu carpeta Descargas" : "en la carpeta de la app";
        new AlertDialog.Builder(this)
                .setTitle("Reporte generado")
                .setMessage("El Excel se guardó " + donde + ".")
                .setPositiveButton("Abrir", (d, w) -> {
                    Intent i = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, XLSX_MIME)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try { startActivity(i); }
                    catch (ActivityNotFoundException e) { msg("No hay una app para abrir Excel; usa Compartir"); }
                })
                .setNeutralButton("Compartir", (d, w) -> {
                    Intent i = new Intent(Intent.ACTION_SEND).setType(XLSX_MIME)
                            .putExtra(Intent.EXTRA_STREAM, uri)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(i, "Compartir reporte"));
                })
                .setNegativeButton("Cerrar", null)
                .show();
    }

    // ------------------------------------------------------------------ utilidades

    /** 105 -> "1h 45m"; 45 -> "45m"; 3000 -> "2d 2h" */
    private String formatoDuracion(Integer min) {
        if (min == null) return "—";
        if (min >= 1440) return (min / 1440) + "d " + ((min % 1440) / 60) + "h";
        if (min >= 60) return (min / 60) + "h " + (min % 60) + "m";
        return min + "m";
    }

    private int parsearColor(String hex) {
        try { return Color.parseColor(hex); } catch (Exception e) { return 0xFF9E9E9E; }
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

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

    private void msg(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }
}