package com.example.mobile_project.usuarios;

import com.google.gson.annotations.SerializedName;

public class UsuarioDetalleResponse {
    @SerializedName("message") public String message;
    @SerializedName("user")    public UsuarioItem user;
}