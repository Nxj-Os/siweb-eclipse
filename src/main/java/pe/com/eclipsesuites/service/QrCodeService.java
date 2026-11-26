package pe.com.eclipsesuites.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@Service
public class QrCodeService {

    public byte[] generarPng(String contenido) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(
                    contenido, BarcodeFormat.QR_CODE, 320, 320,
                    Map.of(EncodeHintType.MARGIN, 1));
            BufferedImage image = new BufferedImage(
                    matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < matrix.getHeight(); y++) {
                for (int x = 0; x < matrix.getWidth(); x++) {
                    image.setRGB(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (!ImageIO.write(image, "png", output)) {
                throw new IllegalStateException("No se pudo codificar el código QR como PNG.");
            }
            return output.toByteArray();
        } catch (WriterException | IOException exception) {
            throw new IllegalStateException("No se pudo generar el código QR de la reserva.", exception);
        }
    }
}
