package com.example.ola;

import org.junit.Test;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import static org.junit.Assert.assertEquals;

public class GastosTest {
    private Gasto gasto(String categoria,String estado,long centimos,int mes) {
        Gasto g=new Gasto();
        g.setCategoria(categoria); g.setEstado(estado); g.setMontoCentimos(centimos);
        Calendar c=Calendar.getInstance(); c.clear(); c.set(2026,mes-1,15);
        g.setFecha(c.getTime()); return g;
    }
    @Test public void decimalesSeGuardanExactamente() {
        assertEquals(1250,Dinero.leerCentimos("12.50"));
        assertEquals(1250,Dinero.leerCentimos("12,50"));
        assertEquals(30,Dinero.leerCentimos("0.10")+Dinero.leerCentimos("0.20"));
        assertEquals("12.50",Dinero.texto(1250));
    }
    @Test(expected=IllegalArgumentException.class) public void rechazaTresDecimalesSignificativos() {
        Dinero.leerCentimos("2.501");
    }
    @Test(expected=IllegalArgumentException.class) public void rechazaCero() { Dinero.leerCentimos("0"); }
    @Test(expected=IllegalArgumentException.class) public void rechazaNegativos() { Dinero.leerCentimos("-1"); }
    @Test(expected=IllegalArgumentException.class) public void rechazaMontoFueraDeLimite() {
        Dinero.leerCentimos("1000000000.01");
    }
    @Test public void resumenVacioNoTieneDatosInventados() {
        ResumenGastos r=ResumenGastos.calcular(Collections.emptyList(),"Todos los meses");
        assertEquals(0,r.cantidad); assertEquals(0,r.totalCentimos);
    }
    @Test public void ambosGraficosUsanElMismoFiltroMensual() {
        ResumenGastos r=ResumenGastos.calcular(Arrays.asList(
                gasto("Alimentación","Pagado",1250,11),
                gasto("Transporte","Pendiente",350,11),
                gasto("Estudios","Pagado",2000,10)),"2026-11");
        assertEquals(2,r.cantidad); assertEquals(1600,r.totalCentimos);
        assertEquals(1,r.pagados); assertEquals(1,r.pendientes);
        assertEquals(1250,r.porCategoria[0]); assertEquals(350,r.porCategoria[1]);
        assertEquals(0,r.porCategoria[2]);
    }
    @Test public void cambiarFiltroNoAcumulaResultados() {
        java.util.List<Gasto> datos=Arrays.asList(gasto("Otros","Pagado",100,11),gasto("Otros","Pagado",200,10));
        assertEquals(300,ResumenGastos.calcular(datos,"Todos los meses").totalCentimos);
        assertEquals(100,ResumenGastos.calcular(datos,"2026-11").totalCentimos);
        assertEquals(0,ResumenGastos.calcular(datos,"2026-09").totalCentimos);
    }
}
