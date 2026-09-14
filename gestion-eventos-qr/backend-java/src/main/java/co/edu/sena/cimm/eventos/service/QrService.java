package co.edu.sena.cimm.eventos.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
import java.util.EnumMap;
import java.util.Map;

/** Genera codigos QR en PNG a partir de un texto (ZXing). */
public class QrService {

    public byte[] generarQrPng(String contenido, int tamanoPx) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix matrix = new MultiFormatWriter()
                    .encode(contenido, BarcodeFormat.QR_CODE, tamanoPx, tamanoPx, hints);

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", salida);
            return salida.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo generar el codigo QR", e);
        }
    }
}
