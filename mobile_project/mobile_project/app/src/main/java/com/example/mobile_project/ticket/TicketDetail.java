package com.example.mobile_project.ticket;

import com.google.gson.annotations.SerializedName;

public class TicketDetail {
    private long id;
    private String codigo, titulo, descripcion, servicio, categoria, prioridad, estado;
    @SerializedName("solicitante_id") private long solicitanteId;
    private String solicitante;
    @SerializedName("area_solicitante") private String areaSolicitante;
    private String ubicacion;
    @SerializedName("tecnico_id") private Long tecnicoId;
    private String tecnico;
    @SerializedName("solucion_aplicada") private String solucionAplicada;
    @SerializedName("estado_nombre") private String estadoNombre;   // nombre en estados_ticket
    @SerializedName("estado_final") private int estadoFinal;         // es_final: 0 | 1

    public long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public String getServicio() { return servicio; }
    public String getCategoria() { return categoria; }
    public String getPrioridad() { return prioridad; }
    public String getEstado() { return estado; }
    public String getEstadoNombre() { return estadoNombre; }
    public boolean isEstadoFinal() { return estadoFinal == 1; }
    public long getSolicitanteId() { return solicitanteId; }
    public String getSolicitante() { return solicitante; }
    public String getAreaSolicitante() { return areaSolicitante; }
    public String getUbicacion() { return ubicacion; }
    public Long getTecnicoId() { return tecnicoId; }
    public String getTecnico() { return tecnico; }
    public String getSolucionAplicada() { return solucionAplicada; }
}