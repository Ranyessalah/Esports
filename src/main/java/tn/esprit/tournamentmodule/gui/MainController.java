package tn.esprit.tournamentmodule.gui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.util.List;
import java.util.Objects;

public class MainController {

    @FXML private StackPane contentArea;
    @FXML private Label navHome;
    @FXML private Label navDashboard;
    @FXML private Label navLeagues;
    @FXML private Label navFixtures;

    @FXML
    public void initialize() {
        navigate("home", navHome);
    }

    @FXML public void onHome()      { navigate("home",      navHome);      }
    @FXML public void onDashboard() { navigate("dashboard", navDashboard); }
    @FXML public void onLeagues()   { navigate("league",    navLeagues);   }
    @FXML public void onFixtures()  { navigate("fixture",   navFixtures);  }

    private void navigate(String view, Label activeItem) {
        setActiveNav(activeItem);
        try {
            String fxml = switch (view) {
                case "home"      -> "/tn/esprit/tournamentmodule/view/HomeView.fxml";
                case "dashboard" -> "/tn/esprit/tournamentmodule/view/DashboardView.fxml";
                case "league"    -> "/tn/esprit/tournamentmodule/view/LeagueView.fxml";
                case "fixture"   -> "/tn/esprit/tournamentmodule/view/FixtureView.fxml";
                default          -> "/tn/esprit/tournamentmodule/view/HomeView.fxml";
            };
            Node content = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(fxml)));
            contentArea.getChildren().setAll(content);
        } catch (Exception e) {
            System.err.println("Navigation error: " + e.getMessage());
        }
    }

    private void setActiveNav(Label active) {
        for (Label l : List.of(navHome, navDashboard, navLeagues, navFixtures))
            l.getStyleClass().remove("nav-item-active");
        if (!active.getStyleClass().contains("nav-item-active"))
            active.getStyleClass().add("nav-item-active");
    }
}
