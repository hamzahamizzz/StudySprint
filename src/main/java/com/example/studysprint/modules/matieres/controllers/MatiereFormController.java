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
                saveButton.addEventFilter(ActionEvent.ACTION, e -> {
                    if (!controller.validateForm()) e.consume();
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

    private boolean validateForm() {
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            GroupUiUtils.showWarning(nameField.getScene().getWindow(), MatiereFormController.class, "Validation", "Le nom est obligatoire.");
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