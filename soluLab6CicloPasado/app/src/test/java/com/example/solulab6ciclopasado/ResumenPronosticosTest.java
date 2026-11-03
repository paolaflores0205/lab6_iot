package com.example.solulab6ciclopasado;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import static org.junit.Assert.assertEquals;

public class ResumenPronosticosTest {
    private Pronostico partido(String a, String b, String estado) {
        Pronostico p = new Pronostico();
        p.setSeleccionA(a);
        p.setSeleccionB(b);
        p.setEstado(estado);
        return p;
    }

    @Test public void sinDatosProduceResumenVacio() {
        ResumenPronosticos r = ResumenPronosticos.contar(new ArrayList<>(), "Todas las selecciones");
        assertEquals(0, r.total);
        assertEquals(0, r.pendientes + r.acertados + r.fallados);
    }

    @Test public void clasificaLosTresEstadosConElMismoConjuntoDeDatos() {
        ResumenPronosticos r = ResumenPronosticos.contar(Arrays.asList(
                partido("Perú", "Brasil", "Pendiente"),
                partido("Perú", "Chile", "Acertado"),
                partido("México", "Brasil", "Fallado"),
                partido("Chile", "Brasil", "Pendiente")), "Todas las selecciones");
        assertEquals(4, r.total);
        assertEquals(2, r.pendientes);
        assertEquals(1, r.acertados);
        assertEquals(1, r.fallados);
    }

    @Test public void filtraEquipoEnCualquierLadoIgnorandoMayusculas() {
        ResumenPronosticos r = ResumenPronosticos.contar(Arrays.asList(
                partido("Perú", "Brasil", "Pendiente"),
                partido("Chile", "PERÚ", "Acertado"),
                partido("México", "Brasil", "Fallado")), "perú");
        assertEquals(2, r.total);
        assertEquals(1, r.pendientes);
        assertEquals(1, r.acertados);
        assertEquals(0, r.fallados);
    }

    @Test public void cambiarFiltroNoAcumulaConteosAnteriores() {
        ArrayList<Pronostico> datos = new ArrayList<>(Arrays.asList(
                partido("Perú", "Brasil", "Pendiente"),
                partido("Chile", "México", "Acertado")));
        assertEquals(2, ResumenPronosticos.contar(datos, "Todas las selecciones").total);
        assertEquals(1, ResumenPronosticos.contar(datos, "Perú").total);
        datos.remove(0);
        assertEquals(0, ResumenPronosticos.contar(datos, "Perú").total);
        assertEquals(1, ResumenPronosticos.contar(datos, "Todas las selecciones").total);
    }
}
