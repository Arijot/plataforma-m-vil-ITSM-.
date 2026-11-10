package com.example.mobile_project;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mobile_project.login.LoginRequest;
import com.example.mobile_project.login.LoginResponse;
import com.example.mobile_project.shared.clients.ApiClient;
import com.example.mobile_project.user.User;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private EditText edtEmail;
    private EditText edtPassword;
    private Button btnIngresar;
    private LinearLayout layoutErrorBanner;
    private TextView txtErrorMessage;

    private static final String PREFS_NAME = "SoporteClinicaPrefs";
    private static final String KEY_TOKEN = "access_token";

    private static final String KEY_ROL_ID = "rol_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedToken = preferences.getString(KEY_TOKEN, null);
        if (savedToken != null && !savedToken.isEmpty()) {
            irATickets();
            return;
        }

        // Vincular componentes del XML
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnIngresar = findViewById(R.id.btnIngresar);
        layoutErrorBanner = findViewById(R.id.layoutErrorBanner);
        txtErrorMessage = findViewById(R.id.txtErrorMessage);

        btnIngresar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                realizarLogin();
            }
        });
    }

    private void realizarLogin() {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            mostrarError("Por favor ingrese correo y contraseña", null);
            return;
        }

        // Ocultar banner previo de error mientras procesa
        layoutErrorBanner.setVisibility(View.GONE);

        LoginRequest request = new LoginRequest(email, password);

        ApiClient.getService().login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginData = response.body();
                    String token = loginData.getAccessToken();
                    User user = loginData.getUser();

                    if (token != null && !token.isEmpty()) {
                        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();

                        // 1. Guardar Access Token
                        editor.putString(KEY_TOKEN, token);

                        // 2. Guardar objeto User completo serializado a JSON
                        if (user != null) {
                            Gson gson = new Gson();
                            String userJson = gson.toJson(user);
                            editor.putString("user_data", userJson);

                            // Guardar campos directo para acceso rápido
                            editor.putLong("user_id", user.getId());
                            editor.putInt("rol_id", user.getRolId());
                            editor.putString("user_email", user.getEmail());
                            editor.putString("user_nombre_completo", user.getNombres() + " " + user.getApellidos());
                            editor.putString("user_cargo", user.getCargo());
                            editor.putString("user_telefono", user.getTelefono());
                            if (user.getGenero() != null) {
                                editor.putInt("user_genero", user.getGenero());
                            }
                        }

                        editor.apply();

                        Toast.makeText(MainActivity.this, "Login exitoso", Toast.LENGTH_SHORT).show();
                        irATickets();
                    } else {
                        mostrarError("No se recibió un token de acceso válido", "SSO-001");
                    }
                } else {
                    try {
                        String errorJson = response.errorBody().string();
                        Gson gson = new Gson();
                        LoginResponse errorResponse = gson.fromJson(errorJson, LoginResponse.class);

                        String mensaje = (errorResponse != null && errorResponse.getMessage() != null)
                                ? errorResponse.getMessage()
                                : "Error al autenticar";

                        String codigo = (errorResponse != null && errorResponse.getErrorCode() != null)
                                ? errorResponse.getErrorCode()
                                : "SSO-001";

                        mostrarError(mensaje, codigo);
                    } catch (Exception e) {
                        mostrarError("Error de comunicación con el servicio de autenticación", "SSO-005");
                    }
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                // Fallo de red / servidor caído
                mostrarError("Error de comunicación con el servicio de autenticación", "SSO-005");
            }
        });
    }

    private void irATickets() {
        Intent intent = new Intent(MainActivity.this, TicketsActivity.class);
        startActivity(intent);
        finish(); // Cierra el Login para que no regrese al presionar el botón atrás
    }

    private void mostrarError(String mensaje, String codigoError) {
        String textoFormateado;
        if (codigoError != null) {
            textoFormateado = mensaje + "\nError: " + codigoError;
        } else {
            textoFormateado = mensaje;
        }

        // Mostrar banner dentro del layout
        txtErrorMessage.setText(textoFormateado);
        layoutErrorBanner.setVisibility(View.VISIBLE);

        // Mostrar Toast emergente
        Toast.makeText(MainActivity.this, textoFormateado, Toast.LENGTH_LONG).show();
    }
}