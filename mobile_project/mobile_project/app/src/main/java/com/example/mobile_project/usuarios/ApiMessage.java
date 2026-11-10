package com.example.mobile_project.usuarios;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import retrofit2.Response;

/** Respuesta simple {message, error_code}; también sirve para leer errores. */
public class ApiMessage {
    @SerializedName("message")    public String message;
    @SerializedName("error_code") public String errorCode;

    /** Extrae el "message" del cuerpo de error de una respuesta HTTP no exitosa. */
    public static String deError(Response<?> response, String porDefecto) {
        try {
            if (response.errorBody() != null) {
                ApiMessage m = new Gson().fromJson(response.errorBody().string(), ApiMessage.class);
                if (m != null && m.message != null && !m.message.isEmpty()) return m.message;
            }
        } catch (Exception ignored) { }
        return porDefecto + " (" + response.code() + ")";
    }
}