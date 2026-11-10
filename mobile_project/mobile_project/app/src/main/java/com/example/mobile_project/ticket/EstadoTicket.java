package com.example.mobile_project.ticket;

import com.google.gson.annotations.SerializedName;

/** Fila de la tabla estados_ticket (id, codigo, nombre, orden, es_final). */
public class EstadoTicket {

    @SerializedName("id")
    private int id;

    @SerializedName("codigo")
    private String codigo;      // ABIERTO, ASIGNADO, EN_PROCESO, RESUELTO, CERRADO, CANCELADO

    @SerializedName("nombre")
    private String nombre;      // Abierto, Asignado, En proceso, ...

    @SerializedName("orden")
    private int orden;

    @SerializedName("es_final")
    private int esFinal;        // 0 | 1 (int porque Gson no convierte 0/1 a boolean)

    public int getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public int getOrden() { return orden; }
    public boolean isFinal() { return esFinal == 1; }
}