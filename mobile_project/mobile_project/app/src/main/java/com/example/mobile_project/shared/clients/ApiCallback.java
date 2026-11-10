package com.example.mobile_project.shared.clients;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.example.mobile_project.shared.session.SessionManager;
import com.example.mobile_project.usuarios.ApiMessage;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Callback base para todas las llamadas autenticadas. Centraliza:
 *  - ignorar respuestas si la Activity ya se cerró (evita crashes/Toasts fantasma)
 *  - ignorar llamadas canceladas
 *  - 401 -> sesión expirada, vuelve al login
 *  - extraer el "message" de errores de Laravel (422, 403, 409...)
 *  - mensaje amigable cuando falla la red
 *
 * Uso:
 *   ApiClient.getService().getRoles(bearer).enqueue(new ApiCallback<List<CatalogoItem>>(this) {
 *       @Override public void onSuccess(List<CatalogoItem> body) { ... }
 *   });
 *
 * Nota: onSuccess puede recibir body == null en respuestas sin cuerpo.
 */
public abstract class ApiCallback<T> implements Callback<T> {

    private final Context context;

    protected ApiCallback(Context context) {
        this.context = context;
    }

    /** Respuesta 2xx. */
    public abstract void onSuccess(T body);

    /** Error HTTP o de red (httpCode = 0 si no hubo respuesta). Por defecto muestra un Toast. */
    public void onError(String message, int httpCode) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    /** Se invoca siempre al terminar (éxito o error): ideal para ocultar progreso o reactivar botones. */
    public void onFinish() { }

    /** Texto si el servidor no envía "message". */
    protected String mensajePorDefecto() {
        return "Ocurrió un error";
    }

    @Override
    public final void onResponse(Call<T> call, Response<T> response) {
        if (call.isCanceled() || actividadCerrada()) return;
        onFinish();

        if (response.isSuccessful()) {
            onSuccess(response.body());
        } else if (response.code() == 401) {
            SessionManager.expirar(context);
        } else {
            onError(ApiMessage.deError(response, mensajePorDefecto()), response.code());
        }
    }

    @Override
    public final void onFailure(Call<T> call, Throwable t) {
        if (call.isCanceled() || actividadCerrada()) return;
        onFinish();
        onError("No se pudo conectar con el servidor. Revisa tu conexión", 0);
    }

    private boolean actividadCerrada() {
        return context instanceof Activity
                && (((Activity) context).isFinishing() || ((Activity) context).isDestroyed());
    }
}