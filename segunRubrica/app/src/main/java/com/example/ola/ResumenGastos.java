package com.example.ola;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ResumenGastos {
    public int cantidad, pendientes, pagados;
    public long totalCentimos;
    public final long[] porCategoria=new long[4];

    public static ResumenGastos calcular(List<Gasto> gastos,String mes) {
        ResumenGastos resumen=new ResumenGastos();
        SimpleDateFormat formato=new SimpleDateFormat("yyyy-MM",Locale.ROOT);
        for (Gasto g:gastos) {
            if (!"Todos los meses".equals(mes)
                    && (g.getFecha()==null || !mes.equals(formato.format(g.getFecha())))) continue;
            resumen.cantidad++;
            resumen.totalCentimos+=g.getMontoCentimos();
            for (int i=0;i<Pantallas.CATEGORIAS.length;i++) {
                if (Pantallas.CATEGORIAS[i].equals(g.getCategoria())) resumen.porCategoria[i]+=g.getMontoCentimos();
            }
            if ("Pagado".equals(g.getEstado())) resumen.pagados++;
            else if ("Pendiente".equals(g.getEstado())) resumen.pendientes++;
        }
        return resumen;
    }
}
