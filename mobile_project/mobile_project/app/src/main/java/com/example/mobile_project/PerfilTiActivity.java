package com.example.mobile_project;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mobile_project.shared.clients.ApiClient;
import com.example.mobile_project.user.User;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PerfilTiActivity extends AppCompatActivity {

    // Vistas actualizadas según activity_perfil_ti.xml
    private ImageView imgProfileTech;
    private TextView txtNameTech;
    private TextView txtRoleTech;
    private TextView txtEmailTech;
    private Button btnListaTickets;
    private Button btnActualizarInfo;
    private Button btnCerrarSesionTech;

    private static final String PREFS_NAME = "SoporteClinicaPrefs";
    private static final String KEY_TOKEN = "access_token";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_perfil_ti);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Vinculación de vistas con los IDs del nuevo XML de TI
        imgProfileTech = findViewById(R.id.imgProfileTech);
        txtNameTech = findViewById(R.id.txtNameTech);
        txtRoleTech = findViewById(R.id.txtRoleTech);
        txtEmailTech = findViewById(R.id.txtEmailTech);
        btnListaTickets = findViewById(R.id.btnListaTickets);
        btnActualizarInfo = findViewById(R.id.btnActualizarInfo);
        btnCerrarSesionTech = findViewById(R.id.btnCerrarSesionTech);

        // 2. Cargar datos del usuario desde SharedPreferences
        cargarDatosUsuario();

        // 3. Botón: Lista de Tickets Registrados -> Navega a TicketsActivity (o la actividad correspondiente)
        btnListaTickets.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Puedes cambiar TicketsActivity por la vista de lista de tickets de TI si difiere
                Intent intent = new Intent(PerfilTiActivity.this, TicketsActivity.class);
                startActivity(intent);
            }
        });

        // 4. Botón: Actualizar Información -> Aquí puedes definir la navegación a su respectiva Activity
        btnActualizarInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Ejemplo de navegación o Toast indicativo (ajústalo según tu proyecto)
                Toast.makeText(PerfilTiActivity.this, "Función de actualizar información", Toast.LENGTH_SHORT).show();
                // Intent intent = new Intent(PerfilTiActivity.this, ActualizarPerfilActivity.class);
                // startActivity(intent);
            }
        });

        // 5. Botón: Cerrar Sesión -> Ejecuta Logout
        btnCerrarSesionTech.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ejecutarLogout();
            }
        });
    }

    private void cargarDatosUsuario() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String userJson = preferences.getString("user_data", null);

        if (userJson != null) {
            Gson gson = new Gson();
            User user = gson.fromJson(userJson, User.class);

            if (user != null) {
                // Pintar Nombre Completo
                String nombreCompleto = user.getNombres() + " " + user.getApellidos();
                txtNameTech.setText(nombreCompleto);

                // Pintar Cargo / Rol específico para TI
                if (user.getCargo() != null && !user.getCargo().isEmpty()) {
                    txtRoleTech.setText(user.getCargo());
                } else {
                    txtRoleTech.setText("Técnico. Área de\nSistemas");
                }

                // Pintar Correo Electrónico
                txtEmailTech.setText(user.getEmail());

                // Validar Género: 1 = hombre_foto, 2 = mujer_foto
                if (user.getGenero() != null) {
                    if (user.getGenero() == 1) {
                        imgProfileTech.setImageResource(R.drawable.hombre_foto);
                    } else if (user.getGenero() == 2) {
                        imgProfileTech.setImageResource(R.drawable.mujer_foto);
                    }
                }
            }
        }
    }

    private void ejecutarLogout() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String token = preferences.getString(KEY_TOKEN, null);

        if (token != null && !token.isEmpty()) {
            String authToken = "Bearer " + token;

            // Llamada al endpoint de Logout usando Retrofit
            ApiClient.getService().logout(authToken).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    limpiarSesionYSalir();
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    // Aunque falle la red, cerramos la sesión localmente por seguridad
                    limpiarSesionYSalir();
                }
            });
        } else {
            limpiarSesionYSalir();
        }
    }

    private void limpiarSesionYSalir() {
        // Borrar datos guardados en SharedPreferences
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();

        Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();

        // Redirigir al Login (MainActivity) y limpiar el stack de pantallas
        Intent intent = new Intent(PerfilTiActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}