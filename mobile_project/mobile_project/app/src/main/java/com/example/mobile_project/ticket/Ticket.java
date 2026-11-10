package com.example.mobile_project.ticket;

import com.google.gson.annotations.SerializedName;

public class Ticket {
    private long id;
    private String codigo, titulo, descripcion, servicio, prioridad, estado, solicitante, tecnico;
    @SerializedName("prioridad_nivel") private int prioridadNivel;
    @SerializedName("color_hex") private String colorHex;
    @SerializedName("tecnico_id") private Long tecnicoId;
    @SerializedName("fecha_creacion") private String fechaCreacion;
    @SerializedName("estado_nombre") private String estadoNombre;   // nombre en estados_ticket (ej. "En proceso")
    @SerializedName("notif_pendientes") private int notifPendientes;   // avisos sin leer sobre este ticket
    @SerializedName("estado_final") private int estadoFinal;         // es_final: 0 | 1

    public long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public String getServicio() { return servicio; }
    public String getPrioridad() { return prioridad; }
    public int getPrioridadNivel() { return prioridadNivel; }
    public String getColorHex() { return colorHex; }
    public String getEstado() { return estado; }
    public String getEstadoNombre() { return estadoNombre; }
    public int getNotifPendientes() { return notifPendientes; }
    public boolean isEstadoFinal() { return estadoFinal == 1; }
    public String getSolicitante() { return solicitante; }
    public Long getTecnicoId() { return tecnicoId; }
    public String getTecnico() { return tecnico; }
    public String getFechaCreacion() { return fechaCreacion; }

    public boolean tieneTecnico() { return tecnicoId != null; }

    public boolean estaTerminado() {
        return "RESUELTO".equals(estado) || "CERRADO".equals(estado) || "CANCELADO".equals(estado);
    }
}