package esprit.tn.esports;

import esprit.tn.esports.utils.TeamWebServer;

public class MainLauncher {

    private static final String SERVER_VERSION = "1.0";
    private static final String APP_NAME = "ClutchX Esports Management System";

    public static void main(String[] args) {
        // Afficher l'en-tête de l'application
        printHeader();

        // === Code de votre camarade conservé ===
        // Démarrer le serveur web avec support QR code
        try {
            System.out.println("🚀 Démarrage du serveur web avec support QR code...");
            TeamWebServer.startServer();

            // Afficher les informations de connexion
            String serverUrl = TeamWebServer.getServerUrl();
            String localIp = TeamWebServer.getLocalIp();

            System.out.println("✅ Serveur web démarré avec succès!");
            System.out.println("📡 URL locale: " + serverUrl);
            System.out.println("📱 IP pour QR codes: " + localIp);
            System.out.println("🔗 Test: " + serverUrl + "/team?id=1");
            System.out.println("📱 Génération QR Code: " + serverUrl + "/qrcode?id=1");

            // Afficher les instructions pour les QR codes
            printQRCodeInstructions();

        } catch (Exception e) {
            System.err.println("⚠️ Warning: Mobile web server could not start. " + e.getMessage());
            System.err.println("   Les fonctionnalités de QR code seront indisponibles.");
            e.printStackTrace();
        }

        // === Code de votre camarade conservé ===
        // Lancer l'application JavaFX
        System.out.println("🎮 Lancement de l'interface JavaFX...");
        try {
            HelloApplication.main(args);
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du lancement de l'application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Affiche l'en-tête de l'application
     */
    private static void printHeader() {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                                                          ║");
        System.out.println("║     🎮 " + APP_NAME + " v" + SERVER_VERSION + " 🎮     ║");
        System.out.println("║                                                          ║");
        System.out.println("║              Powered by ClutchX Esports                 ║");
        System.out.println("║                                                          ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    /**
     * Affiche les instructions pour l'utilisation des QR codes
     */
    private static void printQRCodeInstructions() {
        System.out.println();
        System.out.println("┌─────────────────────────────────────────────────────────┐");
        System.out.println("│ 📱 INSTRUCTIONS POUR LES QR CODES                        │");
        System.out.println("├─────────────────────────────────────────────────────────┤");
        System.out.println("│                                                         │");
        System.out.println("│ 1. Ouvrez l'application et allez dans l'onglet          │");
        System.out.println("│    'Générateur QR Code'                                 │");
        System.out.println("│                                                         │");
        System.out.println("│ 2. Sélectionnez une équipe dans la liste                │");
        System.out.println("│                                                         │");
        System.out.println("│ 3. Cliquez sur 'Générer QR Code'                        │");
        System.out.println("│                                                         │");
        System.out.println("│ 4. Scannez le QR code avec votre smartphone :          │");
        System.out.println("│    - Appareil photo (iPhone/Android récent)            │");
        System.out.println("│    - Application de scan QR code                       │");
        System.out.println("│    - Google Lens                                        │");
        System.out.println("│                                                         │");
        System.out.println("│ 5. Le navigateur s'ouvrira avec les détails de         │");
        System.out.println("│    l'équipe                                             │");
        System.out.println("│                                                         │");
        System.out.println("│ ⚠️  IMPORTANT:                                          │");
        System.out.println("│    - Le téléphone doit être sur le MÊME réseau WiFi    │");
        System.out.println("│    - Vérifiez que le pare-feu n' bloque pas le port 8081│");
        System.out.println("│    - IP locale détectée: " + TeamWebServer.getLocalIp() + "          │");
        System.out.println("│                                                         │");
        System.out.println("└─────────────────────────────────────────────────────────┘");
        System.out.println();
    }

    /**
     * Méthode utilitaire pour redémarrer l'application
     */
    public static void restartApplication() {
        try {
            System.out.println("🔄 Redémarrage de l'application...");
            TeamWebServer.stopServer();
            Thread.sleep(1000);
            main(new String[0]);
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du redémarrage: " + e.getMessage());
        }
    }

    /**
     * Méthode pour obtenir l'état du serveur
     */
    public static boolean isServerRunning() {
        return TeamWebServer.getServerUrl() != null;
    }

    /**
     * Affiche le menu d'aide dans la console
     */
    public static void printHelpMenu() {
        System.out.println();
        System.out.println("📖 AIDE - Commandes disponibles:");
        System.out.println("=================================");
        System.out.println("🌐 URL du serveur: " + TeamWebServer.getServerUrl());
        System.out.println("📱 QR Code API: " + TeamWebServer.getServerUrl() + "/qrcode?id={id}");
        System.out.println("👥 API Équipe: " + TeamWebServer.getServerUrl() + "/team?id={id}");
        System.out.println();
        System.out.println("💡 Exemples d'utilisation:");
        System.out.println("   - Voir équipe ID 1: " + TeamWebServer.getServerUrl() + "/team?id=1");
        System.out.println("   - Générer QR code: " + TeamWebServer.getServerUrl() + "/qrcode?id=1");
        System.out.println("   - Télécharger QR code: " + TeamWebServer.getServerUrl() + "/qrcode/image?id=1");
        System.out.println();
    }
}