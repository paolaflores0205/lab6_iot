package com.example.ola;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 12.50 soles se guarda como 1250 céntimos: no usamos double para las sumas. */
public class Dinero {
    public static long leerCentimos(String texto) {
        try {
            long valor = new BigDecimal(texto.trim().replace(',', '.'))
                    .setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact();
            if (valor <= 0 || valor > 100_000_000_000L) throw new IllegalArgumentException();
            return valor;
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("Ingresa un monto positivo con hasta dos decimales");
        }
    }
    public static String texto(long centimos) {
        return BigDecimal.valueOf(centimos, 2).toPlainString();
    }
    public static String soles(long centimos) { return "S/ " + texto(centimos); }
}
