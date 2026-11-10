package com.example.mobile_project;

import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_project.ticket.TicketDetailResponse.Mensaje;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {

    private final List<Mensaje> items = new ArrayList<>();
    private long miId = -1;

    public void setMiId(long id) { this.miId = id; }

    public void setAll(List<Mensaje> lista) {
        items.clear();
        if (lista != null) items.addAll(lista);
        notifyDataSetChanged();
    }

    /** Agrega solo mensajes más nuevos que el último (evita duplicados entre polling y envío) */
    public int addNuevos(List<Mensaje> lista) {
        long ultimo = getLastId();
        int start = items.size();
        for (Mensaje m : lista) {
            if (m.getId() > ultimo) { items.add(m); ultimo = m.getId(); }
        }
        int agregados = items.size() - start;
        if (agregados > 0) notifyItemRangeInserted(start, agregados);
        return agregados;
    }

    public void clear() { items.clear(); notifyDataSetChanged(); }
    public long getLastId() { return items.isEmpty() ? 0 : items.get(items.size() - 1).getId(); }
    public boolean isEmpty() { return items.isEmpty(); }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Mensaje m = items.get(position);
        boolean mio = m.getAutorId() == miId;

        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) h.bubble.getLayoutParams();
        lp.gravity = mio ? Gravity.END : Gravity.START;
        h.bubble.setLayoutParams(lp);

        int textColor;
        if (m.isInterno()) {
            h.bubble.setBackgroundResource(R.drawable.bg_bubble_internal);
            textColor = Color.BLACK;
        } else if (mio) {
            h.bubble.setBackgroundResource(R.drawable.bg_bubble_mine);
            textColor = Color.WHITE;
        } else {
            h.bubble.setBackgroundResource(R.drawable.bg_bubble_other);
            textColor = Color.BLACK;
        }

        String autor = mio ? "Tú" : m.getAutorNombre() + " · " + etiquetaRol(m.getAutorRol());
        if (m.isInterno()) autor += " · Nota interna";

        h.txtAutor.setText(autor);
        h.txtContenido.setText(m.getContenido());
        h.txtHora.setText(formatearHora(m.getCreadoEn()));
        h.txtAutor.setTextColor(textColor);
        h.txtContenido.setTextColor(textColor);
        h.txtHora.setTextColor(textColor);
    }

    @Override
    public int getItemCount() { return items.size(); }

    private String etiquetaRol(String rol) {
        if (rol == null) return "";
        switch (rol) {
            case "TECNICO_TI": return "Técnico";
            case "ADMINISTRADOR_TI": return "Admin";
            default: return "Usuario";
        }
    }

    private String formatearHora(String fecha) {
        return (fecha != null && fecha.length() >= 16) ? fecha.substring(0, 16).replace('T', ' ') : "";
    }

    static class VH extends RecyclerView.ViewHolder {
        LinearLayout bubble;
        TextView txtAutor, txtContenido, txtHora;

        VH(@NonNull View v) {
            super(v);
            bubble = v.findViewById(R.id.layoutBubble);
            txtAutor = v.findViewById(R.id.txtMsgAutor);
            txtContenido = v.findViewById(R.id.txtMsgContenido);
            txtHora = v.findViewById(R.id.txtMsgHora);
        }
    }
}