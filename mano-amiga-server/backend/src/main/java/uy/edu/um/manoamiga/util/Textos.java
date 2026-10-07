package uy.edu.um.manoamiga.util;

/** Limpieza de textos de entrada (lo que en la versión Node hacía Joi con .trim()). */
public final class Textos {

    private Textos() {}

    public static String trim(String s) {
        return s == null ? null : s.strip();
    }

    public static String minusculas(String s) {
        return s == null ? null : s.strip().toLowerCase(java.util.Locale.ROOT);
    }

    public static String vacioSiNull(String s) {
        return s == null ? "" : s;
    }

    public static String recortar(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
