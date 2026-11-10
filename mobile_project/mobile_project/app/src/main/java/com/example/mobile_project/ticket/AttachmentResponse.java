package com.example.mobile_project.ticket;

import java.util.List;

public class AttachmentResponse {
    private boolean tiene_adjuntos;
    private String mensaje;
    private List<Object> archivos;

    public boolean isTieneAdjuntos() { return tiene_adjuntos; }
    public String getMensaje() { return mensaje; }
}