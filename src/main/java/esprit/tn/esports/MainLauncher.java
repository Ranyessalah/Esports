package esprit.tn.esports;

import esprit.tn.esports.utils.TeamWebServer;
import javafx.application.Application;

public class MainLauncher {
    public static void main(String[] args) {
        // Start the local web server for mobile QR sharing
        try {
            TeamWebServer.startServer();
        } catch (Exception e) {
            System.err.println("Warning: Mobile web server could not start. " + e.getMessage());
        }
        
        Application.launch(HelloApplication.class, args);
    }
}
