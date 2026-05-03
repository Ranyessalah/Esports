package services.userManagement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import utils.ConfigLoader;

import java.awt.Desktop;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Google OAuth2 service for JavaFX desktop apps.
 *
 * Credentials come from:
 *   client_secret_...apps.googleusercontent.com.json
 *
 * Flow:
 *   1. openBrowser()  → user authenticates in their default browser
 *   2. Local server on :8769 catches the redirect with ?code=...
 *   3. exchangeCodeForToken() → POST to token endpoint
 *   4. fetchUserInfo()        → GET https://www.googleapis.com/oauth2/v3/userinfo
 */
public class GoogleAuthService {

    // ── OAuth app credentials ─────────────────────────────────────────────

    private HttpServer callbackServer;
    private final ObjectMapper mapper = new ObjectMapper();

    // ─────────────────────────── PUBLIC API ───────────────────────────────

    /**
     * Launches the full OAuth flow asynchronously.
     * Returns a {@link GoogleUser} on success, or throws on failure/timeout.
     *
     * Usage in LoginController:
     * <pre>
     *   GoogleAuthService google = new GoogleAuthService();
     *   google.authenticate().thenAccept(googleUser -> {
     *       Platform.runLater(() -> handleGoogleUser(googleUser));
     *   }).exceptionally(ex -> { Platform.runLater(() -> showError(ex)); return null; });
     * </pre>
     */
    public CompletableFuture<GoogleUser> authenticate() {
        CompletableFuture<String> codeFuture = new CompletableFuture<>();

        try {
            startCallbackServer(codeFuture);
            openBrowser(buildAuthUrl());
        } catch (Exception e) {
            codeFuture.completeExceptionally(e);
        }

        return codeFuture
                .orTimeout(120, TimeUnit.SECONDS)          // 2-minute timeout
                .thenCompose(this::exchangeCodeForToken)
                .thenCompose(this::fetchUserInfo)
                .whenComplete((u, ex) -> stopCallbackServer());
    }

    /** Stops the local callback server (call on window close / cleanup). */
    public void stop() {
        stopCallbackServer();
    }

    // ─────────────────────────── STEP 1 – Build auth URL ─────────────────

    private String buildAuthUrl() throws UnsupportedEncodingException {
        return ConfigLoader.get("AUTH_ENDPOINT")
                + "?client_id="     + URLEncoder.encode( ConfigLoader.get("CLIENT_ID"), "UTF-8")
                + "&redirect_uri="  + URLEncoder.encode( ConfigLoader.get("REDIRECT_URI"), "UTF-8")
                + "&response_type=code"
                + "&scope="         + URLEncoder.encode("openid email profile", "UTF-8")
                + "&access_type=online"
                + "&prompt=select_account";   // always show account picker
    }

    // ─────────────────────────── STEP 2 – Open browser ───────────────────

    private void openBrowser(String url) throws Exception {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(new URI(url));
        } else {
            // Fallback for headless / Linux without Desktop support
            Runtime.getRuntime().exec(new String[]{"xdg-open", url});
        }
    }

    // ─────────────────────────── STEP 3 – Local callback server ──────────

    private void startCallbackServer(CompletableFuture<String> codeFuture) throws IOException {
        stopCallbackServer();
        callbackServer = HttpServer.create(new InetSocketAddress("localhost", 8769), 0);

        callbackServer.createContext("/oauth2callback", exchange -> {
            String query = exchange.getRequestURI().getQuery(); // "code=...&scope=..."
            String code  = parseParam(query, "code");
            String error = parseParam(query, "error");

            String html;
            if (code != null) {
                html = successPage();
                codeFuture.complete(code);
            } else {
                html = errorPage(error != null ? error : "unknown_error");
                codeFuture.completeExceptionally(
                        new RuntimeException("Google OAuth error: " + error));
            }

            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        });

        callbackServer.start();
    }

    private void stopCallbackServer() {
        if (callbackServer != null) {
            callbackServer.stop(1);
            callbackServer = null;
        }
    }

    // ─────────────────────────── STEP 4 – Exchange code for token ────────

    private CompletableFuture<String> exchangeCodeForToken(String code) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String params = "code="          + URLEncoder.encode(code,          "UTF-8")
                        + "&client_id="     + URLEncoder.encode(ConfigLoader.get("CLIENT_ID"),     "UTF-8")
                        + "&client_secret=" + URLEncoder.encode( ConfigLoader.get("CLIENT_SECRET"),  "UTF-8")
                        + "&redirect_uri="  + URLEncoder.encode( ConfigLoader.get("REDIRECT_URI"),   "UTF-8")
                        + "&grant_type=authorization_code";

                HttpURLConnection conn = (HttpURLConnection)
                        new URL(ConfigLoader.get("TOKEN_ENDPOINT")).openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(params.getBytes(StandardCharsets.UTF_8));
                }

                String body = readResponse(conn);
                JsonNode json = mapper.readTree(body);

                if (json.has("error")) {
                    throw new RuntimeException("Token exchange failed: " + json.get("error").asText());
                }

                return json.get("access_token").asText();

            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("Token exchange error", e);
            }
        });
    }

    // ─────────────────────────── STEP 5 – Fetch user info ────────────────

    private CompletableFuture<GoogleUser> fetchUserInfo(String accessToken) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection)
                        new URL( ConfigLoader.get("USERINFO_URL")).openConnection();
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);

                String body = readResponse(conn);
                JsonNode json = mapper.readTree(body);

                return new GoogleUser(
                        json.path("sub").asText(),
                        json.path("email").asText(),
                        json.path("name").asText(),
                        json.path("given_name").asText(),
                        json.path("family_name").asText(),
                        json.path("picture").asText(),
                        json.path("email_verified").asBoolean()
                );
            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch user info", e);
            }
        });
    }

    // ─────────────────────────── HELPERS ─────────────────────────────────

    private String readResponse(HttpURLConnection conn) throws IOException {
        InputStream is = conn.getResponseCode() < 400
                ? conn.getInputStream() : conn.getErrorStream();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private String parseParam(String query, String key) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                try { return URLDecoder.decode(kv[1], "UTF-8"); }
                catch (Exception e) { return kv[1]; }
            }
        }
        return null;
    }

    // ─────────────────────────── Callback HTML pages ─────────────────────

    private String successPage() {
        return """
        <!DOCTYPE html>
        <html>
        <head><meta charset="UTF-8"><title>ClutchX — Connexion réussie</title>
        <style>
          body { font-family: Arial, sans-serif; background: #1a1a2e; color: #fff;
                 display: flex; justify-content: center; align-items: center;
                 min-height: 100vh; margin: 0; }
          .card { background: #16213e; border-radius: 12px; padding: 40px 48px;
                  text-align: center; box-shadow: 0 8px 32px rgba(0,0,0,.4); }
          .icon { font-size: 48px; margin-bottom: 16px; }
          h2 { margin: 0 0 8px; font-size: 22px; }
          p  { color: #aaa; margin: 0; }
        </style></head>
        <body>
          <div class="card">
            <div class="icon">✅</div>
            <h2>Authentification réussie !</h2>
            <p>Vous pouvez fermer cet onglet et revenir à ClutchX.</p>
          </div>
        </body>
        </html>
        """;
    }

    private String errorPage(String error) {
        return """
        <!DOCTYPE html>
        <html>
        <head><meta charset="UTF-8"><title>ClutchX — Erreur</title>
        <style>
          body { font-family: Arial, sans-serif; background: #1a1a2e; color: #fff;
                 display: flex; justify-content: center; align-items: center;
                 min-height: 100vh; margin: 0; }
          .card { background: #16213e; border-radius: 12px; padding: 40px 48px;
                  text-align: center; }
          .icon { font-size: 48px; margin-bottom: 16px; }
          h2 { color: #e74c3c; margin: 0 0 8px; }
          p  { color: #aaa; margin: 0; }
        </style></head>
        <body>
          <div class="card">
            <div class="icon">❌</div>
            <h2>Authentification échouée</h2>
            <p>Erreur : %s</p>
          </div>
        </body>
        </html>
        """.formatted(error);
    }

    // ─────────────────────────── VALUE OBJECT ────────────────────────────

    /** Immutable record holding the authenticated Google user's profile. */
    public record GoogleUser(
            String googleId,
            String email,
            String fullName,
            String firstName,
            String lastName,
            String pictureUrl,
            boolean emailVerified
    ) {}
}