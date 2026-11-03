package com.example.ola;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import java.util.Locale;

/** Presentación aislada. La lógica de negocio no depende de Canvas. */
public class GraficoView extends View {
    private final Paint pincel=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF circulo=new RectF();
    private final int[] colores={0xFF1976D2,0xFF2E7D32,0xFFB06000,0xFF7B1FA2};
    private String[] etiquetas={};
    private double[] valores={};
    private boolean circular;
    public GraficoView(Context context,AttributeSet attrs) { super(context,attrs); setBackgroundColor(Color.WHITE); }
    public void setDatos(String[] nombres,double[] datos,boolean tipoCircular) {
        etiquetas=nombres.clone(); valores=datos.clone(); circular=tipoCircular;
        StringBuilder descripcion=new StringBuilder(circular ? "Circular. " : "Barras en soles. ");
        for (int i=0;i<valores.length;i++) descripcion.append(etiquetas[i]).append(": ").append(valores[i]).append(". ");
        setContentDescription(descripcion); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        invalidate();
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        double total=0,maximo=0;
        for (double valor:valores) { total+=valor; maximo=Math.max(maximo,valor); }
        float ancho=getWidth(),alto=getHeight();
        pincel.setTextAlign(Paint.Align.CENTER); pincel.setTextSize(dp(11)); pincel.setColor(Color.DKGRAY);
        if (total==0) { canvas.drawText("Sin gastos para este filtro",ancho/2,alto/2,pincel); return; }
        float base=alto-dp(52);
        if (circular) {
            float lado=Math.min(ancho-dp(40),alto-dp(82));
            circulo.set((ancho-lado)/2,dp(12),(ancho+lado)/2,dp(12)+lado);
            float inicio=-90;
            for (int i=0;i<valores.length;i++) {
                float angulo=(float)(360*valores[i]/total);
                pincel.setColor(colores[i%colores.length]); canvas.drawArc(circulo,inicio,angulo,true,pincel);
                if (valores[i]>0) {
                    double medio=Math.toRadians(inicio+angulo/2);
                    pincel.setColor(Color.WHITE);
                    canvas.drawText(Math.round(100*valores[i]/total)+"%",
                            circulo.centerX()+(float)Math.cos(medio)*lado*0.30f,
                            circulo.centerY()+(float)Math.sin(medio)*lado*0.30f+dp(4),pincel);
                }
                inicio+=angulo;
            }
        } else {
            for (int i=0;i<valores.length;i++) {
                float centro=ancho*(i+0.5f)/valores.length;
                float altura=(float)((base-dp(26))*valores[i]/maximo);
                pincel.setColor(colores[i%colores.length]);
                canvas.drawRect(centro-ancho/14,base-altura,centro+ancho/14,base,pincel);
                pincel.setColor(Color.DKGRAY);
                canvas.drawText(String.format(Locale.ROOT,"%.2f",valores[i]),centro,base-altura-dp(6),pincel);
            }
        }
        for (int i=0;i<valores.length;i++) {
            pincel.setColor(colores[i%colores.length]);
            float centro=ancho*(i+0.5f)/valores.length;
            canvas.drawText(etiquetas[i],centro,alto-dp(28),pincel);
            canvas.drawText(circular ? String.valueOf((int)valores[i]) : "S/ "+String.format(Locale.ROOT,"%.2f",valores[i]),
                    centro,alto-dp(10),pincel);
        }
    }
    private float dp(float v) { return v*getResources().getDisplayMetrics().density; }
}
