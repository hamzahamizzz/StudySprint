package com.example.studysprint.modules.matieres.controllers;

import com.example.studysprint.modules.groupes.utils.GroupUiUtils;
import com.example.studysprint.modules.matieres.models.Chapitre;
import com.example.studysprint.modules.matieres.models.Matiere;
import com.example.studysprint.modules.matieres.services.ChapitreService;
import com.example.studysprint.modules.matieres.services.PdfService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ChapitreSelectionController {

    @FXML private Label matiereLabel;
    @FXML private VBox chapitresCheckBoxContainer;
    @FXML private Button genererPdfButton;
    @FXML private Button retourButton;

    private Matiere currentMatiere;
    private List<Chapitre> allChapitres;
    private final ChapitreService chapitreService = new ChapitreService();
    private final PdfService pdfService = new PdfService();

    public void setMatiere(Matiere matiere) {
        this.currentMatiere = matiere;
        matiereLabel.setText("Sélectionnez les chapitres à résumer : " + matiere.getName());
        loadChapitres();
    }

    @FXML
    private void initialize() {
        if (retourButton != null) {
            retourButton.setGraphic(GroupUiUtils.icon("fas-arrow-left", "create-btn-icon"));
            retourButton.setOnAction(e -> onRetour());
        }
    }

    private void onRetour() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/matieres/ChapitreListView.fxml"));
            javafx.scene.Parent root = loader.load();
            ChapitreListController controller = loader.getController();
            controller.setMatiere(currentMatiere);
            Stage stage = (Stage) retourButton.getScene().getWindow();
            GroupUiUtils.switchScene(stage, root, "Chapitres - " + currentMatiere.getName());
        } catch (Exception e) {
            e.printStackTrace();
            GroupUiUtils.showError(retourButton.getScene().getWindow(), getClass(), "Navigation impossible", "Impossible de retourner aux chapitres.", e.getMessage());
        }
    }

    private void loadChapitres() {
        allChapitres = chapitreService.getBySubjectId(currentMatiere.getId());
        chapitresCheckBoxContainer.getChildren().clear();
        for (Chapitre c : allChapitres) {
            CheckBox cb = new CheckBox((c.getOrderNo() != null ? c.getOrderNo() + ". " : "") + c.getTitle());
            cb.setUserData(c);
            chapitresCheckBoxContainer.getChildren().add(cb);
        }
    }

    @FXML
    private void onGenererPdf() {
        List<Chapitre> selected = new ArrayList<>();
        for (var node : chapitresCheckBoxContainer.getChildren()) {
            if (node instanceof CheckBox cb && cb.isSelected()) {
                selected.add((Chapitre) cb.getUserData());
            }
        }
        if (selected.isEmpty()) {
            GroupUiUtils.showWarning(genererPdfButton.getScene().getWindow(), getClass(), "Aucune sélection", "Veuillez sélectionner au moins un chapitre.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        fileChooser.setInitialFileName("resume_" + currentMatiere.getName() + ".pdf");
        File file = fileChooser.showSaveDialog(genererPdfButton.getScene().getWindow());
        if (file == null) return;

        try {
            byte[] pdfData = pdfService.genererResumeChapitres(selected, currentMatiere.getName());
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(pdfData);
            }
            GroupUiUtils.showSuccess(genererPdfButton.getScene().getWindow(), getClass(), "PDF généré", "Le fichier a été enregistré : " + file.getName());
        } catch (Exception e) {
            GroupUiUtils.showError(genererPdfButton.getScene().getWindow(), getClass(), "Erreur", "Impossible de générer le PDF.", e.getMessage());
        }
    }
}