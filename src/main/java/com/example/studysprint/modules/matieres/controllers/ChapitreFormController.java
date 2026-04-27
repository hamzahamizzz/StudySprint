package com.example.studysprint.modules.matieres.controllers;

import com.example.studysprint.modules.groupes.utils.GroupUiUtils;
import com.example.studysprint.modules.matieres.models.Chapitre;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Window;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Optional;

public class ChapitreFormController {

    @FXML private TextField titleField;
    @FXML private TextField orderNoField;
    @FXML private TextArea summaryArea;
    @FXML private TextArea contentArea;
    @FXML private TextField attachmentUrlField;
    // AI fields are not in the simple form (they can be added later)

    public static Chapitre showDialog(Chapitre existing, Integer subjectId, Window owner) {
        try {
            FXMLLoader loader = new FXMLLoader(ChapitreFormController.class.getResource("/fxml/matieres/ChapitreFormView.fxml"));
            Parent content = loader.load();
            ChapitreFormController controller = loader.getController();
            if (existing != null) {
                controller.titleField.setText(existing.getTitle());
                if (existing.getOrderNo() != null) controller.orderNoField.setText(String.valueOf(existing.getOrderNo()));
                controller.summaryArea.setText(existing.getSummary());
                controller.contentArea.setText(existing.getContent());
                controller.attachmentUrlField.setText(existing.getAttachmentUrl());
            }

            Dialog<ButtonType> dialog = new Dialog<>();
            if (owner != null) dialog.initOwner(owner);
            dialog.setTitle(existing == null ? "Ajouter un chapitre" : "Modifier le chapitre");
            DialogPane pane = dialog.getDialogPane();
            pane.setContent(content);
            ButtonType saveType = new ButtonType(existing == null ? "Ajouter" : "Enregistrer", ButtonBar.ButtonData.OK_DONE);
            pane.getButtonTypes().addAll(saveType, ButtonType.CANCEL);
            GroupUiUtils.applyDialogStyle(pane, ChapitreFormController.class);
            Button saveButton = (Button) pane.lookupButton(saveType);
            if (saveButton != null) {
                saveButton.setDisable(!controller.isFormValid(false));
                controller.titleField.textProperty().addListener((obs, old, val) -> saveButton.setDisable(!controller.isFormValid(false)));
                controller.orderNoField.textProperty().addListener((obs, old, val) -> saveButton.setDisable(!controller.isFormValid(false)));
                controller.summaryArea.textProperty().addListener((obs, old, val) -> saveButton.setDisable(!controller.isFormValid(false)));
                controller.contentArea.textProperty().addListener((obs, old, val) -> saveButton.setDisable(!controller.isFormValid(false)));

                saveButton.addEventFilter(ActionEvent.ACTION, e -> {
                    if (!controller.isFormValid(true)) e.consume();
                });
            }
            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isEmpty() || result.get() != saveType) return null;
            return controller.buildChapitre(existing, subjectId);
        } catch (IOException e) {
            GroupUiUtils.showError(owner, ChapitreFormController.class, "Erreur", "Impossible d'ouvrir le formulaire.", e.getMessage());
            return null;
        }
    }

    private boolean isFormValid(boolean showWarning) {
        String title = titleField.getText();
        String orderText = orderNoField.getText();
        String summary = summaryArea.getText();
        String content = contentArea.getText();
        
        if (title == null || title.trim().isEmpty()) {
            if (showWarning) GroupUiUtils.showWarning(titleField.getScene().getWindow(), ChapitreFormController.class, "Validation", "Le titre du chapitre est obligatoire.");
            return false;
        }
        if (title.trim().length() < 2) {
            if (showWarning) GroupUiUtils.showWarning(titleField.getScene().getWindow(), ChapitreFormController.class, "Validation", "Le titre doit contenir au moins 2 caractères.");
            return false;
        }
        if (title.trim().length() > 150) {
            if (showWarning) GroupUiUtils.showWarning(titleField.getScene().getWindow(), ChapitreFormController.class, "Validation", "Le titre est trop long (maximum 150 caractères).");
            return false;
        }
        
        if (orderText == null || orderText.trim().isEmpty()) {
            if (showWarning) GroupUiUtils.showWarning(orderNoField.getScene().getWindow(), ChapitreFormController.class, "Validation", "L'ordre du chapitre est obligatoire.");
            return false;
        }
        try {
            int order = Integer.parseInt(orderText.trim());
            if (order < 0) {
                if (showWarning) GroupUiUtils.showWarning(orderNoField.getScene().getWindow(), ChapitreFormController.class, "Validation", "L'ordre ne peut pas être négatif.");
                return false;
            }
        } catch (NumberFormatException e) {
            if (showWarning) GroupUiUtils.showWarning(orderNoField.getScene().getWindow(), ChapitreFormController.class, "Validation", "L'ordre doit être un nombre entier valide.");
            return false;
        }
        
        if (summary == null || summary.trim().isEmpty()) {
            if (showWarning) GroupUiUtils.showWarning(summaryArea.getScene().getWindow(), ChapitreFormController.class, "Validation", "Le résumé est obligatoire.");
            return false;
        }
        if (summary.length() > 1000) {
            if (showWarning) GroupUiUtils.showWarning(summaryArea.getScene().getWindow(), ChapitreFormController.class, "Validation", "Le résumé est trop long (maximum 1000 caractères).");
            return false;
        }
        
        if (content == null || content.trim().isEmpty()) {
            if (showWarning) GroupUiUtils.showWarning(contentArea.getScene().getWindow(), ChapitreFormController.class, "Validation", "Le contenu est obligatoire.");
            return false;
        }
        if (content.length() > 50000) {
            if (showWarning) GroupUiUtils.showWarning(contentArea.getScene().getWindow(), ChapitreFormController.class, "Validation", "Le contenu est trop long.");
            return false;
        }
        
        return true;
    }

    private Chapitre buildChapitre(Chapitre existing, Integer subjectId) {
        Chapitre c = existing == null ? new Chapitre() : existing;
        c.setTitle(titleField.getText().trim());
        String orderText = orderNoField.getText().trim();
        c.setOrderNo(orderText.isEmpty() ? 0 : Integer.parseInt(orderText));
        c.setSummary(summaryArea.getText());
        c.setContent(contentArea.getText());
        c.setAttachmentUrl(attachmentUrlField.getText().trim());
        if (subjectId != null) c.setSubjectId(subjectId);
        if (c.getCreatedAt() == null) c.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        if (c.getCreatedById() == null) c.setCreatedById(1);
        // AI fields remain null for now
        return c;
    }
}