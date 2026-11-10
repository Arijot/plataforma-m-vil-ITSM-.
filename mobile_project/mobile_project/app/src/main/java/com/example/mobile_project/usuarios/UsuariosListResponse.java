package com.example.mobile_project.usuarios;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class UsuariosListResponse {
    @SerializedName("data")     public List<UsuarioItem> data;
    @SerializedName("total")    public int total;
    @SerializedName("page")     public int page;
    @SerializedName("per_page") public int perPage;
}
