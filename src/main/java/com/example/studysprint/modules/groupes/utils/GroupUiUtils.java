package com.example.studysprint.modules.groupes.utils;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;
import org.controlsfx.control.Notifications;
import javafx.geometry.Pos;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;

public final class GroupUiUtils {

    private GroupUiUtils() {
    }

    public static void switchScene(Stage stage, Parent root, String title) {
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

    public static void applyDialogStyle(DialogPane pane, Class<?> resourceAnchor) {
        if (pane == null || resourceAnchor == null) return;
        var cssUrl = resourceAnchor.getResource("/styles/main.css");
        if (cssUrl == null) return;
        String stylesheet = cssUrl.toExternalForm();
        if (!pane.getStylesheets().contains(stylesheet)) {
            pane.getStylesheets().add(stylesheet);
        }
    }

    public static void showSuccess(Window owner, Class<?> resourceAnchor, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        if (owner != null) alert.initOwner(owner);
        alert.setTitle("Succès");
        alert.setHeaderText(header);
        alert.setContentText(content);
        applyDialogStyle(alert.getDialogPane(), resourceAnchor);
        alert.showAndWait();
    }

    public static void showWarning(Window owner, Class<?> resourceAnchor, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        if (owner != null) alert.initOwner(owner);
        alert.setTitle("Attention");
        alert.setHeaderText(header);
        alert.setContentText(content);
        applyDialogStyle(alert.getDialogPane(), resourceAnchor);
        alert.showAndWait();
    }

    public static void showError(Window owner, Class<?> resourceAnchor, String header, String content, String details) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        if (owner != null) alert.initOwner(owner);
        alert.setTitle("Erreur");
        alert.setHeaderText(header);
        alert.setContentText(content + (details == null || details.isBlank() ? "" : "\n\nDétails: " + details));
        applyDialogStyle(alert.getDialogPane(), resourceAnchor);
        alert.showAndWait();
    }

    // --- ControlsFX Notifications with Custom Icons ---

    public static void showNotifSuccess(String title, String text) {
        FontIcon icon = icon("fas-check-circle", null);
        icon.setIconSize(24);
        icon.setIconColor(javafx.scene.paint.Color.web("#10b981"));
        
        Notifications.create()
            .title(title)
            .text(text)
            .graphic(icon)
            .hideAfter(javafx.util.Duration.seconds(4))
            .position(Pos.BOTTOM_RIGHT)
            .show();
    }

    public static void showNotifInfo(String title, String text) {
        FontIcon icon = icon("fas-info-circle", null);
        icon.setIconSize(24);
        icon.setIconColor(javafx.scene.paint.Color.web("#3b82f6"));

        Notifications.create()
            .title(title)
            .text(text)
            .graphic(icon)
            .hideAfter(javafx.util.Duration.seconds(4))
            .position(Pos.BOTTOM_RIGHT)
            .show();
    }

    public static void showNotifWarning(String title, String text) {
        FontIcon icon = icon("fas-exclamation-triangle", null);
        icon.setIconSize(24);
        icon.setIconColor(javafx.scene.paint.Color.web("#f59e0b"));

        Notifications.create()
            .title(title)
            .text(text)
            .graphic(icon)
            .hideAfter(javafx.util.Duration.seconds(5))
            .position(Pos.BOTTOM_RIGHT)
            .show();
    }

    public static void showNotifError(String title, String text) {
        FontIcon icon = icon("fas-times-circle", null);
        icon.setIconSize(24);
        icon.setIconColor(javafx.scene.paint.Color.web("#ef4444"));

        Notifications.create()
            .title(title)
            .text(text)
            .graphic(icon)
            .hideAfter(javafx.util.Duration.seconds(6))
            .position(Pos.BOTTOM_RIGHT)
            .show();
    }

    public static FontIcon icon(String literal, String styleClass) {
        FontIcon icon = new FontIcon(literal);
        icon.getStyleClass().add("detail-icon-glyph");
        if (styleClass != null && !styleClass.isBlank()) {
            icon.getStyleClass().add(styleClass);
        }
        return icon;
    }

    public static String privacyIconLiteral(String privacy) {
        return "private".equalsIgnoreCase(privacy) ? "fas-lock" : "fas-globe";
    }

    public static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    public static String initial(String value) {
        if (value == null || value.isBlank()) return "G";
        return value.substring(0, 1).toUpperCase();
    }

    public static String formatRelativeTime(Timestamp timestamp) {
        if (timestamp == null) return "inconnue";
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime value = timestamp.toLocalDateTime();
        Duration delta = Duration.between(value, now);
        long seconds = Math.max(0, delta.getSeconds());

        if (seconds < 60) return "à l'instant";
        long minutes = seconds / 60;
        if (minutes < 60) return "il y a " + minutes + " min";
        long hours = minutes / 60;
        if (hours < 24) return "il y a " + hours + " h";
        long days = hours / 24;
        if (days < 7) return "il y a " + days + " j";
        long weeks = days / 7;
        if (weeks < 5) return "il y a " + weeks + " sem";
        long months = days / 30;
        if (months < 12) return "il y a " + months + " mois";
        long years = days / 365;
        return "il y a " + years + " ans";
    }
}
