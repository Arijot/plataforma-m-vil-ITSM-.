package com.example.mobile_project.usuarios;

import com.example.mobile_project.R;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/** Usuario tal como lo devuelve GET /usuarios y GET /usuarios/{id}. */
public class UsuarioItem implements Serializable {

    public static final String ROL_TECNICO = "TECNICO_TI";

    @SerializedName("id")              public long id;
    @SerializedName("rol_id")          public int rolId;
    @SerializedName("rol_codigo")      public String rolCodigo;
    @SerializedName("rol_nombre")      public String rolNombre;
    @SerializedName("area_id")         public Integer areaId;
    @SerializedName("area_nombre")     public String areaNombre;
    @SerializedName("nombres")         public String nombres;
    @SerializedName("apellidos")       public String apellidos;
    @SerializedName("nombre_completo") public String nombreCompleto;
    @SerializedName("email")           public String email;
    @SerializedName("cargo")           public String cargo;
    @SerializedName("telefono")        public String telefono;
    @SerializedName("nivel_soporte")   public Integer nivelSoporte;
    @SerializedName("activo")          public boolean activo;
    @SerializedName("genero")          public Integer genero;

    /** Foto según género: 1 = hombre_foto, 2 = mujer_foto (igual que tus perfiles). */
    public int getFotoRes() {
        if (genero != null) {
            if (genero == 1) return R.drawable.hombre_foto;
            if (genero == 2) return R.drawable.mujer_foto;
        }
        // Sin género: cambia por tu propio placeholder si tienes uno
        return android.R.drawable.sym_def_app_icon;
    }

    public String getNombreMostrar() {
        if (nombreCompleto != null && !nombreCompleto.trim().isEmpty()) return nombreCompleto;
        return (nombres == null ? "" : nombres) + " " + (apellidos == null ? "" : apellidos);
    }
}