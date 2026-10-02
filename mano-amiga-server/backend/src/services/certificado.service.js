const PDFDocument = require('pdfkit');

/**
 * Genera el PDF del certificado de participación a pedido (no se guarda en el servidor).
 * No menciona la condición judicial: el certificado es igual para todos los voluntarios.
 */
function generarPDF(d) {
  return new Promise((resolve, reject) => {
    const doc = new PDFDocument({ size: 'A4', margin: 60, info: { Title: 'Certificado de participación', Author: 'Mano Amiga' } });
    const partes = [];
    doc.on('data', (p) => partes.push(p));
    doc.on('end', () => resolve(Buffer.concat(partes)));
    doc.on('error', reject);

    const fecha = new Date(d.fecha).toLocaleDateString('es-UY', { day: '2-digit', month: 'long', year: 'numeric', timeZone: 'America/Montevideo' });

    doc.fontSize(12).fillColor('#2E7D32').text('MANO AMIGA', { align: 'center' });
    doc.moveDown(2);
    doc.fontSize(26).fillColor('#000').text('Certificado de participación', { align: 'center' });
    doc.moveDown(2);
    doc.fontSize(14).text('Se certifica que', { align: 'center' });
    doc.moveDown(0.5);
    doc.fontSize(20).text(d.voluntario, { align: 'center' });
    doc.moveDown(0.5);
    doc.fontSize(14).text(`participó en la actividad "${d.actividad}"`, { align: 'center' });
    doc.text(`organizada por ${d.organizacion}, el ${fecha},`, { align: 'center' });
    doc.text(`completando ${d.horas} horas de voluntariado.`, { align: 'center' });
    if (d.direccion) {
      doc.moveDown(0.5);
      doc.fontSize(11).fillColor('#555').text(d.direccion, { align: 'center' });
    }
    doc.moveDown(4);
    doc.fontSize(9).fillColor('#777').text(`Código de verificación: ${d.codigo}`, { align: 'center' });
    doc.text(`Emitido el ${new Date().toISOString().slice(0, 10)}`, { align: 'center' });
    doc.end();
  });
}

module.exports = { generarPDF };
