package com.example.mobile_project.ticket;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TicketDetailResponse {
    private TicketDetail ticket;
    private Adjuntos adjuntos;
    private List<Mensaje> mensajes;
    private List<Historial> historial;
    private Encuesta encuesta;
    @SerializedName("estados_permitidos") private List<Estado> estadosPermitidos;
    private TicketAcciones acciones;   // permisos y datos de gestión según rol/estado (reabrir, encuesta, plazo...)
    @SerializedName("mi_usuario_id") private long miUsuarioId;

    public TicketDetail getTicket() { return ticket; }
    public Adjuntos getAdjuntos() { return adjuntos; }
    public List<Mensaje> getMensajes() { return mensajes; }
    public List<Historial> getHistorial() { return historial; }
    public Encuesta getEncuesta() { return encuesta; }
    public List<Estado> getEstadosPermitidos() { return estadosPermitidos; }
    public TicketAcciones getAcciones() { return acciones; }
    public long getMiUsuarioId() { return miUsuarioId; }

    // ---------- clases anidadas ----------
    public static class Adjuntos {
        @SerializedName("tiene_adjuntos") private boolean tieneAdjuntos;
        private String mensaje;
        private List<Archivo> archivos;
        public boolean isTieneAdjuntos() { return tieneAdjuntos; }
        public String getMensaje() { return mensaje; }
        public List<Archivo> getArchivos() { return archivos; }
    }

    public static class Archivo {
        private long id;
        @SerializedName("nombre_archivo") private String nombreArchivo;
        @SerializedName("ruta_almacenamiento") private String rutaAlmacenamiento;
        @SerializedName("mime_type") private String mimeType;
        public String getNombreArchivo() { return nombreArchivo; }
        public String getRutaAlmacenamiento() { return rutaAlmacenamiento; }
        public String getMimeType() { return mimeType; }
    }

    public static class Mensaje {
        private long id;
        @SerializedName("autor_id") private long autorId;
        @SerializedName("autor_nombre") private String autorNombre;
        @SerializedName("autor_rol") private String autorRol;
        private String contenido;
        @SerializedName("es_interno") private int esInterno;
        @SerializedName("creado_en") private String creadoEn;
        public long getId() { return id; }
        public long getAutorId() { return autorId; }
        public String getAutorNombre() { return autorNombre; }
        public String getAutorRol() { return autorRol; }
        public String getContenido() { return contenido; }
        public boolean isInterno() { return esInterno == 1; }
        public String getCreadoEn() { return creadoEn; }
    }

    public static class Historial {
        @SerializedName("tipo_evento") private String tipoEvento;
        private String detalle;
        @SerializedName("creado_en") private String creadoEn;
        @SerializedName("realizado_por") private String realizadoPor;
        public String getTipoEvento() { return tipoEvento; }
        public String getDetalle() { return detalle; }
        public String getCreadoEn() { return creadoEn; }
        public String getRealizadoPor() { return realizadoPor; }
    }

    public static class Encuesta {
        private int calificacion;
        private String comentario;
        public int getCalificacion() { return calificacion; }
        public String getComentario() { return comentario; }
    }

    public static class Estado {
        private String codigo, nombre;
        public String getCodigo() { return codigo; }
        public String getNombre() { return nombre; }
    }

    public static class Tecnico {
        private long id;
        private String nombre;
        public long getId() { return id; }
        public String getNombre() { return nombre; }
    }

    /** Respuesta del polling GET tickets/{codigo}/mensajes */
    public static class MensajesResponse {
        private List<Mensaje> mensajes;
        private String estado;
        @SerializedName("tecnico_id") private Long tecnicoId;
        private String tecnico;
        public List<Mensaje> getMensajes() { return mensajes; }
        public String getEstado() { return estado; }
        public Long getTecnicoId() { return tecnicoId; }
        public String getTecnico() { return tecnico; }
    }
}
