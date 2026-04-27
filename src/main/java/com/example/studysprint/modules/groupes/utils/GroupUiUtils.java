package com.example.studysprint.modules.groupes.utils;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;

public final class GroupUiUtils {

    // Utility class (static helpers only).
    private GroupUiUtils() {
    }

    // Switch scene while keeping the current window size/state.
    public static void switchScene(Stage stage, Parent root, String title) {
        Scene currentScene = stage.getScene();
        if (currentScene != null) {
            currentScene.setRoot(root);
        } else {
            stage.setScene(new Scene(root));
        }
        stage.setTitle(title);
    }

    // Apply the shared CSS stylesheet to a dialog.
    public static void applyDialogStyle(DialogPane pane, Class<?> resourceAnchor) {
        if (pane == null || resourceAnchor == null) {
            return;
        }
        var cssUrl = resourceAnchor.getResource("/styles/groupes-light-blue.css");
        if (cssUrl == null) {
            return;
        }
        String stylesheet = cssUrl.toExternalForm();
        if (!pane.getStylesheets().contains(stylesheet)) {
            pane.getStylesheets().add(stylesheet);
        }
    }

    // Show an information alert with the shared stylesheet.
    public static void showSuccess(Window owner, Class<?> resourceAnchor, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle("Succes");
        alert.setHeaderText(header);
        alert.setContentText(content);
        applyDialogStyle(alert.getDialogPane(), resourceAnchor);
        alert.showAndWait();
    }

    // Show a warning alert with the shared stylesheet.
    public static void showWarning(Window owner, Class<?> resourceAnchor, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle("Attention");
        alert.setHeaderText(header);
        alert.setContentText(content);
        applyDialogStyle(alert.getDialogPane(), resourceAnchor);
        alert.showAndWait();
    }

    // Show an error alert with optional details and the shared stylesheet.
    public static void showError(Window owner, Class<?> resourceAnchor, String header, String content, String details) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle("Erreur");
        alert.setHeaderText(header);
        alert.setContentText(content + (details == null || details.isBlank() ? "" : "\n\nDetails: " + details));
        applyDialogStyle(alert.getDialogPane(), resourceAnchor);
        alert.showAndWait();
    }

    // Create a FontIcon with the standard glyph + optional extra style class.
    public static FontIcon icon(String literal, String styleClass) {
        FontIcon icon = new FontIcon(literal);
        icon.getStyleClass().add("detail-icon-glyph");
        if (styleClass != null && !styleClass.isBlank()) {
            icon.getStyleClass().add(styleClass);
        }
        return icon;
    }

    // Map the group privacy value to an icon literal.
    public static String privacyIconLiteral(String privacy) {
        if (privacy == null) {
            return "fas-globe";
        }
        return "private".equalsIgnoreCase(privacy) ? "fas-lock" : "fas-globe";
    }

    // Convert null strings to empty strings.
    public static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    // Return the first uppercase letter of a label (used for avatars).
    public static String initial(String value) {
        if (value == null || value.isBlank()) {
            return "G";
        }
        return value.substring(0, 1).toUpperCase();
    }

    // Format timestamp as relative French text.
    public static String formatRelativeTime(Timestamp timestamp) {
        if (timestamp == null) {
            return "inconnue";
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime value = timestamp.toLocalDateTime();
        Duration delta = Duration.between(value, now);
        long seconds = Math.max(0, delta.getSeconds());

        if (seconds < 60) {
            return "a l'instant";
        }

        long minutes = seconds / 60;
        if (minutes < 60) {
            return "il y a " + minutes + " min";
        }

        long hours = minutes / 60;
        if (hours < 24) {
            return "il y a " + hours + " h";
        }

        long days = hours / 24;
        if (days < 7) {
            return "il y a " + days + " j";
        }

        long weeks = days / 7;
        if (weeks < 5) {
            return "il y a " + weeks + " sem";
        }

        long months = days / 30;
        if (months < 12) {
            return "il y a " + months + " mois";
        }

        long years = days / 365;
        return "il y a " + years + " ans";
    }
}
