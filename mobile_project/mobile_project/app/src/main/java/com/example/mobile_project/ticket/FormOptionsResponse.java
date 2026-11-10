package com.example.mobile_project.ticket;

import java.util.List;

public class FormOptionsResponse {
    private List<ServicioDTO> servicios;
    private List<PrioridadDTO> prioridades;

    public List<ServicioDTO> getServicios() { return servicios; }
    public List<PrioridadDTO> getPrioridades() { return prioridades; }

    public static class ServicioDTO {
        private int id;
        private String nombre;

        public int getId() { return id; }
        public String getNombre() { return nombre; }

        @Override
        public String toString() { return nombre; } // Para mostrar en el Spinner
    }

    public static class PrioridadDTO {
        private int id;
        private String nombre;

        public int getId() { return id; }
        public String getNombre() { return nombre; }

        @Override
        public String toString() { return nombre; } // Para mostrar en el Spinner
    }
}