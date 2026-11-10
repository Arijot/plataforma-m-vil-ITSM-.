package com.example.mobile_project.usuarios;

import com.google.gson.annotations.SerializedName;

/** Cuerpo para POST /usuarios y PUT /usuarios/{id}. */
public class UsuarioRequest {
    @SerializedName("nombres")       public String nombres;
    @SerializedName("apellidos")     public String apellidos;
    @SerializedName("email")         public String email;
    @SerializedName("password")      public String password;      // obligatorio al crear, opcional al editar
    @SerializedName("rol_id")        public Integer rolId;
    @SerializedName("area_id")       public Integer areaId;
    @SerializedName("cargo")         public String cargo;
    @SerializedName("telefono")      public String telefono;
    @SerializedName("nivel_soporte") public Integer nivelSoporte;
    @SerializedName("genero")        public Integer genero;
    @SerializedName("activo")        public Boolean activo;
}
