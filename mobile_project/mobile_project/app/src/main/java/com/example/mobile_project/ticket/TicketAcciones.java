package com.example.mobile_project.ticket;

import com.google.gson.annotations.SerializedName;

/** Objeto "acciones" que devuelve TicketController::show (se calcula en el servidor según rol y estado) */
public class TicketAcciones {
    @SerializedName("puede_encuestar") private boolean puedeEncuestar;
    @SerializedName("puede_reabrir") private boolean puedeReabrir;
    @SerializedName("reabrir_hasta") private String reabrirHasta;                 // fecha límite para reabrir
    @SerializedName("puede_editar_gestion") private boolean puedeEditarGestion;   // solo admin y ticket no final
    private String complejidad;                                                    // BAJA | MEDIA | ALTA (técnico/admin)
    @SerializedName("plazo_dias") private Integer plazoDias;
    @SerializedName("fecha_limite_tecnico") private String fechaLimiteTecnico;
    @SerializedName("minutos_restantes_tecnico") private Long minutosRestantesTecnico;   // negativo = vencido

    public boolean isPuedeEncuestar() { return puedeEncuestar; }
    public boolean isPuedeReabrir() { return puedeReabrir; }
    public String getReabrirHasta() { return reabrirHasta; }
    public boolean isPuedeEditarGestion() { return puedeEditarGestion; }
    public String getComplejidad() { return complejidad; }
    public Integer getPlazoDias() { return plazoDias; }
    public String getFechaLimiteTecnico() { return fechaLimiteTecnico; }
    public Long getMinutosRestantesTecnico() { return minutosRestantesTecnico; }
}
