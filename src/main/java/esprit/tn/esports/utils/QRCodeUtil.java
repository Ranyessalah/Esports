package esprit.tn.esports.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import esprit.tn.esports.entite.Equipe;
import esprit.tn.esports.entite.Player;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for QR Code generation and handling
 */
public class QRCodeUtil {

    /**
     * Generate a QR Code image from the given text
     * @param text The text to encode
     * @param width Width of the QR code
     * @param height Height of the QR code
     * @return JavaFX Image of the QR code
     * @throws Exception if QR code generation fails
     */
    public static Image generateQRCodeImage(String text, int width, int height) throws Exception {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        hints.put(EncodeHintType.MARGIN, 1);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix;
        try {
            bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);
        } catch (WriterException e) {
            String ascii = toAsciiQrPayload(text);
            try {
                bitMatrix = qrCodeWriter.encode(ascii, BarcodeFormat.QR_CODE, width, height, hints);
            } catch (WriterException e2) {
                String clipped = ascii.length() > 1200 ? ascii.substring(0, 1197) + "..." : ascii;
                try {
                    bitMatrix = qrCodeWriter.encode(clipped, BarcodeFormat.QR_CODE, width, height, hints);
                } catch (WriterException e3) {
                    String minimal = clipped.length() > 500 ? clipped.substring(0, 497) + "..." : clipped;
                    bitMatrix = qrCodeWriter.encode(minimal, BarcodeFormat.QR_CODE, width, height, hints);
                }
            }
        }

        java.awt.image.BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
        return SwingFXUtils.toFXImage(bufferedImage, null);
    }

    /** Remplace les émojis par des libellés courts pour réduire la taille encodée. */
    private static String toAsciiQrPayload(String s) {
        if (s == null) {
            return "";
        }
        return s
                .replace("🏆", "[EQ] ")
                .replace("👥", "[JO] ")
                .replace("👨‍🏫", "[CO] ")
                .replaceAll("\\R", "\n");
    }

    /**
     * Create a QR code string from team ID and team name
     * Format: TEAM_ID|TEAM_NAME
     * @param teamId The team ID
     * @param teamName The team name
     * @return The encoded QR string
     */
    public static String createTeamQRString(int teamId, String teamName) {
        return teamId + "|" + teamName;
    }

    /**
     * Encode the team's web page URL into the QR code.
     * Scanning with any phone camera will open the team details page in the browser.
     */
    public static String createRichTeamQRString(Equipe equipe, List<Player> players) {
        if (equipe == null) return "error";
        String ip = TeamWebServer.getLocalIp();
        return "http://" + ip + ":4567/equipe/" + equipe.getId();
    }

    private static String nullSafe(String s) {
        return s != null ? s : "";
    }

    /**
     * Parse a QR code string to extract team ID
     * @param qrText The QR code text
     * @return The team ID, or -1 if invalid format
     */
    private static final Pattern ID_APP_PATTERN = Pattern.compile("\\[ID APP:\\s*(\\d+)\\]", Pattern.CASE_INSENSITIVE);

    public static int extractTeamIdFromQR(String qrText) {
        if (qrText == null || qrText.isBlank()) {
            return -1;
        }
        String trimmed = qrText.trim();
        // 1) Première ligne au format id|nom
        String firstLine = trimmed.split("\\R", 2)[0].trim();
        try {
            int pipe = firstLine.indexOf('|');
            if (pipe > 0) {
                return Integer.parseInt(firstLine.substring(0, pipe).trim());
            }
            if (!firstLine.isEmpty() && !firstLine.contains("|")) {
                return Integer.parseInt(firstLine);
            }
        } catch (NumberFormatException ignored) {
            // fall through
        }
        // 2) Ancien format : [ID APP: n] dans le texte enrichi
        Matcher m = ID_APP_PATTERN.matcher(trimmed);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }
        return -1;
    }

    /**
     * Parse a QR code string to extract team name
     * @param qrText The QR code text
     * @return The team name, or empty string if invalid format
     */
    public static String extractTeamNameFromQR(String qrText) {
        if (qrText == null || qrText.isBlank()) {
            return "";
        }
        String firstLine = qrText.trim().split("\\R", 2)[0];
        String[] parts = firstLine.split("\\|", 2);
        if (parts.length >= 2) {
            return parts[1].trim();
        }
        return "";
    }
}
