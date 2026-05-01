package esprit.tn.esports.controller;

import esprit.tn.esports.HelloApplication;
import esprit.tn.esports.entite.User;
import esprit.tn.esports.service.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button loginButton;

    private UserService userService;

    public void initialize() {
        userService = new UserService();
    }

    @FXML
    void handleLogin(ActionEvent event) {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Veuillez remplir tous les champs.");
            return;
        }

        User user = userService.login(email, password);

        if (user != null) {
            // Login successful
            errorLabel.setText("");
            
            // Check roles to determine which page to load
            String role = user.getRoles();
            String fxmlFile = "";
            
            // Check if role contains ADMIN (basic check for string or JSON array ["ROLE_ADMIN"])
            if (role != null && (role.contains("ROLE_ADMIN") || role.contains("ADMIN"))) {
                fxmlFile = "matchIndex.fxml"; // Path relative to HelloApplication
            } else {
                fxmlFile = "matchIndex_client.fxml"; // Path relative to HelloApplication
            }

            try {
                // Determine resource using HelloApplication scope
                FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxmlFile));
                Parent root = loader.load();
                
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                Scene scene = new Scene(root, 1200, 760);
                stage.setScene(scene);
                stage.show();
                
            } catch (IOException e) {
                e.printStackTrace();
                errorLabel.setText("Erreur lors du chargement de la page.");
            }
            
        } else {
            errorLabel.setText("Email ou mot de passe incorrect.");
        }
    }
}
