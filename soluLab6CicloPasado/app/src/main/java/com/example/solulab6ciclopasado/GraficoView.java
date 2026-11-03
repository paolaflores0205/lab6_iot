package com.example.solulab6ciclopasado;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/** Dibujo aislado: las estadísticas solo necesitan contar y llamar a setDatos(). */
public class GraficoView extends View {
    private final Paint pincel = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int[] colores = {Color.rgb(191, 122, 0), Color.rgb(25, 128, 72), Color.rgb(190, 52, 52)};
    private final String[] etiquetas = {"Pendientes", "Acertados", "Fallados"};
    private int[] valores = {0, 0, 0};
    private boolean circular;

    public GraficoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBackgroundColor(Color.WHITE);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    public void setCircular(boolean circular) {
        this.circular = circular;
        invalidate();
    }

    public void setDatos(int pendientes, int acertados, int fallados) {
        valores = new int[]{pendientes, acertados, fallados};
        setContentDescription((circular ? "Gráfico circular. " : "Gráfico de barras. ")
                + "Pendientes: " + pendientes + ", acertados: " + acertados + ", fallados: " + fallados);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float ancho = getWidth();
        float alto = getHeight();
        int total = valores[0] + valores[1] + valores[2];
        pincel.setTextSize(dp(12));
        pincel.setTextAlign(Paint.Align.CENTER);
        if (total == 0) {
            pincel.setColor(Color.DKGRAY);
            canvas.drawText("Sin pronósticos para este filtro", ancho / 2, alto / 2, pincel);
            return;
        }
        float pie = alto - dp(44);
        if (circular) {
            float lado = Math.min(ancho - dp(40), alto - dp(76));
            RectF circulo = new RectF((ancho - lado) / 2, dp(10), (ancho + lado) / 2, dp(10) + lado);
            float inicio = -90;
            for (int i = 0; i < 3; i++) {
                float angulo = 360f * valores[i] / total;
                pincel.setColor(colores[i]);
                canvas.drawArc(circulo, inicio, angulo, true, pincel);
                if (valores[i] > 0) {
                    double medio = Math.toRadians(inicio + angulo / 2);
                    pincel.setColor(Color.WHITE);
                    canvas.drawText(Math.round(100f * valores[i] / total) + "%",
                            circulo.centerX() + (float) Math.cos(medio) * lado * 0.32f,
                            circulo.centerY() + (float) Math.sin(medio) * lado * 0.32f + dp(4), pincel);
                }
                inicio += angulo;
            }
        } else {
            int maximo = Math.max(valores[0], Math.max(valores[1], valores[2]));
            for (int i = 0; i < 3; i++) {
                float centro = ancho * (i + 0.5f) / 3;
                float altura = (pie - dp(30)) * valores[i] / maximo;
                pincel.setColor(colores[i]);
                canvas.drawRect(centro - ancho / 12, pie - altura, centro + ancho / 12, pie, pincel);
                pincel.setColor(Color.DKGRAY);
                canvas.drawText(String.valueOf(valores[i]), centro, pie - altura - dp(5), pincel);
            }
        }
        for (int i = 0; i < 3; i++) {
            pincel.setColor(colores[i]);
            canvas.drawText(etiquetas[i], ancho * (i + 0.5f) / 3, alto - dp(24), pincel);
            canvas.drawText(String.valueOf(valores[i]), ancho * (i + 0.5f) / 3, alto - dp(7), pincel);
        }
    }

    private float dp(float valor) {
        return valor * getResources().getDisplayMetrics().density;
    }
}
