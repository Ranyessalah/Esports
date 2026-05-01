package esprit.tn.esports.utils;

import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class QRCodeGenerator {

    public static Image generateQRCodeWithLogo(String text, int size, String logoPath) {
        try {
            // 🔥 QR config
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 1);

            // 🔥 create QR
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size, hints);

            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(matrix);

            // 🔥 add logo (IMPORTANT FIX)
            if (logoPath != null && !logoPath.isEmpty()) {
                File file = new File(logoPath);
                if (file.exists()) {
                    BufferedImage logo = ImageIO.read(file);

                    int logoSize = size / 5;

                    // 🔥 convert correctly
                    BufferedImage resizedLogo = new BufferedImage(logoSize, logoSize, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D gLogo = resizedLogo.createGraphics();
                    gLogo.drawImage(logo, 0, 0, logoSize, logoSize, null);
                    gLogo.dispose();

                    // 🔥 merge
                    Graphics2D g = qrImage.createGraphics();
                    int x = (size - logoSize) / 2;
                    int y = (size - logoSize) / 2;
                    g.drawImage(resizedLogo, x, y, null);
                    g.dispose();
                }
            }

            // 🔥 convert to JavaFX Image
            return SwingFXUtils.toFXImage(qrImage, null);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}