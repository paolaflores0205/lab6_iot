package com.example.solulab6ciclopasado;

import java.util.List;

/** Conteo sencillo y sin dependencias de pantalla o Firebase. */
public class ResumenPronosticos {
    public int total;
    public int pendientes;
    public int acertados;
    public int fallados;

    public static ResumenPronosticos contar(List<Pronostico> datos, String filtro) {
        ResumenPronosticos resumen = new ResumenPronosticos();
        for (Pronostico p : datos) {
            boolean mostrar = "Todas las selecciones".equals(filtro)
                    || filtro.equalsIgnoreCase(p.getSeleccionA())
                    || filtro.equalsIgnoreCase(p.getSeleccionB());
            if (!mostrar) continue;
            resumen.total++;
            if ("Pendiente".equals(p.getEstado())) resumen.pendientes++;
            else if ("Acertado".equals(p.getEstado())) resumen.acertados++;
            else if ("Fallado".equals(p.getEstado())) resumen.fallados++;
        }
        return resumen;
    }
}
