package uy.edu.um.manoamiga.servicio;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import uy.edu.um.manoamiga.servicio.InscripcionService.DatosCertificado;

/**
 * Genera el PDF del certificado de participación a pedido (no se guarda en el servidor).
 * No menciona la condición judicial: el certificado es igual para todos los voluntarios.
 */
@Service
public class CertificadoService {

    private static final ZoneId MONTEVIDEO = ZoneId.of("America/Montevideo");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-UY"));

    public byte[] generarPdf(DatosCertificado d) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 60, 60, 60, 60);
        PdfWriter.getInstance(doc, salida);
        doc.addTitle("Certificado de participación");
        doc.addAuthor("Mano Amiga");
        doc.open();

        String fecha = d.fecha() != null ? FECHA.format(d.fecha().atZone(MONTEVIDEO)) : "";

        doc.add(centrado("MANO AMIGA", 12, new Color(0x2E, 0x7D, 0x32), 0));
        doc.add(centrado("Certificado de participación", 26, Color.BLACK, 40));
        doc.add(centrado("Se certifica que", 14, Color.BLACK, 40));
        doc.add(centrado(d.voluntario(), 20, Color.BLACK, 10));
        doc.add(centrado("participó en la actividad \"" + d.actividad() + "\"", 14, Color.BLACK, 10));
        doc.add(centrado("organizada por " + d.organizacion() + ", el " + fecha + ",", 14, Color.BLACK, 0));
        doc.add(centrado("completando " + formatearHoras(d.horas()) + " horas de voluntariado.", 14, Color.BLACK, 0));
        if (!d.direccion().isBlank()) doc.add(centrado(d.direccion(), 11, new Color(0x55, 0x55, 0x55), 10));
        doc.add(centrado("Código de verificación: " + d.codigo(), 9, new Color(0x77, 0x77, 0x77), 70));
        doc.add(centrado("Emitido el " + LocalDate.now(MONTEVIDEO), 9, new Color(0x77, 0x77, 0x77), 0));

        doc.close();
        return salida.toByteArray();
    }

    private static Paragraph centrado(String texto, float tamano, Color color, float espacioAntes) {
        Paragraph p = new Paragraph(texto, new Font(Font.HELVETICA, tamano, Font.NORMAL, color));
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingBefore(espacioAntes);
        return p;
    }

    /** 3.0 -> "3", 2.5 -> "2.5" (como en la versión Node). */
    private static String formatearHoras(double horas) {
        return horas == Math.rint(horas) ? String.valueOf((long) horas) : String.valueOf(horas);
    }
}
