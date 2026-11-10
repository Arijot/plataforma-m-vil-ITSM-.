package com.example.mobile_project;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_project.estados_ticket.EstadosCache;
import com.example.mobile_project.ticket.Ticket;

import java.util.List;
import java.util.Locale;

public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.TicketViewHolder> {

    private final List<Ticket> ticketList;
    private final int rolId;   // 1 usuario final | 2 técnico | 3 admin

    public TicketAdapter(List<Ticket> ticketList, int rolId) {
        this.ticketList = ticketList;
        this.rolId = rolId;
    }

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ticket_card, parent, false);
        return new TicketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        final Ticket ticket = ticketList.get(position);

        holder.txtTicketCode.setText(ticket.getCodigo());
        holder.txtTicketTitle.setText(ticket.getNotifPendientes() > 0 ? "🔔 " + ticket.getTitulo() : ticket.getTitulo());
        // Nombre del estado según el catálogo que devuelve getEstadosTicket; si aún no cargó, el que trae el ticket
        String nombreEstado = EstadosCache.nombre(ticket.getEstado());
        if (nombreEstado == null) nombreEstado = ticket.getEstadoNombre();
        holder.txtStatusBadge.setText(nombreEstado != null && !nombreEstado.isEmpty()
                ? nombreEstado : formatearEstado(ticket.getEstado()));
        holder.txtPriorityLabel.setText(ticket.getPrioridad());
        holder.imgPriorityIcon.setImageResource(R.drawable.ic_arrow_down_green);

        pintarAsignacion(holder.txtAsignacion, ticket);

        // Guarda el código en SharedPreferences y abre el detalle
        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            context.getSharedPreferences("SoporteClinicaPrefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("selected_ticket_code", ticket.getCodigo())
                    .apply();
            context.startActivity(new Intent(context, TicketDetailActivity.class));
        });
    }

    /** Línea de asignación según el rol */
    private void pintarAsignacion(TextView tv, Ticket t) {
        tv.setVisibility(View.VISIBLE);

        if (rolId == 3) {                                   // ADMIN: ver de un vistazo qué falta asignar
            if (t.tieneTecnico()) {
                tv.setText("✔ Asignado a " + t.getTecnico());
                tv.setTextColor(Color.parseColor("#2E7D32"));
            } else if (t.estaTerminado()) {
                tv.setText("Sin técnico");
                tv.setTextColor(Color.parseColor("#777777"));
            } else {
                tv.setText("⚠ Sin asignar");
                tv.setTextColor(Color.parseColor("#E65100"));
            }
        } else if (rolId == 2) {                            // TÉCNICO: quién lo solicitó
            tv.setText("Solicitante: " + t.getSolicitante());
            tv.setTextColor(Color.parseColor("#555555"));
        } else {                                            // USUARIO: quién lo atiende
            if (t.tieneTecnico()) {
                tv.setText("Técnico: " + t.getTecnico());
                tv.setTextColor(Color.parseColor("#7E57C2"));
            } else {
                tv.setText("Pendiente de asignación");
                tv.setTextColor(Color.parseColor("#777777"));
            }
        }
    }

    /** EN_PROCESO -> "En proceso" */
    private String formatearEstado(String estado) {
        if (estado == null) return "";
        String s = estado.replace('_', ' ').toLowerCase(Locale.ROOT);
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @Override
    public int getItemCount() {
        return ticketList != null ? ticketList.size() : 0;
    }

    public static class TicketViewHolder extends RecyclerView.ViewHolder {
        TextView txtTicketCode, txtStatusBadge, txtTicketTitle, txtPriorityLabel, txtAsignacion;
        ImageView imgPriorityIcon;

        public TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTicketCode = itemView.findViewById(R.id.txtTicketCode);
            txtStatusBadge = itemView.findViewById(R.id.txtStatusBadge);
            txtTicketTitle = itemView.findViewById(R.id.txtTicketTitle);
            txtPriorityLabel = itemView.findViewById(R.id.txtPriorityLabel);
            txtAsignacion = itemView.findViewById(R.id.txtAsignacion);
            imgPriorityIcon = itemView.findViewById(R.id.imgPriorityIcon);
        }
    }
}