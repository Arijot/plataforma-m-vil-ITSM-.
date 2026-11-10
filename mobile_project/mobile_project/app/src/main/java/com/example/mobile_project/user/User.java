package com.example.mobile_project.user;

import com.google.gson.annotations.SerializedName;

public class User {
    private long id;

    @SerializedName("rol_id")
    private int rolId;

    @SerializedName("area_id")
    private Integer areaId;

    private String nombres;
    private String apellidos;
    private String email;
    private String cargo;
    private String telefono;

    @SerializedName("nivel_soporte")
    private Integer nivelSoporte;

    private boolean activo;

    @SerializedName("intentos_fallidos")
    private int intentosFallidos;

    @SerializedName("bloqueado_hasta")
    private String bloqueadoHasta;

    @SerializedName("ultimo_login")
    private String ultimoLogin;

    @SerializedName("creado_en")
    private String creadoEn;

    @SerializedName("actualizado_en")
    private String actualizadoEn;

    private Integer genero;

    // Getters y Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public int getRolId() { return rolId; }
    public void setRolId(int rolId) { this.rolId = rolId; }

    public Integer getAreaId() { return areaId; }
    public void setAreaId(Integer areaId) { this.areaId = areaId; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public Integer getNivelSoporte() { return nivelSoporte; }
    public void setNivelSoporte(Integer nivelSoporte) { this.nivelSoporte = nivelSoporte; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public int getIntentosFallidos() { return intentosFallidos; }
    public void setIntentosFallidos(int intentosFallidos) { this.intentosFallidos = intentosFallidos; }

    public String getBloqueadoHasta() { return bloqueadoHasta; }
    public void setBloqueadoHasta(String bloqueadoHasta) { this.bloqueadoHasta = bloqueadoHasta; }

    public String getUltimoLogin() { return ultimoLogin; }
    public void setUltimoLogin(String ultimoLogin) { this.ultimoLogin = ultimoLogin; }

    public String getCreadoEn() { return creadoEn; }
    public void setCreadoEn(String creadoEn) { this.creadoEn = creadoEn; }

    public String getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(String actualizadoEn) { this.actualizadoEn = actualizadoEn; }

    public Integer getGenero() { return genero; }
    public void setGenero(Integer genero) { this.genero = genero; }
}
