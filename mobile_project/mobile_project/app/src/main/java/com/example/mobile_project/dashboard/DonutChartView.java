package com.example.mobile_project.dashboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/** Dona simple sin librerías externas. setDatos(valores, colores) y listo. */
public class DonutChartView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintCentro = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final List<Float> valores = new ArrayList<>();
    private final List<Integer> colores = new ArrayList<>();
    private String textoCentro = "";

    public DonutChartView(Context c) { super(c); init(); }
    public DonutChartView(Context c, AttributeSet a) { super(c, a); init(); }
    public DonutChartView(Context c, AttributeSet a, int s) { super(c, a, s); init(); }

    private void init() {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.BUTT);
        paintCentro.setColor(0xFF333333);
        paintCentro.setTextAlign(Paint.Align.CENTER);
        paintCentro.setFakeBoldText(true);
    }

    public void setDatos(List<Float> v, List<Integer> c, String centro) {
        valores.clear(); colores.clear();
        valores.addAll(v); colores.addAll(c);
        textoCentro = centro != null ? centro : "";
        invalidate();
    }

    @Override
    protected void onMeasure(int w, int h) {
        int lado = MeasureSpec.getSize(w);
        setMeasuredDimension(lado, lado);   // cuadrado
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float d = getResources().getDisplayMetrics().density;
        float grosor = 26 * d;
        paint.setStrokeWidth(grosor);
        float margen = grosor / 2f + 2 * d;
        oval.set(margen, margen, getWidth() - margen, getHeight() - margen);

        float total = 0;
        for (float v : valores) total += v;

        if (total <= 0) {                       // sin datos: anillo gris
            paint.setColor(Color.parseColor("#E0E0E0"));
            canvas.drawArc(oval, 0, 360, false, paint);
        } else {
            float inicio = -90f;
            for (int i = 0; i < valores.size(); i++) {
                float barrido = 360f * valores.get(i) / total;
                paint.setColor(colores.get(i));
                canvas.drawArc(oval, inicio, Math.max(barrido - 1.5f, 0.5f), false, paint);   // 1.5° de separación
                inicio += barrido;
            }
        }

        paintCentro.setTextSize(16 * d);
        canvas.drawText(textoCentro, getWidth() / 2f, getHeight() / 2f + 6 * d, paintCentro);
    }
}