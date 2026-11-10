package com.example.mobile_project;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mobile_project.shared.clients.ApiCallback;
import com.example.mobile_project.shared.clients.ApiClient;
import com.example.mobile_project.shared.session.SessionManager;
import com.example.mobile_project.usuarios.ApiMessage;
import com.example.mobile_project.usuarios.CatalogoItem;
import com.example.mobile_project.usuarios.UsuarioDetalleResponse;
import com.example.mobile_project.usuarios.UsuarioItem;
import com.example.mobile_project.usuarios.UsuarioRequest;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;

/** Crear (sin extra) o editar (con EXTRA_USUARIO_ID) un usuario. */
public class UsuarioFormActivity extends AppCompatActivity {

    public static final String EXTRA_USUARIO_ID = "usuario_id";

    private SessionManager session;

    private TextView txtTituloForm;
    private ImageView imgFoto;
    private RadioGroup rgGenero;
    private RadioButton rbMujer;
    private EditText etNombres, etApellidos, etEmail, etPassword, etCargo, etTelefono;
    private Spinner spRol, spArea, spNivel;
    private LinearLayout layoutNivel;
    private SwitchCompat swActivo;
    private Button btnGuardar, btnEliminar, btnCancelar;

    private final List<CatalogoItem> roles = new ArrayList<>();
    private final List<CatalogoItem> areas = new ArrayList<>();

    private long usuarioId = -1;
    private boolean modoEdicion = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_usuario_form);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        session = new SessionManager(this);

        View footer = findViewById(R.id.includeFooter);
        if (footer != null) FooterHelper.setupFooter(this, footer);

        txtTituloForm = findViewById(R.id.txtTituloForm);
        imgFoto = findViewById(R.id.imgFoto);
        rgGenero = findViewById(R.id.rgGenero);
        rbMujer = findViewById(R.id.rbMujer);
        etNombres = findViewById(R.id.etNombres);
        etApellidos = findViewById(R.id.etApellidos);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etCargo = findViewById(R.id.etCargo);
        etTelefono = findViewById(R.id.etTelefono);
        spRol = findViewById(R.id.spRol);
        spArea = findViewById(R.id.spArea);
        spNivel = findViewById(R.id.spNivel);
        layoutNivel = findViewById(R.id.layoutNivel);
        swActivo = findViewById(R.id.swActivoForm);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnEliminar = findViewById(R.id.btnEliminarForm);
        btnCancelar = findViewById(R.id.btnCancelar);

        usuarioId = getIntent().getLongExtra(EXTRA_USUARIO_ID, -1);
        modoEdicion = usuarioId > 0;

        if (modoEdicion) {
            txtTituloForm.setText("Editar Usuario");
            etPassword.setHint("Nueva contraseña (opcional)");
            btnEliminar.setVisibility(View.VISIBLE);
        }

        spNivel.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Nivel 1", "Nivel 2", "Nivel 3"}));

        rgGenero.setOnCheckedChangeListener((g, checkedId) -> actualizarFoto());
        actualizarFoto();

        spRol.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                boolean esTecnico = pos >= 0 && pos < roles.size()
                        && UsuarioItem.ROL_TECNICO.equals(roles.get(pos).codigo);
                layoutNivel.setVisibility(esTecnico ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> p) { }
        });

        btnGuardar.setOnClickListener(v -> guardar());
        btnCancelar.setOnClickListener(v -> finish());
        btnEliminar.setOnClickListener(v -> confirmarEliminar());

        cargarRoles();
    }

    private void actualizarFoto() {
        imgFoto.setImageResource(rbMujer.isChecked() ? R.drawable.mujer_foto : R.drawable.hombre_foto);
    }

    // ---------- Carga encadenada: roles -> áreas -> usuario (si edita) ----------

    private void cargarRoles() {
        ApiClient.getService().getRoles(session.getBearer())
                .enqueue(new ApiCallback<List<CatalogoItem>>(this) {
                    @Override
                    public void onSuccess(List<CatalogoItem> body) {
                        roles.clear();
                        if (body != null) roles.addAll(body);
                        spRol.setAdapter(new ArrayAdapter<>(UsuarioFormActivity.this,
                                android.R.layout.simple_spinner_dropdown_item, roles));
                        cargarAreas();
                    }
                    @Override public void onError(String m, int c) { super.onError(m, c); finish(); }
                    @Override protected String mensajePorDefecto() { return "No se pudieron cargar los roles"; }
                });
    }

    private void cargarAreas() {
        ApiClient.getService().getAreas(session.getBearer())
                .enqueue(new ApiCallback<List<CatalogoItem>>(this) {
                    @Override
                    public void onSuccess(List<CatalogoItem> body) {
                        areas.clear();
                        areas.add(new CatalogoItem(null, "Sin área"));
                        if (body != null) areas.addAll(body);
                        spArea.setAdapter(new ArrayAdapter<>(UsuarioFormActivity.this,
                                android.R.layout.simple_spinner_dropdown_item, areas));
                        if (modoEdicion) cargarUsuario();
                    }
                    @Override public void onError(String m, int c) { super.onError(m, c); finish(); }
                    @Override protected String mensajePorDefecto() { return "No se pudieron cargar las áreas"; }
                });
    }

    private void cargarUsuario() {
        ApiClient.getService().getUsuario(session.getBearer(), usuarioId)
                .enqueue(new ApiCallback<UsuarioItem>(this) {
                    @Override
                    public void onSuccess(UsuarioItem body) {
                        if (body != null) pintarUsuario(body);
                    }
                    @Override public void onError(String m, int c) { super.onError(m, c); finish(); }
                    @Override protected String mensajePorDefecto() { return "No se pudo cargar el usuario"; }
                });
    }

    private void pintarUsuario(UsuarioItem u) {
        etNombres.setText(u.nombres);
        etApellidos.setText(u.apellidos);
        etEmail.setText(u.email);
        etCargo.setText(u.cargo);
        etTelefono.setText(u.telefono);
        swActivo.setChecked(u.activo);

        if (u.genero != null && u.genero == 2) rbMujer.setChecked(true);
        else ((RadioButton) findViewById(R.id.rbHombre)).setChecked(true);
        actualizarFoto();

        for (int i = 0; i < roles.size(); i++) {
            if (roles.get(i).id != null && roles.get(i).id == u.rolId) { spRol.setSelection(i); break; }
        }
        for (int i = 0; i < areas.size(); i++) {
            if (u.areaId != null && u.areaId.equals(areas.get(i).id)) { spArea.setSelection(i); break; }
        }
        if (u.nivelSoporte != null && u.nivelSoporte >= 1 && u.nivelSoporte <= 3) {
            spNivel.setSelection(u.nivelSoporte - 1);
        }
    }

    // ---------- Guardar (crear / actualizar) ----------

    private void guardar() {
        String nombres = etNombres.getText().toString().trim();
        String apellidos = etApellidos.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (nombres.isEmpty()) { etNombres.setError("Obligatorio"); etNombres.requestFocus(); return; }
        if (apellidos.isEmpty()) { etApellidos.setError("Obligatorio"); etApellidos.requestFocus(); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Correo inválido"); etEmail.requestFocus(); return;
        }
        if ((!modoEdicion || !password.isEmpty()) && password.length() < 6) {
            etPassword.setError("Mínimo 6 caracteres"); etPassword.requestFocus(); return;
        }
        if (roles.isEmpty()) { Toast.makeText(this, "Roles no cargados", Toast.LENGTH_SHORT).show(); return; }

        CatalogoItem rol = roles.get(spRol.getSelectedItemPosition());
        CatalogoItem area = areas.isEmpty() ? null : areas.get(spArea.getSelectedItemPosition());

        UsuarioRequest body = new UsuarioRequest();
        body.nombres = nombres;
        body.apellidos = apellidos;
        body.email = email;
        body.password = password.isEmpty() ? null : password;
        body.rolId = rol.id;
        body.areaId = (area != null) ? area.id : null;
        body.cargo = etCargo.getText().toString().trim();
        body.telefono = etTelefono.getText().toString().trim();
        body.genero = rbMujer.isChecked() ? 2 : 1;
        body.activo = swActivo.isChecked();
        body.nivelSoporte = UsuarioItem.ROL_TECNICO.equals(rol.codigo)
                ? spNivel.getSelectedItemPosition() + 1 : null;

        btnGuardar.setEnabled(false);

        Call<UsuarioDetalleResponse> call = modoEdicion
                ? ApiClient.getService().updateUsuario(session.getBearer(), usuarioId, body)
                : ApiClient.getService().createUsuario(session.getBearer(), body);

        call.enqueue(new ApiCallback<UsuarioDetalleResponse>(this) {
            @Override
            public void onSuccess(UsuarioDetalleResponse b) {
                Toast.makeText(UsuarioFormActivity.this,
                        modoEdicion ? "Usuario actualizado" : "Usuario creado", Toast.LENGTH_SHORT).show();
                finish();
            }
            @Override public void onFinish() { btnGuardar.setEnabled(true); }
            @Override protected String mensajePorDefecto() { return "No se pudo guardar"; }
        });
    }

    // ---------- Eliminar ----------

    private void confirmarEliminar() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar usuario")
                .setMessage("¿Seguro que deseas eliminar este usuario? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (d, w) -> eliminar())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminar() {
        ApiClient.getService().deleteUsuario(session.getBearer(), usuarioId)
                .enqueue(new ApiCallback<ApiMessage>(this) {
                    @Override
                    public void onSuccess(ApiMessage body) {
                        Toast.makeText(UsuarioFormActivity.this, "Usuario eliminado", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    @Override protected String mensajePorDefecto() { return "No se pudo eliminar"; }
                });
    }
}