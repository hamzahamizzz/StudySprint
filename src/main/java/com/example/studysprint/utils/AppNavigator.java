package com.example.studysprint.utils;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public final class AppNavigator {
    public static final String LOGIN_FXML = "/fxml/auth/login.fxml";
    public static final String LOGIN_TITLE = "Connexion - StudySprint";
    public static final String HOME_FXML = "/fxml/home/home.fxml";
    public static final String HOME_TITLE = "Accueil - StudySprint";
    public static final String ADMIN_FXML = "/fxml/utilisateurs/main-admin-layout.fxml";
    public static final String ADMIN_TITLE = "Tableau de Bord - StudySprint";

    private AppNavigator() {
    }

    public static boolean isAdmin(Utilisateur user) {
        return user != null && "ROLE_ADMIN".equalsIgnoreCase(user.getRole());
    }

    public static String defaultFxmlFor(Utilisateur user) {
        return isAdmin(user) ? ADMIN_FXML : HOME_FXML;
    }

    public static String defaultTitleFor(Utilisateur user) {
        return isAdmin(user) ? ADMIN_TITLE : HOME_TITLE;
    }

    public static boolean switchTo(Stage stage, String fxmlPath, String title, Class<?> resourceAnchor) {
        if (stage == null || resourceAnchor == null || fxmlPath == null || fxmlPath.isBlank()) {
            return false;
        }
        try {
            FXMLLoader loader = new FXMLLoader(resourceAnchor.getResource(fxmlPath));
            Parent root = loader.load();
            switchScene(stage, root, title);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void switchScene(Stage stage, Parent root, String title) {
        if (stage == null || root == null) {
            return;
        }

        double width = stage.getWidth();
        double height = stage.getHeight();
        double x = stage.getX();
        double y = stage.getY();
        boolean maximized = stage.isMaximized();
        boolean fullscreen = stage.isFullScreen();

        stage.setScene(new Scene(root));
        stage.setTitle(title);

        if (!maximized && !fullscreen) {
            stage.setWidth(width);
            stage.setHeight(height);
            stage.setX(x);
            stage.setY(y);
        }

        stage.setMaximized(maximized);
        stage.setFullScreen(fullscreen);
    }

    public static boolean openDefaultForUser(Stage stage, Utilisateur user, Class<?> resourceAnchor) {
        if (user == null) {
            return switchTo(stage, LOGIN_FXML, LOGIN_TITLE, resourceAnchor);
        }
        return switchTo(stage, defaultFxmlFor(user), defaultTitleFor(user), resourceAnchor);
    }

    public static boolean openDefaultForCurrentSession(Stage stage, Class<?> resourceAnchor) {
        Utilisateur current = SessionManager.getInstance().getCurrentUser();
        return openDefaultForUser(stage, current, resourceAnchor);
    }

    public static boolean ensureAdminAccess(Stage stage, Class<?> resourceAnchor) {
        Utilisateur current = SessionManager.getInstance().getCurrentUser();
        if (current == null) {
            switchTo(stage, LOGIN_FXML, LOGIN_TITLE, resourceAnchor);
            return false;
        }

        if (!isAdmin(current)) {
            openDefaultForUser(stage, current, resourceAnchor);
            return false;
        }

        return true;
    }
}