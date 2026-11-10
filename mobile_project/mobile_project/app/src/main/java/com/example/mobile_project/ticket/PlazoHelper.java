package com.example.mobile_project.ticket;

import android.view.View;
import android.widget.TextView;

public final class PlazoHelper {

    private PlazoHelper() { }

    private static final int ROJO = 0xFFC62828, NARANJA = 0xFFEF6C00, VERDE = 0xFF2E7D32;

    /** 3000 -> "2d 2h" · 330 -> "5h 30m" · 45 -> "45m" */
    public static String duracion(long minutos) {
        long m = Math.abs(minutos);
        if (m >= 1440) return (m / 1440) + "d " + ((m % 1440) / 60) + "h";
        if (m >= 60) return (m / 60) + "h " + (m % 60) + "m";
        return m + "m";
    }

    /** "⏳ Quedan 2d 4h"  /  "⚠ Vencido hace 3h 10m"  /  "" si no hay plazo */
    public static String texto(Long minutos) {
        if (minutos == null) return "";
        return minutos < 0
                ? "⚠ Vencido hace " + duracion(minutos)
                : "⏳ Quedan " + duracion(minutos);
    }

    /** Rojo si venció, naranja si falta menos de 24 h, verde si hay margen */
    public static int color(Long minutos) {
        if (minutos == null) return VERDE;
        if (minutos < 0) return ROJO;
        return minutos < 1440 ? NARANJA : VERDE;
    }

    /** Pinta el TextView; si no hay plazo lo oculta. */
    public static void aplicar(TextView tv, Long minutos) {
        if (tv == null) return;
        if (minutos == null) {
            tv.setVisibility(View.GONE);
            return;
        }
        tv.setText(texto(minutos));
        tv.setTextColor(color(minutos));
        tv.setVisibility(View.VISIBLE);
    }
}