package com.example.mobile_project;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_project.usuarios.UsuarioItem;

import java.util.ArrayList;
import java.util.List;

public class UsuariosAdapter extends RecyclerView.Adapter<UsuariosAdapter.VH> {

    public interface Listener {
        void onEditar(UsuarioItem u);
        void onEliminar(UsuarioItem u);
        void onCambiarEstado(UsuarioItem u, boolean activo);
    }

    private final List<UsuarioItem> items = new ArrayList<>();
    private final Listener listener;

    public UsuariosAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<UsuarioItem> nuevos) {
        items.clear();
        if (nuevos != null) items.addAll(nuevos);
        notifyDataSetChanged();
    }

    public boolean isEmpty() { return items.isEmpty(); }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_usuario, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        UsuarioItem u = items.get(position);

        h.img.setImageResource(u.getFotoRes());
        h.nombre.setText(u.getNombreMostrar());

        String rol = u.rolNombre != null ? u.rolNombre : "";
        String cargo = (u.cargo != null && !u.cargo.isEmpty()) ? " · " + u.cargo : "";
        h.rolCargo.setText(rol + cargo);
        h.email.setText(u.email);

        h.estado.setText(u.activo ? "Activo" : "Inactivo");
        h.estado.setTextColor(u.activo ? 0xFF2E7D32 : 0xFFC62828);

        // Evita disparar el listener al reciclar la vista
        h.sw.setOnCheckedChangeListener(null);
        h.sw.setChecked(u.activo);
        h.sw.setOnCheckedChangeListener((btn, checked) -> {
            if (checked != u.activo) listener.onCambiarEstado(u, checked);
        });

        h.btnEditar.setOnClickListener(v -> listener.onEditar(u));
        h.btnEliminar.setOnClickListener(v -> listener.onEliminar(u));
        h.itemView.setOnClickListener(v -> listener.onEditar(u));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView img;
        final TextView nombre, rolCargo, email, estado;
        final SwitchCompat sw;
        final ImageButton btnEditar, btnEliminar;

        VH(@NonNull View v) {
            super(v);
            img = v.findViewById(R.id.imgUsuario);
            nombre = v.findViewById(R.id.txtNombre);
            rolCargo = v.findViewById(R.id.txtRolCargo);
            email = v.findViewById(R.id.txtEmail);
            estado = v.findViewById(R.id.txtEstado);
            sw = v.findViewById(R.id.swActivo);
            btnEditar = v.findViewById(R.id.btnEditar);
            btnEliminar = v.findViewById(R.id.btnEliminar);
        }
    }
}