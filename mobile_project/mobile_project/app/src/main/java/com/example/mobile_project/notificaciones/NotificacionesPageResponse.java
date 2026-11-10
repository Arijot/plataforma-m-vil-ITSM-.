package com.example.mobile_project.notificaciones;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class NotificacionesPageResponse {
    private List<Item> data;
    @SerializedName("current_page") private int currentPage;
    @SerializedName("last_page") private int lastPage;
    private int total;
    @SerializedName("no_leidas") private int noLeidas;

    public List<Item> getData() { return data; }
    public int getCurrentPage() { return currentPage; }
    public int getLastPage() { return lastPage; }
    public int getTotal() { return total; }
    public int getNoLeidas() { return noLeidas; }

    public static class Item {
        private long id;
        @SerializedName("ticket_id") private Long ticketId;
        @SerializedName("ticket_codigo") private String ticketCodigo;
        private String tipo, titulo, cuerpo;
        private int leida;                       // 0 | 1
        @SerializedName("creado_en") private String creadoEn;

        public long getId() { return id; }
        public Long getTicketId() { return ticketId; }
        public String getTicketCodigo() { return ticketCodigo; }
        public String getTipo() { return tipo; }
        public String getTitulo() { return titulo; }
        public String getCuerpo() { return cuerpo; }
        public boolean isLeida() { return leida == 1; }
        public void setLeida(boolean v) { leida = v ? 1 : 0; }
        public String getCreadoEn() { return creadoEn; }
    }
}
