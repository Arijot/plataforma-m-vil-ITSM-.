package com.example.mobile_project.estados_ticket;

import com.example.mobile_project.shared.clients.ApiClient;
import com.example.mobile_project.ticket.EstadoTicket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Catálogo de estados leído del backend (getEstadosTicket -> GET /estados-ticket).
 * Lo comparten la lista (badge y chips) y el detalle (estado pintado y dropdown).
 */
public final class EstadosCache {

    private static volatile List<EstadoTicket> estados = new ArrayList<>();

    private EstadosCache() { }

    public static List<EstadoTicket> get() { return estados; }

    public static boolean vacio() { return estados == null || estados.isEmpty(); }

    public static void set(List<EstadoTicket> lista) {
        if (lista == null || lista.isEmpty()) return;
        List<EstadoTicket> copia = new ArrayList<>(lista);
        Collections.sort(copia, (a, b) -> Integer.compare(a.getOrden(), b.getOrden()));
        estados = copia;
    }

    /** Nombre del estado según la BD, o null si el catálogo aún no cargó / no existe el código */
    public static String nombre(String codigo) {
        if (codigo == null) return null;
        for (EstadoTicket e : estados) {
            if (codigo.equals(e.getCodigo())) return e.getNombre();
        }
        return null;
    }

    /** Pide el catálogo al backend; al terminar con éxito lo guarda y ejecuta alTerminar (hilo principal) */
    public static void cargar(String authHeader, Runnable alTerminar) {
        ApiClient.getService().getEstadosTicket(authHeader).enqueue(new Callback<List<EstadoTicket>>() {
            @Override
            public void onResponse(Call<List<EstadoTicket>> call, Response<List<EstadoTicket>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    set(r.body());
                    if (alTerminar != null) alTerminar.run();
                }
            }

            @Override
            public void onFailure(Call<List<EstadoTicket>> call, Throwable t) { }
        });
    }
}
