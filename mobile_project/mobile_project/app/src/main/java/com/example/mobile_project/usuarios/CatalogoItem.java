package com.example.mobile_project.usuarios;

import com.google.gson.annotations.SerializedName;

/** Rol o área para los Spinner. */
public class CatalogoItem {
    @SerializedName("id")     public Integer id;
    @SerializedName("codigo") public String codigo;   // solo roles
    @SerializedName("nombre") public String nombre;

    public CatalogoItem() { }

    public CatalogoItem(Integer id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    @Override
    public String toString() { return nombre; }
}
