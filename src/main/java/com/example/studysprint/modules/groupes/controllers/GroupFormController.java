package com.example.studysprint.modules.groupes.controllers;

import com.example.studysprint.modules.groupes.models.StudyGroup;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Optional;

public class GroupFormController {

    @FXML
    private TextField nameField;
    @FXML
    private TextArea descriptionField;
    @FXML
    private ChoiceBox<String> privacyChoice;
    @FXML
    private TextField subjectField;

    @FXML
    public void initialize() {
        // Fields are injected from FXML
    }

    // Display form dialog and return edited StudyGroup or null if cancelled
    public StudyGroup showDialog(StudyGroup existing) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/groupes/GroupFormView.fxml"));
            Parent content = loader.load();
            GroupFormController controller = loader.getController();

            // Populate existing data if editing
            if (existing != null) {
                controller.nameField.setText(existing.getName());
                controller.descriptionField.setText(existing.getDescription());
                controller.privacyChoice.setValue(existing.getPrivacy());
                controller.subjectField.setText(existing.getSubject());
            } else {
                controller.privacyChoice.setValue("public");
            }

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(existing == null ? "Ajouter un groupe" : "Modifier le groupe");
            dialog.setHeaderText(existing == null ? "Remplissez les informations du nouveau groupe" : "Mettez a jour les informations du groupe");

            DialogPane pane = dialog.getDialogPane();
            pane.setContent(content);

            ButtonType saveType = new ButtonType(existing == null ? "➕ Ajouter" : "💾 Enregistrer");
            ButtonType cancelType = new ButtonType("✖ Annuler");
            pane.getButtonTypes().addAll(saveType, cancelType);
            applyDialogStyle(pane);

            Button saveButton = (Button) pane.lookupButton(saveType);
            if (saveButton != null) {
                saveButton.getStyleClass().add("primary-btn");
                saveButton.addEventFilter(ActionEvent.ACTION, event -> {
                    if (!controller.validateForm()) {
                        event.consume();
                    }
                });
            }

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isEmpty() || result.get() != saveType) {
                return null;
            }

            return controller.buildGroupFromForm(existing);
        } catch (IOException e) {
            showError("Formulaire indisponible", "Impossible d'ouvrir le formulaire pour le moment.", e.getMessage());
            return null;
        }
    }

    private boolean validateForm() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        String subject = subjectField.getText() == null ? "" : subjectField.getText().trim();

        if (name.isBlank() || subject.isBlank()) {
            showWarning("Validation", "Le nom et la matiere sont obligatoires.");
            return false;
        }

        return true;
    }

    private StudyGroup buildGroupFromForm(StudyGroup existing) {
        StudyGroup group = existing == null ? new StudyGroup() : existing;
        group.setName(nameField.getText().trim());
        group.setDescription(descriptionField.getText());
        group.setPrivacy(privacyChoice.getValue());
        group.setSubject(subjectField.getText().trim());

        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (group.getCreatedAt() == null) {
            group.setCreatedAt(now);
        }
        group.setUpdatedAt(now);
        group.setLastActivity(now);

        if (group.getCreatedById() == null) {
            group.setCreatedById(1);
        }

        return group;
    }

    private void showWarning(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Attention");
        alert.setHeaderText(header);
        alert.setContentText(content);
        applyDialogStyle(alert.getDialogPane());
        alert.showAndWait();
    }

    private void showError(String header, String content, String details) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(header);
        alert.setContentText(content + (details == null ? "" : "\n" + details));
        applyDialogStyle(alert.getDialogPane());
        alert.showAndWait();
    }

    private void applyDialogStyle(DialogPane pane) {
        var cssUrl = getClass().getResource("/styles/groupes-light-blue.css");
        if (cssUrl == null) {
            return;
        }
        String stylesheet = cssUrl.toExternalForm();
        if (!pane.getStylesheets().contains(stylesheet)) {
            pane.getStylesheets().add(stylesheet);
        }
    }
}
