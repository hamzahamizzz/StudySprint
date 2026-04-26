package com.example.studysprint.modules.groupes.controllers;

import com.example.studysprint.modules.groupes.models.StudyGroup;
import com.example.studysprint.modules.groupes.utils.GroupUiUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Window;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import com.example.studysprint.utils.SessionManager;

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

    // Open the modal form and return the saved group, or null if the user cancels.
    public static StudyGroup showDialog(StudyGroup existing, Window owner) {
        try {
            FXMLLoader loader = new FXMLLoader(GroupFormController.class.getResource("/fxml/groupes/GroupFormView.fxml"));
            Parent content = loader.load();
            GroupFormController controller = loader.getController();

            controller.prefill(existing);

            Dialog<ButtonType> dialog = new Dialog<>();
            if (owner != null) {
                dialog.initOwner(owner);
            }
            dialog.setTitle(existing == null ? "Ajouter un groupe" : "Modifier le groupe");
            dialog.setHeaderText(existing == null ? "Remplissez les informations du nouveau groupe" : "Mettez a jour les informations du groupe");

            DialogPane pane = dialog.getDialogPane();
            pane.setContent(content);

            ButtonType saveType = new ButtonType(existing == null ? "Ajouter" : "Enregistrer", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
            pane.getButtonTypes().addAll(saveType, cancelType);
            GroupUiUtils.applyDialogStyle(pane, GroupFormController.class);

            Button saveButton = (Button) pane.lookupButton(saveType);
            if (saveButton != null) {
                saveButton.getStyleClass().add("primary-btn");
                saveButton.setGraphic(GroupUiUtils.icon(existing == null ? "fas-plus" : "fas-save", "detail-dialog-icon"));
                saveButton.addEventFilter(ActionEvent.ACTION, event -> {
                    if (!controller.validateForm()) {
                        event.consume();
                    }
                });
            }

            Button cancelButton = (Button) pane.lookupButton(cancelType);
            if (cancelButton != null) {
                cancelButton.setGraphic(GroupUiUtils.icon("fas-times", "detail-dialog-icon"));
            }

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isEmpty() || result.get() != saveType) {
                return null;
            }

            return controller.buildGroupFromForm(existing);
        } catch (IOException e) {
            GroupUiUtils.showError(owner, GroupFormController.class,
                    "Formulaire indisponible",
                    "Impossible d'ouvrir le formulaire pour le moment.",
                    e.getMessage());
            return null;
        }
    }

    // Fill the form when editing an existing group.
    private void prefill(StudyGroup existing) {
        if (existing != null) {
            nameField.setText(existing.getName());
            descriptionField.setText(existing.getDescription());
            privacyChoice.setValue(existing.getPrivacy());
            subjectField.setText(existing.getSubject());
        } else {
            privacyChoice.setValue("public");
        }
    }

    // Block submission when required fields are empty.
    private boolean validateForm() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        String subject = subjectField.getText() == null ? "" : subjectField.getText().trim();

        if (name.isBlank() || subject.isBlank()) {
            GroupUiUtils.showWarning(nameField.getScene() == null ? null : nameField.getScene().getWindow(),
                    GroupFormController.class,
                    "Validation",
                    "Le nom et la matiere sont obligatoires.");
            return false;
        }

        return true;
    }

    // Build the StudyGroup object from the form values.
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
            var currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                group.setCreatedById(currentUser.getId());
            }
        }

        return group;
    }
}
