package com.example.studysprint;

import com.example.studysprint.modules.quizz.controllers.DashboardController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        TabPane tabPane = new TabPane();

        tabPane.getTabs().addAll(
                loadDashboardTab(),
                loadTab("Quiz — Étudiant",      "/fxml/quizz/QuizFront.fxml"),
                loadTab("Quiz — Admin",          "/fxml/quizz/QuizBack.fxml"),
                loadTab("Flashcards — Étudiant", "/fxml/quizz/FlashcardFront.fxml"),
                loadTab("Flashcards — Admin",    "/fxml/quizz/FlashcardBack.fxml")
        );

        BorderPane root = new BorderPane(tabPane);
        Scene scene = new Scene(root, 1060, 700);

        // ── Charger le CSS StudySprint ──
        String css = Objects.requireNonNull(
                getClass().getResource("/css/studysprint.css"),
                "studysprint.css introuvable"
        ).toExternalForm();
        scene.getStylesheets().add(css);

        stage.setTitle("StudySprint — Quiz & Flashcards");
        stage.setScene(scene);
        stage.show();
    }

    private Tab loadDashboardTab() throws IOException {
        URL resource = getClass().getResource("/fxml/quizz/Dashboard.fxml");
        if (resource == null) throw new IOException("FXML non trouvé : Dashboard.fxml");
        FXMLLoader loader = new FXMLLoader(resource);
        Tab tab = new Tab("📊 Dashboard", loader.load());
        tab.setClosable(false);
        DashboardController ctrl = loader.getController();
        ctrl.initDashboard(1L);
        return tab;
    }

    private Tab loadTab(String title, String fxmlPath) throws IOException {
        URL resource = getClass().getResource(fxmlPath);
        if (resource == null) throw new IOException("FXML non trouvé : " + fxmlPath);
        FXMLLoader loader = new FXMLLoader(resource);
        Tab tab = new Tab(title, loader.load());
        tab.setClosable(false);
        return tab;
    }
}
