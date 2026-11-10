package com.example.mobile_project;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mobile_project.shared.clients.ApiClient;
import com.example.mobile_project.ticket.FormOptionsResponse;
import com.example.mobile_project.ticket.TicketCreateResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterTicketActivity extends AppCompatActivity {

    private EditText edtTicketTitle, edtUbicacion, edtDescription;
    private Spinner spinnerService, spinnerPriority;
    private Button btnAttachFile, btnCrearTicket;
    private LinearLayout layoutPreview;
    private ImageView imgPreview;
    private TextView txtFileName;

    private List<FormOptionsResponse.ServicioDTO> listaServicios = new ArrayList<>();
    private List<FormOptionsResponse.PrioridadDTO> listaPrioridades = new ArrayList<>();

    private Uri selectedImageUri = null;
    private static final String PREFS_NAME = "SoporteClinicaPrefs";
    private static final String KEY_TOKEN = "access_token";

    // Selector de imágenes de galería
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        imgPreview.setImageURI(selectedImageUri);
                        txtFileName.setText(getFileName(selectedImageUri));
                        layoutPreview.setVisibility(View.VISIBLE);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_ticket);

        // Bindings
        edtTicketTitle = findViewById(R.id.edtTicketTitle);
        edtUbicacion = findViewById(R.id.edtUbicacion);
        edtDescription = findViewById(R.id.edtDescription);
        spinnerService = findViewById(R.id.spinnerService);
        spinnerPriority = findViewById(R.id.spinnerPriority);
        btnAttachFile = findViewById(R.id.btnAttachFile);
        btnCrearTicket = findViewById(R.id.btnCrearTicket);
        layoutPreview = findViewById(R.id.layoutPreview);
        imgPreview = findViewById(R.id.imgPreview);
        txtFileName = findViewById(R.id.txtFileName);

        // Cargar combos
        cargarOpcionesFormulario();

        View footer = findViewById(R.id.includeFooter);

        if (footer != null) {
            // 1. Configurar visibilidad según el rol en SharedPreferences
            FooterHelper.setupFooter(this, footer);
        }


        // Evento seleccionar imagen
        btnAttachFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        // Evento crear ticket
        btnCrearTicket.setOnClickListener(v -> registrarTicket());
    }

    private void cargarOpcionesFormulario() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String token = preferences.getString(KEY_TOKEN, null);

        if (token != null) {
            ApiClient.getService().getFormOptions("Bearer " + token).enqueue(new Callback<FormOptionsResponse>() {
                @Override
                public void onResponse(Call<FormOptionsResponse> call, Response<FormOptionsResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        listaServicios = response.body().getServicios();
                        listaPrioridades = response.body().getPrioridades();

                        ArrayAdapter<FormOptionsResponse.ServicioDTO> adapterServicios = new ArrayAdapter<>(
                                RegisterTicketActivity.this, android.R.layout.simple_spinner_dropdown_item, listaServicios);
                        spinnerService.setAdapter(adapterServicios);

                        ArrayAdapter<FormOptionsResponse.PrioridadDTO> adapterPrioridades = new ArrayAdapter<>(
                                RegisterTicketActivity.this, android.R.layout.simple_spinner_dropdown_item, listaPrioridades);
                        spinnerPriority.setAdapter(adapterPrioridades);
                    }
                }

                @Override
                public void onFailure(Call<FormOptionsResponse> call, Throwable t) {
                    Toast.makeText(RegisterTicketActivity.this, "Error al cargar servicios y prioridades", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void registrarTicket() {
        String titulo = edtTicketTitle.getText().toString().trim();
        String ubicacion = edtUbicacion.getText().toString().trim();
        String descripcion = edtDescription.getText().toString().trim();

        if (titulo.isEmpty() || descripcion.isEmpty()) {
            Toast.makeText(this, "Por favor complete título y descripción", Toast.LENGTH_SHORT).show();
            return;
        }

        if (spinnerService.getSelectedItem() == null || spinnerPriority.getSelectedItem() == null) {
            Toast.makeText(this, "Seleccione un servicio y prioridad válidos", Toast.LENGTH_SHORT).show();
            return;
        }

        int servicioId = ((FormOptionsResponse.ServicioDTO) spinnerService.getSelectedItem()).getId();
        int prioridadId = ((FormOptionsResponse.PrioridadDTO) spinnerPriority.getSelectedItem()).getId();

        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String token = preferences.getString(KEY_TOKEN, null);

        if (token == null) return;

        // Construir RequestBody
        RequestBody tituloPart = RequestBody.create(MediaType.parse("text/plain"), titulo);
        RequestBody descPart = RequestBody.create(MediaType.parse("text/plain"), descripcion);
        RequestBody servPart = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(servicioId));
        RequestBody prioPart = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(prioridadId));
        RequestBody ubiPart = RequestBody.create(MediaType.parse("text/plain"), ubicacion);

        MultipartBody.Part filePart = null;
        if (selectedImageUri != null) {
            File file = getFileFromUri(selectedImageUri);
            if (file != null) {
                RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
                filePart = MultipartBody.Part.createFormData("archivo", file.getName(), requestFile);
            }
        }

        btnCrearTicket.setEnabled(false);

        ApiClient.getService().createTicket("Bearer " + token, tituloPart, descPart, servPart, prioPart, ubiPart, filePart)
                .enqueue(new Callback<TicketCreateResponse>() {
                    @Override
                    public void onResponse(Call<TicketCreateResponse> call, Response<TicketCreateResponse> response) {
                        btnCrearTicket.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(RegisterTicketActivity.this, "Ticket creado: " + response.body().getCodigo(), Toast.LENGTH_LONG).show();

                            // Redirigir al listado de tickets
                            Intent intent = new Intent(RegisterTicketActivity.this, TicketsActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(RegisterTicketActivity.this, "Error al registrar el ticket", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<TicketCreateResponse> call, Throwable t) {
                        btnCrearTicket.setEnabled(true);
                        Toast.makeText(RegisterTicketActivity.this, "Error de red al crear el ticket", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (var cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) result = cursor.getString(index);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }

    private File getFileFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            File file = new File(getCacheDir(), getFileName(uri));
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();
            return file;
        } catch (Exception e) {
            return null;
        }
    }
}