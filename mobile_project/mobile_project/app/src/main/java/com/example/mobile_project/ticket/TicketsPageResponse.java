package com.example.mobile_project.ticket;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TicketsPageResponse {
    private List<Ticket> data;
    @SerializedName("current_page") private int currentPage;
    @SerializedName("last_page") private int lastPage;
    private int total;
    @SerializedName("per_page") private int perPage;
    private List<EstadoTicket> estados;   // catálogo de estados_ticket (para armar los chips)

    public List<Ticket> getData() { return data; }
    public int getCurrentPage() { return currentPage; }
    public int getLastPage() { return lastPage; }
    public int getTotal() { return total; }
    public int getPerPage() { return perPage; }
    public List<EstadoTicket> getEstados() { return estados; }
}