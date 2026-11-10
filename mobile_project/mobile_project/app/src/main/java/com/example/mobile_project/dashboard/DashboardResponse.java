package com.example.mobile_project.dashboard;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Respuesta de GET api/dashboard/sla */
public class DashboardResponse {

    @SerializedName("periodo") private String periodo;
    @SerializedName("meta") private int meta;
    @SerializedName("kpis") private Kpis kpis;
    @SerializedName("por_prioridad") private List<Prioridad> porPrioridad;
    @SerializedName("por_servicio") private List<Servicio> porServicio;
    @SerializedName("filtros") private Filtros filtros;

    public String getPeriodo() { return periodo; }
    public int getMeta() { return meta; }
    public Kpis getKpis() { return kpis; }
    public List<Prioridad> getPorPrioridad() { return porPrioridad; }
    public List<Servicio> getPorServicio() { return porServicio; }
    public Filtros getFiltros() { return filtros; }

    public static class Kpis {
        @SerializedName("cumplimiento") private Double cumplimiento;      // null = sin datos
        @SerializedName("variacion") private Double variacion;            // puntos vs periodo anterior
        @SerializedName("evaluados") private int evaluados;
        @SerializedName("dentro_sla") private int dentroSla;
        @SerializedName("fuera_sla") private int fueraSla;
        @SerializedName("resueltos_tarde") private int resueltosTarde;
        @SerializedName("abiertos_vencidos") private int abiertosVencidos;
        @SerializedName("mttr_min") private Integer mttrMin;
        @SerializedName("total_tickets") private int totalTickets;

        public Double getCumplimiento() { return cumplimiento; }
        public Double getVariacion() { return variacion; }
        public int getEvaluados() { return evaluados; }
        public int getDentroSla() { return dentroSla; }
        public int getFueraSla() { return fueraSla; }
        public int getResueltosTarde() { return resueltosTarde; }
        public int getAbiertosVencidos() { return abiertosVencidos; }
        public Integer getMttrMin() { return mttrMin; }
        public int getTotalTickets() { return totalTickets; }
    }

    public static class Prioridad {
        @SerializedName("id") private int id;
        @SerializedName("codigo") private String codigo;
        @SerializedName("nombre") private String nombre;
        @SerializedName("color_hex") private String colorHex;
        @SerializedName("total") private int total;
        @SerializedName("evaluados") private int evaluados;
        @SerializedName("dentro_sla") private int dentroSla;
        @SerializedName("cumplimiento") private Double cumplimiento;

        public int getId() { return id; }
        public String getCodigo() { return codigo; }
        public String getNombre() { return nombre; }
        public String getColorHex() { return colorHex; }
        public int getTotal() { return total; }
        public int getEvaluados() { return evaluados; }
        public int getDentroSla() { return dentroSla; }
        public Double getCumplimiento() { return cumplimiento; }
    }

    public static class Servicio {
        @SerializedName("id") private int id;
        @SerializedName("nombre") private String nombre;
        @SerializedName("total") private int total;
        @SerializedName("evaluados") private int evaluados;
        @SerializedName("dentro_sla") private int dentroSla;
        @SerializedName("cumplimiento") private Double cumplimiento;

        public int getId() { return id; }
        public String getNombre() { return nombre; }
        public int getTotal() { return total; }
        public int getEvaluados() { return evaluados; }
        public int getDentroSla() { return dentroSla; }
        public Double getCumplimiento() { return cumplimiento; }
    }

    public static class Filtros {
        @SerializedName("servicios") private List<Opcion> servicios;
        @SerializedName("prioridades") private List<Opcion> prioridades;

        public List<Opcion> getServicios() { return servicios; }
        public List<Opcion> getPrioridades() { return prioridades; }
    }

    public static class Opcion {
        @SerializedName("id") private int id;
        @SerializedName("nombre") private String nombre;

        public int getId() { return id; }
        public String getNombre() { return nombre; }
    }
}
