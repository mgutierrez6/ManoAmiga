package uy.edu.um.manoamiga.util;

public final class Numeros {
    private Numeros() {}

    /** Redondea a 2 decimales (horas). */
    public static double redondear(double n) {
        return Math.round(n * 100) / 100.0;
    }
}
