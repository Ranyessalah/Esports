package esprit.tn.esports.utils;

import esprit.tn.esports.entite.Equipe;
import esprit.tn.esports.entite.Player;
import esprit.tn.esports.service.EquipeService;
import esprit.tn.esports.service.PlayerService;
import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;

/**
 * A ultra-lightweight web server (NanoHTTPD) to display team details on mobile devices.
 * This version is more stable and avoids conflicts with JavaFX.
 */
public class TeamWebServer extends NanoHTTPD {

    private final EquipeService equipeService = new EquipeService();
    private final PlayerService playerService = new PlayerService();
    private static String localIp = "localhost";
    private static TeamWebServer instance;

    public TeamWebServer() {
        super(4567);
    }

    public static void startServer() {
        try {
            if (instance == null) {
                instance = new TeamWebServer();
                instance.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
                localIp = discoverLocalIp();
                System.out.println("--- NANO-SERVER STARTED ---");
                System.out.println("Mobile View available at: http://" + localIp + ":4567/equipe/<id>");
            }
        } catch (IOException e) {
            System.err.println("Could not start team web server: " + e.getMessage());
        }
    }

    public static String getLocalIp() {
        return localIp;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        
        if (uri.startsWith("/equipe/")) {
            try {
                String idStr = uri.substring("/equipe/".length());
                int id = Integer.parseInt(idStr);
                
                Equipe equipe = equipeService.getById(id);
                if (equipe == null) {
                    return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/html", "<h1>Équipe non trouvée</h1>");
                }

                List<Player> players = playerService.getPlayersByEquipe(id);
                String html = renderTeamPage(equipe, players);
                return newFixedLengthResponse(Response.Status.OK, "text/html", html);
                
            } catch (Exception e) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/html", "<h1>Erreur</h1><p>" + e.getMessage() + "</p>");
            }
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/html", "<h1>Page non trouvée</h1>");
    }

    private static String discoverLocalIp() {
        // 1. Try DatagramSocket trick (Best for finding the primary outbound route)
        try (final java.net.DatagramSocket socket = new java.net.DatagramSocket()) {
            socket.connect(java.net.InetAddress.getByName("8.8.8.8"), 10002);
            String ip = socket.getLocalAddress().getHostAddress();
            if (ip != null && !ip.equals("0.0.0.0") && !ip.equals("127.0.0.1") && !ip.contains(":")) {
                return ip;
            }
        } catch (Exception ignored) {}

        // 2. Comprehensive iteration of all network interfaces
        try {
            java.util.List<String> candidates = new java.util.ArrayList<>();
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    // We only want IPv4 addresses that aren't loopback
                    if (addr instanceof java.net.Inet4Address && !addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        // Prioritize common local subnets
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            return ip;
                        }
                        candidates.add(ip);
                    }
                }
            }
            if (!candidates.isEmpty()) return candidates.get(0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 3. Last resort fallback
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "localhost";
        }
    }

    private String renderTeamPage(Equipe equipe, List<Player> players) {
        String nom = equipe.getNom() != null ? escHtml(equipe.getNom()) : "Équipe";
        String game = equipe.getGame() != null ? escHtml(equipe.getGame()) : "N/A";
        String cat  = equipe.getCategorie() != null ? escHtml(equipe.getCategorie()) : "N/A";
        String coach = (equipe.getCoach() != null && equipe.getCoach().getUser() != null)
                ? escHtml(equipe.getCoach().getUser().getEmail()) : "Non assigné";

        StringBuilder rows = new StringBuilder();
        if (players == null || players.isEmpty()) {
            rows.append("<tr><td colspan='3' style='text-align:center; padding:30px; color:#9ca3af;'>Aucun joueur inscrit</td></tr>");
        } else {
            int i = 1;
            for (Player p : players) {
                String pUser = (p.getUser() != null) ? escHtml(p.getUser().getEmail()) : "Joueur #" + p.getId();
                String pLvl = p.getNiveau() != null ? escHtml(p.getNiveau()) : "Membre";
                rows.append("<tr>")
                    .append("<td style='padding:12px; color:#94a3b8; font-size:12px;'>").append(i++).append("</td>")
                    .append("<td style='padding:12px; font-weight:600;'>").append(pUser).append("</td>")
                    .append("<td style='padding:12px;'><span style='background:#f1f5f9; padding:4px 10px; border-radius:6px; font-size:11px; font-weight:700;'>").append(pLvl).append("</span></td>")
                    .append("</tr>");
            }
        }

        return "<!DOCTYPE html><html><head>"
            + "<meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<link href='https://fonts.googleapis.com/css2?family=Outfit:wght@400;600;800&display=swap' rel='stylesheet'>"
            + "<style>"
            + "body { font-family:'Outfit',sans-serif; background:#f8fafc; color:#1e293b; margin:0; padding:15px; }"
            + ".card { background:#fff; border-radius:24px; box-shadow:0 10px 25px -5px rgba(0,0,0,0.1); overflow:hidden; max-width:500px; margin:0 auto; }"
            + ".header { background:#1e1b4b; padding:40px 25px; text-align:center; color:#fff; }"
            + ".logo-wrap { width:100px; height:100px; background:#fff; border-radius:50%; margin:0 auto 20px; display:flex; align-items:center; justify-content:center; box-shadow:0 0 0 8px rgba(255,255,255,0.1); }"
            + ".logo-wrap img { max-width:70%; max-height:70%; }"
            + "h1 { margin:0; font-size:28px; letter-spacing:-1px; }"
            + ".tag { display:inline-block; background:rgba(255,255,255,0.1); border:1px solid rgba(255,255,255,0.2); padding:4px 12px; border-radius:20px; font-size:11px; margin-top:10px; font-weight:600; }"
            + ".content { padding:25px; }"
            + ".meta { display:grid; grid-template-columns:1fr 1fr; gap:15px; margin-bottom:30px; }"
            + ".m-item { background:#f8fafc; padding:15px; border-radius:16px; border:1px solid #f1f5f9; }"
            + ".m-label { font-size:10px; color:#64748b; font-weight:700; text-transform:uppercase; margin-bottom:4px; display:block; }"
            + ".m-val { font-size:14px; font-weight:600; color:#1e293b; }"
            + "h2 { font-size:16px; margin:0 0 15px; color:#475569; display:flex; align-items:center; gap:8px; }"
            + "table { width:100%; border-collapse:collapse; }"
            + "th { text-align:left; font-size:11px; color:#64748b; text-transform:uppercase; padding:10px; border-bottom:1px solid #f1f5f9; }"
            + "tr:last-child td { border:none; }"
            + ".footer { text-align:center; padding:20px; font-size:11px; color:#94a3b8; }"
            + "</style></head><body>"
            + "<div class='card'>"
            + "  <div class='header'>"
            + "    <div class='logo-wrap'><img src='https://api.dicebear.com/7.x/initials/svg?seed=" + nom + "'></div>"
            + "    <h1>" + nom + "</h1>"
            + "    <div class='tag'>" + game.toUpperCase() + " • " + cat.toUpperCase() + "</div>"
            + "  </div>"
            + "  <div class='content'>"
            + "    <div class='meta'>"
            + "      <div class='m-item'><span class='m-label'>Coach Principal</span><span class='m-val'>" + coach + "</span></div>"
            + "      <div class='m-item'><span class='m-label'>Membres</span><span class='m-val'>" + (players != null ? players.size() : 0) + " joueurs</span></div>"
            + "    </div>"
            + "    <h2>📋 Roster de l'équipe</h2>"
            + "    <table>"
            + "      <thead><tr><th>#</th><th>Nom / Email</th><th>Rôle</th></tr></thead>"
            + "      <tbody>" + rows + "</tbody>"
            + "    </table>"
            + "  </div>"
            + "  <div class='footer'>Rapport Officiel ClutchX • " + java.time.LocalDate.now() + "</div>"
            + "</div>"
            + "</body></html>";
    }

    /** Escape HTML special characters */
    private String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
