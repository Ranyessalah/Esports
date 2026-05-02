package esprit.tn.esports.utils;

import fi.iki.elonen.NanoHTTPD;
import esprit.tn.esports.entite.Coach;
import esprit.tn.esports.entite.Equipe;
import esprit.tn.esports.entite.Player;
import esprit.tn.esports.service.EquipeService;
import esprit.tn.esports.service.PlayerService;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Inet4Address;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class TeamWebServer extends NanoHTTPD {

    private static final int PORT = 8081;
    private static TeamWebServer instance;
    private static String localIpAddress = "localhost";

    public TeamWebServer() throws IOException {
        super(PORT);
        detectLocalIp();
        start(SOCKET_READ_TIMEOUT, false);
        System.out.println("Server started: http://" + localIpAddress + ":" + PORT);
        System.out.println("QR Code URL base: http://" + localIpAddress + ":" + PORT + "/team?id=");
    }

    private void detectLocalIp() {
        try {
            // Best effort: resolve the currently routed local IPv4 (typically Wi-Fi/LAN).
            String routedIp = detectRoutedLocalIp();
            if (routedIp != null && !routedIp.isBlank()) {
                localIpAddress = routedIp;
                return;
            }

            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                    continue;
                }
                String name = (ni.getName() + " " + ni.getDisplayName()).toLowerCase();
                if (name.contains("virtual")
                        || name.contains("vethernet")
                        || name.contains("vmware")
                        || name.contains("hyper-v")
                        || name.contains("docker")
                        || name.contains("loopback")) {
                    continue;
                }
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress() && addr.isSiteLocalAddress()) {
                        localIpAddress = addr.getHostAddress();
                        return;
                    }
                }
            }
            // Fallback: évite localhost si possible
            InetAddress host = InetAddress.getLocalHost();
            if (host instanceof Inet4Address && !host.isLoopbackAddress()) {
                localIpAddress = host.getHostAddress();
                return;
            }
        } catch (Exception e) {
            System.err.println("Impossible de detecter l'IP locale, utilisation de localhost");
        }
    }

    private String detectRoutedLocalIp() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(new InetSocketAddress("8.8.8.8", 53));
            InetAddress local = socket.getLocalAddress();
            if (local instanceof Inet4Address && !local.isLoopbackAddress() && local.isSiteLocalAddress()) {
                return local.getHostAddress();
            }
        } catch (Exception ignored) {
            // fallback handled by interface scan
        }
        return null;
    }

    public static synchronized void startServer() {
        try {
            if (instance == null) {
                instance = new TeamWebServer();
            }
        } catch (IOException e) {
            System.err.println("Erreur au demarrage du serveur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void stopServer() {
        if (instance != null) {
            instance.stop();
            instance = null;
            System.out.println("Server arrete");
        }
    }

    public static String getLocalIp() {
        return localIpAddress;
    }

    public static boolean isRunning() {
        return instance != null;
    }

    public static String getServerUrl() {
        if (instance == null) {
            startServer();
        }
        return "http://" + localIpAddress + ":" + PORT;
    }

    /**
     * Some clients / NanoHTTPD versions leave GET query params out of getParms();
     * always parse the raw query string as a fallback.
     */
    private static String getQueryParam(IHTTPSession session, String name) {
        Map<String, String> parms = session.getParms();
        if (parms != null) {
            String v = parms.get(name);
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        String query = session.getQueryParameterString();
        if (query == null || query.isBlank()) {
            return null;
        }
        for (String part : query.split("&")) {
            if (part.isEmpty()) {
                continue;
            }
            int eq = part.indexOf('=');
            if (eq < 0) {
                if (name.equals(part)) {
                    return "";
                }
                continue;
            }
            String k = part.substring(0, eq);
            if (!name.equals(k)) {
                continue;
            }
            String val = part.length() > eq + 1 ? part.substring(eq + 1) : "";
            try {
                return URLDecoder.decode(val, StandardCharsets.UTF_8).trim();
            } catch (Exception e) {
                return val.trim();
            }
        }
        return null;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        if (uri == null) {
            uri = "/";
        }
        // Ignore query string in path (NanoHTTPD usually strips it)
        int pathEnd = uri.indexOf('?');
        if (pathEnd >= 0) {
            uri = uri.substring(0, pathEnd);
        }

        if (uri.equals("/") || uri.equals("/index.html")) {
            String html = generateHomePage();
            return newFixedLengthResponse(Response.Status.OK, "text/html", html);
        }

        if (uri.equals("/team")) {
            String idParam = getQueryParam(session, "id");

            if (idParam == null || idParam.isEmpty()) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/html",
                        generateErrorPage("ID d'equipe manquant"));
            }

            try {
                int id = Integer.parseInt(idParam);
                EquipeService service = new EquipeService();
                Equipe e = service.getById(id);

                if (e == null) {
                    return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/html",
                            generateErrorPage("Equipe non trouvee avec l'ID: " + id));
                }

                List<Player> players = new PlayerService().getPlayersByEquipe(id);
                e.setPlayers(players);

                String html = generatePage(e);
                return newFixedLengthResponse(Response.Status.OK, "text/html", html);
            } catch (NumberFormatException e) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/html",
                        generateErrorPage("ID d'equipe invalide"));
            }
        }

        if (uri.equals("/qrcode")) {
            String idParam = getQueryParam(session, "id");
            String sizeParam = getQueryParam(session, "size");

            if (idParam == null || idParam.isEmpty()) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain",
                        "ID d'equipe manquant");
            }

            try {
                int id = Integer.parseInt(idParam);
                int size = 300;
                if (sizeParam != null && !sizeParam.isEmpty()) {
                    size = Integer.parseInt(sizeParam);
                    size = Math.min(size, 800);
                }

                String url = getServerUrl() + "/team?id=" + id;
                byte[] qrCodeImage = generateQRCodeImage(url, size, size);
                String base64Image = Base64.getEncoder().encodeToString(qrCodeImage);

                String html = generateQRCodePage(id, base64Image, url);
                return newFixedLengthResponse(Response.Status.OK, "text/html", html);
            } catch (NumberFormatException e) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain",
                        "ID d'equipe invalide");
            } catch (WriterException | IOException e) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain",
                        "Erreur lors de la generation du QR code: " + e.getMessage());
            }
        }

        if (uri.equals("/qrcode/image")) {
            String idParam = getQueryParam(session, "id");

            if (idParam == null || idParam.isEmpty()) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain",
                        "ID d'equipe manquant");
            }

            try {
                int id = Integer.parseInt(idParam);
                String url = getServerUrl() + "/team?id=" + id;
                byte[] qrCodeImage = generateQRCodeImage(url, 400, 400);

                ByteArrayInputStream inputStream = new ByteArrayInputStream(qrCodeImage);
                Response response = newFixedLengthResponse(Response.Status.OK, "image/png", inputStream, qrCodeImage.length);
                response.addHeader("Content-Disposition", "attachment; filename=\"qrcode_team_" + id + ".png\"");
                return response;
            } catch (NumberFormatException e) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain",
                        "ID d'equipe invalide");
            } catch (WriterException | IOException e) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain",
                        "Erreur lors de la generation du QR code: " + e.getMessage());
            }
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/html",
                generateErrorPage("Page non trouvee"));
    }

    private byte[] generateQRCodeImage(String text, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        return outputStream.toByteArray();
    }

    private String generateHomePage() {
        String serverUrl = getServerUrl();
        return "<!DOCTYPE html>\n"
                + "<html>\n"
                + "<head>\n"
                + "    <meta charset=\"UTF-8\">\n"
                + "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                + "    <title>Generateur de QR Code - Equipes E-Sports</title>\n"
                + "    <style>\n"
                + "        * { margin: 0; padding: 0; box-sizing: border-box; }\n"
                + "        body {\n"
                + "            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;\n"
                + "            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n"
                + "            min-height: 100vh;\n"
                + "            padding: 20px;\n"
                + "        }\n"
                + "        .container {\n"
                + "            max-width: 600px;\n"
                + "            margin: 0 auto;\n"
                + "            background: white;\n"
                + "            border-radius: 20px;\n"
                + "            overflow: hidden;\n"
                + "            box-shadow: 0 20px 60px rgba(0,0,0,0.3);\n"
                + "        }\n"
                + "        .header {\n"
                + "            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n"
                + "            color: white;\n"
                + "            padding: 40px;\n"
                + "            text-align: center;\n"
                + "        }\n"
                + "        .header h1 { font-size: 2rem; margin-bottom: 10px; }\n"
                + "        .content { padding: 40px; }\n"
                + "        .form-group { margin-bottom: 25px; }\n"
                + "        label { display: block; font-weight: bold; margin-bottom: 8px; color: #333; }\n"
                + "        input {\n"
                + "            width: 100%;\n"
                + "            padding: 12px;\n"
                + "            border: 2px solid #e0e0e0;\n"
                + "            border-radius: 10px;\n"
                + "            font-size: 16px;\n"
                + "        }\n"
                + "        input:focus { outline: none; border-color: #667eea; }\n"
                + "        button {\n"
                + "            width: 100%;\n"
                + "            padding: 14px;\n"
                + "            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n"
                + "            color: white;\n"
                + "            border: none;\n"
                + "            border-radius: 10px;\n"
                + "            font-size: 16px;\n"
                + "            font-weight: bold;\n"
                + "            cursor: pointer;\n"
                + "        }\n"
                + "        button:hover { transform: translateY(-2px); }\n"
                + "        .result {\n"
                + "            margin-top: 30px;\n"
                + "            padding: 20px;\n"
                + "            background: #f8f9fa;\n"
                + "            border-radius: 10px;\n"
                + "            text-align: center;\n"
                + "            display: none;\n"
                + "        }\n"
                + "        .result.show { display: block; }\n"
                + "        .qr-container img { max-width: 250px; border-radius: 10px; }\n"
                + "        .info-server {\n"
                + "            margin-top: 20px;\n"
                + "            padding: 15px;\n"
                + "            background: #e0e7ff;\n"
                + "            border-radius: 10px;\n"
                + "            text-align: center;\n"
                + "        }\n"
                + "        .footer {\n"
                + "            background: #f8f9fa;\n"
                + "            padding: 20px;\n"
                + "            text-align: center;\n"
                + "            color: #666;\n"
                + "        }\n"
                + "    </style>\n"
                + "</head>\n"
                + "<body>\n"
                + "    <div class=\"container\">\n"
                + "        <div class=\"header\">\n"
                + "            <h1>Generateur de QR Code</h1>\n"
                + "            <p>Creez un QR code pour partager les informations de votre equipe</p>\n"
                + "        </div>\n"
                + "        <div class=\"content\">\n"
                + "            <div class=\"form-group\">\n"
                + "                <label for=\"teamId\">ID de l'equipe</label>\n"
                + "                <input type=\"number\" id=\"teamId\" placeholder=\"Entrez l'ID de l'equipe\">\n"
                + "            </div>\n"
                + "            <button onclick=\"generateQRCode()\">Generer le QR Code</button>\n"
                + "            <div class=\"result\" id=\"result\">\n"
                + "                <h3>QR Code genere avec succes</h3>\n"
                + "                <div class=\"qr-container\" id=\"qrContainer\"></div>\n"
                + "            </div>\n"
                + "            <div class=\"info-server\">\n"
                + "                Serveur accessible sur : <code>" + serverUrl + "</code>\n"
                + "            </div>\n"
                + "        </div>\n"
                + "        <div class=\"footer\">\n"
                + "            <p>ClutchX Esports</p>\n"
                + "        </div>\n"
                + "    </div>\n"
                + "    <script>\n"
                + "        function generateQRCode() {\n"
                + "            const teamId = document.getElementById('teamId').value;\n"
                + "            if (!teamId) { alert('Veuillez entrer un ID'); return; }\n"
                + "            const serverUrl = '" + serverUrl + "';\n"
                + "            const qrUrl = serverUrl + '/qrcode?id=' + teamId;\n"
                + "            document.getElementById('qrContainer').innerHTML = '<img src=\"' + qrUrl + '\" alt=\"QR Code\">';\n"
                + "            document.getElementById('result').classList.add('show');\n"
                + "        }\n"
                + "    </script>\n"
                + "</body>\n"
                + "</html>";
    }

    private String generateQRCodePage(int teamId, String qrCodeBase64, String url) {
        return "<!DOCTYPE html>\n"
                + "<html>\n"
                + "<head>\n"
                + "    <meta charset=\"UTF-8\">\n"
                + "    <title>QR Code - Equipe</title>\n"
                + "    <style>\n"
                + "        body {\n"
                + "            font-family: Arial, sans-serif;\n"
                + "            display: flex;\n"
                + "            justify-content: center;\n"
                + "            align-items: center;\n"
                + "            min-height: 100vh;\n"
                + "            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n"
                + "        }\n"
                + "        .card {\n"
                + "            background: white;\n"
                + "            padding: 40px;\n"
                + "            border-radius: 20px;\n"
                + "            text-align: center;\n"
                + "        }\n"
                + "        img { max-width: 250px; margin: 20px 0; }\n"
                + "    </style>\n"
                + "</head>\n"
                + "<body>\n"
                + "    <div class=\"card\">\n"
                + "        <h1>QR Code Equipe</h1>\n"
                + "        <img src=\"data:image/png;base64," + qrCodeBase64 + "\" alt=\"QR Code\">\n"
                + "        <p><a href=\"" + url + "\">Voir l'equipe</a></p>\n"
                + "    </div>\n"
                + "</body>\n"
                + "</html>";
    }

    private String generatePage(Equipe e) {
        StringBuilder coachBlock = new StringBuilder();
        Coach c = e.getCoach();
        if (c != null) {
            String email = c.getUser() != null && c.getUser().getEmail() != null
                    ? c.getUser().getEmail() : "N/A";
            String spec = c.getSpecialite() != null && !c.getSpecialite().isBlank()
                    ? c.getSpecialite() : "—";
            String pays = c.getPays() != null && !c.getPays().isBlank() ? c.getPays() : "—";
            coachBlock.append("            <div class=\"info-item\">\n")
                    .append("                <div class=\"info-label\">Coach</div>\n")
                    .append("                <div class=\"info-value\">").append(escHtml(email)).append("</div>\n")
                    .append("                <div class=\"info-sub\">Specialite: ").append(escHtml(spec))
                    .append(" &nbsp;|&nbsp; Pays: ").append(escHtml(pays)).append("</div>\n")
                    .append("            </div>\n");
        } else {
            coachBlock.append("            <div class=\"info-item\">\n")
                    .append("                <div class=\"info-label\">Coach</div>\n")
                    .append("                <div class=\"info-value muted\">Aucun coach assigne</div>\n")
                    .append("            </div>\n");
        }

        StringBuilder playersList = new StringBuilder();
        List<Player> pl = e.getPlayers();
        if (pl != null && !pl.isEmpty()) {
            playersList.append("            <div class=\"section-title\">Joueurs (").append(pl.size()).append(")</div>\n")
                    .append("            <ul class=\"player-list\">\n");
            for (Player p : pl) {
                String name = p.getUser() != null && p.getUser().getEmail() != null
                        ? p.getUser().getEmail() : ("Joueur #" + p.getId());
                String pays = p.getPays() != null ? p.getPays() : "—";
                String niv = p.getNiveau() != null ? p.getNiveau() : "—";
                playersList.append("                <li><span class=\"p-name\">").append(escHtml(name)).append("</span>")
                        .append("<span class=\"p-meta\">").append(escHtml(pays)).append(" &middot; ")
                        .append(escHtml(niv)).append("</span></li>\n");
            }
            playersList.append("            </ul>\n");
        } else {
            playersList.append("            <div class=\"info-item\">\n")
                    .append("                <div class=\"info-label\">Joueurs</div>\n")
                    .append("                <div class=\"info-value muted\">Aucun joueur dans cette equipe</div>\n")
                    .append("            </div>\n");
        }

        return "<!DOCTYPE html>\n"
                + "<html>\n"
                + "<head>\n"
                + "    <meta charset=\"UTF-8\">\n"
                + "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                + "    <title>" + escHtml(e.getNom()) + " - Equipe E-Sports</title>\n"
                + "    <style>\n"
                + "        html { -webkit-text-size-adjust: 100%; }\n"
                + "        body {\n"
                + "            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif;\n"
                + "            display: flex;\n"
                + "            flex-direction: column;\n"
                + "            align-items: center;\n"
                + "            justify-content: flex-start;\n"
                + "            min-height: 100vh;\n"
                + "            margin: 0;\n"
                + "            padding: 20px 16px 40px;\n"
                + "            box-sizing: border-box;\n"
                + "            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n"
                + "        }\n"
                + "        .card {\n"
                + "            background: white;\n"
                + "            border-radius: 20px;\n"
                + "            max-width: 520px;\n"
                + "            width: 100%;\n"
                + "            box-shadow: 0 12px 40px rgba(0,0,0,0.2);\n"
                + "            flex-shrink: 0;\n"
                + "        }\n"
                + "        .header {\n"
                + "            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n"
                + "            color: white;\n"
                + "            padding: 28px;\n"
                + "            text-align: center;\n"
                + "        }\n"
                + "        .content { padding: 24px; }\n"
                + "        .info-item {\n"
                + "            margin-bottom: 16px;\n"
                + "            padding: 16px;\n"
                + "            background: #f8f9fa;\n"
                + "            border-radius: 12px;\n"
                + "        }\n"
                + "        .info-label { font-size: 12px; color: #667eea; text-transform: uppercase; letter-spacing: 0.5px; }\n"
                + "        .info-value { font-size: 18px; font-weight: bold; color: #1a1a2e; margin-top: 4px; word-break: break-all; }\n"
                + "        .info-sub { font-size: 14px; color: #555; margin-top: 8px; }\n"
                + "        .muted { color: #888; font-weight: normal; }\n"
                + "        .section-title { font-size: 14px; font-weight: 700; color: #667eea; margin: 8px 0 12px; }\n"
                + "        .player-list { list-style: none; margin: 0; padding: 0; }\n"
                + "        .player-list li { display: flex; flex-direction: column; padding: 12px 16px; border-radius: 10px; background: #f0f2ff; margin-bottom: 8px; }\n"
                + "        .p-name { font-weight: 600; color: #1a1a2e; word-break: break-all; }\n"
                + "        .p-meta { font-size: 13px; color: #64748b; margin-top: 4px; }\n"
                + "    </style>\n"
                + "</head>\n"
                + "<body>\n"
                + "    <div class=\"card\">\n"
                + "        <div class=\"header\">\n"
                + "            <h1>" + escHtml(e.getNom()) + "</h1>\n"
                + "        </div>\n"
                + "        <div class=\"content\">\n"
                + "            <div class=\"info-item\">\n"
                + "                <div class=\"info-label\">Jeu</div>\n"
                + "                <div class=\"info-value\">" + escHtml(e.getGame() != null ? e.getGame() : "Non specifie") + "</div>\n"
                + "            </div>\n"
                + "            <div class=\"info-item\">\n"
                + "                <div class=\"info-label\">Categorie</div>\n"
                + "                <div class=\"info-value\">" + escHtml(e.getCategorie() != null ? e.getCategorie() : "Non specifiee") + "</div>\n"
                + "            </div>\n"
                + coachBlock
                + playersList
                + "        </div>\n"
                + "    </div>\n"
                + "</body>\n"
                + "</html>";
    }

    private static String escHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String generateErrorPage(String message) {
        return "<!DOCTYPE html>\n"
                + "<html>\n"
                + "<head>\n"
                + "    <meta charset=\"UTF-8\">\n"
                + "    <title>Erreur</title>\n"
                + "    <style>\n"
                + "        body {\n"
                + "            font-family: Arial, sans-serif;\n"
                + "            display: flex;\n"
                + "            justify-content: center;\n"
                + "            align-items: center;\n"
                + "            min-height: 100vh;\n"
                + "            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n"
                + "        }\n"
                + "        .card {\n"
                + "            background: white;\n"
                + "            padding: 40px;\n"
                + "            border-radius: 20px;\n"
                + "            text-align: center;\n"
                + "        }\n"
                + "    </style>\n"
                + "</head>\n"
                + "<body>\n"
                + "    <div class=\"card\">\n"
                + "        <h1>Erreur</h1>\n"
                + "        <p>" + message + "</p>\n"
                + "        <a href=\"/\">Retour a l'accueil</a>\n"
                + "    </div>\n"
                + "</body>\n"
                + "</html>";
    }
}