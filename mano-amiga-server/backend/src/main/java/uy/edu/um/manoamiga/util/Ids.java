package uy.edu.um.manoamiga.util;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.types.ObjectId;

/** Conversión de ids de texto a ObjectId (los ids siempre llegan validados como 24 caracteres hexadecimales). */
public final class Ids {

    /** Un id de MongoDB: 24 caracteres hexadecimales. Se usa en las validaciones (@Pattern). */
    public static final String REGEX = "^[a-fA-F0-9]{24}$";
    private static final Pattern PATRON = Pattern.compile(REGEX);

    private Ids() {}

    public static boolean valido(String id) {
        return id != null && PATRON.matcher(id).matches();
    }

    public static ObjectId oid(String id) {
        return id == null ? null : new ObjectId(id);
    }

    public static List<ObjectId> oids(Collection<String> ids) {
        return ids.stream().map(ObjectId::new).toList();
    }

    public static String str(ObjectId id) {
        return id == null ? null : id.toHexString();
    }
}
