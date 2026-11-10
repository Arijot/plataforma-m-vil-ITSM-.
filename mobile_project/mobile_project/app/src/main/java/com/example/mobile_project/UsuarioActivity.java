package com.example.mobile_project;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_project.shared.clients.ApiCallback;
import com.example.mobile_project.shared.clients.ApiClient;
import com.example.mobile_project.shared.session.SessionManager;
import com.example.mobile_project.usuarios.ApiMessage;
import com.example.mobile_project.usuarios.UsuarioDetalleResponse;
import com.example.mobile_project.usuarios.UsuarioItem;
import com.example.mobile_project.usuarios.UsuariosListResponse;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;

/** Lista de usuarios (con foto) + buscar + filtrar + crear/editar/eliminar/activar. */
public class UsuarioActivity extends AppCompatActivity implements UsuariosAdapter.Listener {

    private SessionManager session;

    private EditText etBuscar;
    private Spinner spEstado;
    private TextView txtTotal, txtVacio;
    private ProgressBar progress;
    private RecyclerView rv;
    private FloatingActionButton fabNuevo;

    private UsuariosAdapter adapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable buscarPendiente;
    private Call<UsuariosListResponse> callLista;   // para cancelar búsquedas anteriores

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_usuario);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        session = new SessionManager(this);

        // Footer global (botones según el rol, igual que en TicketsActivity)
        View footer = findViewById(R.id.includeFooter);
        if (footer != null) FooterHelper.setupFooter(this, footer);

        etBuscar = findViewById(R.id.etBuscar);
        spEstado = findViewById(R.id.spEstado);
        txtTotal = findViewById(R.id.txtTotal);
        txtVacio = findViewById(R.id.txtVacio);
        progress = findViewById(R.id.progress);
        rv = findViewById(R.id.rvUsuarios);
        fabNuevo = findViewById(R.id.fabNuevo);

        adapter = new UsuariosAdapter(this);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        spEstado.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Todos", "Activos", "Inactivos"}));
        spEstado.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { cargarUsuarios(); }
            @Override public void onNothingSelected(AdapterView<?> p) { }
        });

        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                if (buscarPendiente != null) handler.removeCallbacks(buscarPendiente);
                buscarPendiente = UsuarioActivity.this::cargarUsuarios;
                handler.postDelayed(buscarPendiente, 400);
            }
        });

        fabNuevo.setOnClickListener(v ->
                startActivity(new Intent(this, UsuarioFormActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarUsuarios(); // refresca al volver del formulario
    }

    private void cargarUsuarios() {
        if (callLista != null) callLista.cancel();   // evita que una respuesta vieja pise a la nueva

        String buscar = etBuscar.getText().toString().trim();
        Integer activo = null; // Todos
        int pos = spEstado.getSelectedItemPosition();
        if (pos == 1) activo = 1;
        if (pos == 2) activo = 0;

        progress.setVisibility(View.VISIBLE);
        callLista = ApiClient.getService().getUsuarios(
                session.getBearer(), buscar.isEmpty() ? null : buscar, null, activo, 1, 200);

        callLista.enqueue(new ApiCallback<UsuariosListResponse>(this) {
            @Override
            public void onSuccess(UsuariosListResponse body) {
                if (body == null) return;
                adapter.setItems(body.data);
                txtTotal.setText(body.total + (body.total == 1 ? " usuario" : " usuarios"));
                txtVacio.setVisibility(adapter.isEmpty() ? View.VISIBLE : View.GONE);
            }
            @Override public void onFinish() { progress.setVisibility(View.GONE); }
            @Override protected String mensajePorDefecto() { return "No se pudo cargar la lista"; }
        });
    }

    // ---------------- Acciones del adapter ----------------

    @Override
    public void onEditar(UsuarioItem u) {
        Intent i = new Intent(this, UsuarioFormActivity.class);
        i.putExtra(UsuarioFormActivity.EXTRA_USUARIO_ID, u.id);
        startActivity(i);
    }

    @Override
    public void onEliminar(UsuarioItem u) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar usuario")
                .setMessage("¿Eliminar a " + u.getNombreMostrar() + "? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (d, w) -> eliminar(u))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminar(UsuarioItem u) {
        ApiClient.getService().deleteUsuario(session.getBearer(), u.id)
                .enqueue(new ApiCallback<ApiMessage>(this) {
                    @Override
                    public void onSuccess(ApiMessage body) {
                        Toast.makeText(UsuarioActivity.this, "Usuario eliminado", Toast.LENGTH_SHORT).show();
                        cargarUsuarios();
                    }
                    @Override
                    public void onError(String message, int httpCode) {
                        // 409: tiene historial -> el mensaje del servidor sugiere desactivarlo
                        super.onError(message, httpCode);
                        cargarUsuarios();
                    }
                    @Override protected String mensajePorDefecto() { return "No se pudo eliminar"; }
                });
    }

    @Override
    public void onCambiarEstado(UsuarioItem u, boolean activo) {
        Map<String, Object> body = new HashMap<>();
        body.put("activo", activo);

        ApiClient.getService().setEstadoUsuario(session.getBearer(), u.id, body)
                .enqueue(new ApiCallback<UsuarioDetalleResponse>(this) {
                    @Override public void onSuccess(UsuarioDetalleResponse b) { cargarUsuarios(); }
                    @Override
                    public void onError(String message, int httpCode) {
                        super.onError(message, httpCode);
                        cargarUsuarios();   // revierte el switch al estado real
                    }
                    @Override protected String mensajePorDefecto() { return "No se pudo cambiar el estado"; }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (buscarPendiente != null) handler.removeCallbacks(buscarPendiente);
        if (callLista != null) callLista.cancel();
    }
}