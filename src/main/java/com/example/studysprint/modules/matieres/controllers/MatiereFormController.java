package com.example.studysprint.modules.matieres.controllers;

import com.example.studysprint.modules.groupes.utils.GroupUiUtils;
import com.example.studysprint.modules.matieres.models.Matiere;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Window;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Optional;

public class MatiereFormController {

    @FXML private TextField nameField;
    @FXML private TextField codeField;
    @FXML private TextArea descriptionField;

    public static Matiere showDialog(Matiere existing, Window owner) {
        try {
            FXMLLoader loader = new FXMLLoader(MatiereFormController.class.getResource("/fxml/matieres/MatiereFormView.fxml"));
            Parent content = loader.load();
            MatiereFormController controller = loader.getController();
            if (existing != null) {
                controller.nameField.setText(existing.getName());
                controller.codeField.setText(existing.getCode());
                controller.descriptionField.setText(existing.getDescription());
            }

            Dialog<ButtonType> dialog = new Dialog<>();
            if (owner != null) dialog.initOwner(owner);
            dialog.setTitle(existing == null ? "Ajouter une matière" : "Modifier la matière");
            DialogPane pane = dialog.getDialogPane();
            pane.setContent(content);
            ButtonType saveType = new ButtonType(existing == null ? "Ajouter" : "Enregistrer", ButtonBar.ButtonData.OK_DONE);
            pane.getButtonTypes().addAll(saveType, ButtonType.CANCEL);
            GroupUiUtils.applyDialogStyle(pane, MatiereFormController.class);
            Button saveButton = (Button) pane.lookupButton(saveType);
            if (saveButton != null) {
                saveButton.setDisable(!controller.isFormValid(false));
                controller.nameField.textProperty().addListener((obs, old, val) -> saveButton.setDisable(!controller.isFormValid(false)));
                controller.codeField.textProperty().addListener((obs, old, val) -> saveButton.setDisable(!controller.isFormValid(false)));
                controller.descriptionField.textProperty().addListener((obs, old, val) -> saveButton.setDisable(!controller.isFormValid(false)));

                saveButton.addEventFilter(ActionEvent.ACTION, e -> {
                    if (!controller.isFormValid(true)) e.consume();
                });
            }
            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isEmpty() || result.get() != saveType) return null;
            return controller.buildMatiere(existing);
        } catch (IOException e) {
            GroupUiUtils.showError(owner, MatiereFormController.class, "Erreur", "Impossible d'ouvrir le formulaire.", e.getMessage());
            return null;
        }
    }

    private boolean isFormValid(boolean showWarning) {
        String name = nameField.getText();
        String code = codeField.getText();
        String desc = descriptionField.getText();

        if (name == null || name.trim().isEmpty()) {
            if (showWarning) GroupUiUtils.showWarning(nameField.getScene().getWindow(), MatiereFormController.class, "Validation", "Le nom de la matière est obligatoire.");
            return false;
        }
        if (name.trim().length() < 2) {
            if (showWarning) GroupUiUtils.showWarning(nameField.getScene().getWindow(), MatiereFormController.class, "Validation", "Le nom doit contenir au moins 2 caractères.");
            return false;
        }
        if (name.trim().length() > 100) {
            if (showWarning) GroupUiUtils.showWarning(nameField.getScene().getWindow(), MatiereFormController.class, "Validation", "Le nom est trop long (maximum 100 caractères).");
            return false;
        }

        if (code == null || code.trim().isEmpty()) {
            if (showWarning) GroupUiUtils.showWarning(codeField.getScene().getWindow(), MatiereFormController.class, "Validation", "Le code de la matière est obligatoire.");
            return false;
        }
        if (code.trim().length() > 20) {
            if (showWarning) GroupUiUtils.showWarning(codeField.getScene().getWindow(), MatiereFormController.class, "Validation", "Le code est trop long (maximum 20 caractères).");
            return false;
        }
        if (code.trim().contains(" ")) {
            if (showWarning) GroupUiUtils.showWarning(codeField.getScene().getWindow(), MatiereFormController.class, "Validation", "Le code ne doit pas contenir d'espaces.");
            return false;
        }

        if (desc == null || desc.trim().isEmpty()) {
            if (showWarning) GroupUiUtils.showWarning(descriptionField.getScene().getWindow(), MatiereFormController.class, "Validation", "La description est obligatoire.");
            return false;
        }
        if (desc.trim().length() < 5) {
            if (showWarning) GroupUiUtils.showWarning(descriptionField.getScene().getWindow(), MatiereFormController.class, "Validation", "La description doit contenir au moins 5 caractères.");
            return false;
        }
        if (desc.length() > 500) {
            if (showWarning) GroupUiUtils.showWarning(descriptionField.getScene().getWindow(), MatiereFormController.class, "Validation", "La description est trop longue (maximum 500 caractères).");
            return false;
        }

        return true;
    }

    private Matiere buildMatiere(Matiere existing) {
        Matiere m = existing == null ? new Matiere() : existing;
        m.setName(nameField.getText().trim());
        m.setCode(codeField.getText().trim());
        m.setDescription(descriptionField.getText());
        if (m.getCreatedAt() == null) m.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        if (m.getCreatedById() == null) m.setCreatedById(1);
        return m;
    }
}