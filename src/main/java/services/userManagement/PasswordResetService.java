package services.userManagement;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class PasswordResetService {

    // email → (code, expiry)
    private static final ConcurrentHashMap<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String SENDER_EMAIL = "ghayeth.arbi@esprit.tn\n";    // ← à changer
    private static final String SENDER_PASSWORD = "nkac wpuh kbun mipq";    // ← mot de passe app Gmail

    // ── Générer et envoyer OTP ──────────────────────────────────────────

    public boolean sendOtp(String email) {
        // Vérifier que l'email existe en base
        try {
            UserService userService = new UserService();
            if (userService.findByEmail(email) == null) return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false; }

        String code = generateCode();
        otpStore.put(email, new OtpEntry(code, LocalDateTime.now().plusMinutes(5)));

        return sendEmail(email, code);
    }

    // ── Vérifier OTP ───────────────────────────────────────────────────

    public boolean verifyOtp(String email, String code) {
        OtpEntry entry = otpStore.get(email);
        if (entry == null) return false;
        if (LocalDateTime.now().isAfter(entry.expiry)) {
            otpStore.remove(email);
            return false;
        }
        return entry.code.equals(code);
    }

    // ── Réinitialiser le mot de passe ──────────────────────────────────

    public boolean resetPassword(String email, String otpCode, String newPassword) {
        if (!verifyOtp(email, otpCode)) return false;
        try {
            UserService userService = new UserService();
            userService.updatePassword(email, newPassword);
            otpStore.remove(email); // invalider le code après usage
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private String generateCode() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }

    private boolean sendEmail(String to, String code) {
        try {
            Properties properties = new Properties();
            properties.put("mail.smtp.host", "smtp.gmail.com");
            properties.put("mail.smtp.port", "587");
            properties.put("mail.smtp.ssl.trust", "smtp.gmail.com");
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");
            properties.setProperty("mail.smtp.ssl.protocols", "TLSv1.2");

            Authenticator authenticator = new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SENDER_EMAIL.trim(), SENDER_PASSWORD);
                }
            };

            Session session = Session.getInstance(properties, authenticator);

            // ✅ Un seul message, bien configuré
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(SENDER_EMAIL.trim(), "ClutchX"));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            msg.setSubject("ClutchX — Code de vérification", "UTF-8");

            // ✅ Clé du fix : MimeMultipart avec type "alternative" + part HTML
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(buildHtmlEmail(code), "text/html; charset=UTF-8");

            Multipart multipart = new MimeMultipart("alternative");
            multipart.addBodyPart(htmlPart);

            msg.setContent(multipart);

            Transport.send(msg);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    private String buildHtmlEmail(String code) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto;
                        background: #1a1a3a; border-radius: 12px; padding: 32px;
                        border: 1px solid rgba(124,58,237,0.35);">
                <h2 style="color: white; text-align: center; margin-bottom: 8px;">
                    🎮 ClutchX
                </h2>
                <p style="color: #a0a0c0; text-align: center; margin-bottom: 28px;">
                    Réinitialisation de mot de passe
                </p>
                <div style="background: rgba(124,58,237,0.15); border: 1px solid rgba(124,58,237,0.4);
                            border-radius: 12px; padding: 24px; text-align: center; margin-bottom: 24px;">
                    <p style="color: #c0c0e0; font-size: 14px; margin: 0 0 12px 0;">
                        Votre code de vérification :
                    </p>
                    <span style="font-size: 36px; font-weight: bold; color: white;
                                 letter-spacing: 12px;">%s</span>
                </div>
                <p style="color: #6060a0; font-size: 12px; text-align: center;">
                    Ce code expire dans <strong style="color: #a855f7;">5 minutes</strong>.<br>
                    Si vous n'avez pas demandé de réinitialisation, ignorez cet email.
                </p>
            </div>
        """.formatted(code);
    }

    // ── Data class ─────────────────────────────────────────────────────

    private static class OtpEntry {
        final String code;
        final LocalDateTime expiry;
        OtpEntry(String code, LocalDateTime expiry) {
            this.code = code; this.expiry = expiry;
        }
    }
}