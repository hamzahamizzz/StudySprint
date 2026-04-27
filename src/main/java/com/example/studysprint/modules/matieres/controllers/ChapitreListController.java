package com.example.studysprint.modules.matieres.controllers;

import com.example.studysprint.modules.groupes.utils.GroupUiUtils;
import com.example.studysprint.modules.matieres.models.Chapitre;
import com.example.studysprint.modules.matieres.models.Matiere;
import com.example.studysprint.modules.matieres.services.ChapitreService;
import com.example.studysprint.modules.matieres.services.QrCodeService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;

public class ChapitreListController {

    @FXML private Label matiereNomLabel;
    @FXML private Button ajouterChapitreButton;
    @FXML private VBox chapitresListBox;
    @FXML private Button resumePdfButton;
    @FXML private Button retourButton;


    private Matiere currentMatiere;
    private final ChapitreService chapitreService = new ChapitreService();
    private final ObservableList<Chapitre> chapitres = FXCollections.observableArrayList();



    public void setMatiere(Matiere matiere) {
        this.currentMatiere = matiere;
        if (matiere != null) {
            matiereNomLabel.setText("Chapitres de : " + matiere.getName() + (matiere.getCode() != null ? " (" + matiere.getCode() + ")" : ""));
            loadChapitres();
        }
    }

    @FXML
    private void initialize() {
        if (retourButton != null) {
            retourButton.setGraphic(GroupUiUtils.icon("fas-arrow-left", "create-btn-icon"));
            retourButton.setOnAction(e -> onRetour());
        }
        ajouterChapitreButton.setGraphic(GroupUiUtils.icon("fas-plus", "create-btn-icon"));
        ajouterChapitreButton.setOnAction(e -> onAjouterChapitre());
        resumePdfButton.setGraphic(GroupUiUtils.icon("fas-file-pdf", "create-btn-icon"));
        resumePdfButton.setOnAction(e -> onResumePdf());
    }

    private void onRetour() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/matieres/MatiereListView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) retourButton.getScene().getWindow();
            GroupUiUtils.switchScene(stage, root, "StudySprint - Matières");
        } catch (IOException e) {
            GroupUiUtils.showError(retourButton.getScene().getWindow(), getClass(), "Navigation impossible", "Impossible de retourner aux matières.", e.getMessage());
        }
    }

    private void loadChapitres() {
        if (currentMatiere == null) return;
        chapitres.setAll(chapitreService.getBySubjectId(currentMatiere.getId()));
        renderChapitres();
    }

    private void renderChapitres() {
        chapitresListBox.getChildren().clear();
        if (chapitres.isEmpty()) {
            Label empty = new Label("Aucun chapitre pour cette matière.");
            empty.getStyleClass().add("detail-empty-post");
            chapitresListBox.getChildren().add(empty);
            return;
        }
        for (Chapitre c : chapitres) {
            VBox card = new VBox(8);
            card.getStyleClass().add("detail-post-card");

            Label title = new Label((c.getOrderNo() != null && c.getOrderNo() > 0 ? c.getOrderNo() + ". " : "") + c.getTitle());
            title.getStyleClass().add("detail-post-title");

            Label summary = new Label(c.getSummary() == null || c.getSummary().isBlank() ? "Pas de résumé" : c.getSummary());
            summary.getStyleClass().add("detail-post-body");
            summary.setWrapText(true);

            HBox actions = new HBox(10);
            Button editBtn = new Button("Modifier");
            editBtn.getStyleClass().addAll("compose-cancel-btn");
            editBtn.setOnAction(e -> onEditChapitre(c));

            Button deleteBtn = new Button("Supprimer");
            deleteBtn.getStyleClass().addAll("detail-danger-btn");
            deleteBtn.setOnAction(e -> onDeleteChapitre(c));

            Button qrBtn = new Button("QR Code");
            qrBtn.getStyleClass().addAll("compose-cancel-btn");
            qrBtn.setOnAction(e -> onGenererQrCode(c));

            actions.getChildren().addAll(editBtn, deleteBtn, qrBtn);
            
            card.getChildren().addAll(title, summary, actions);
            chapitresListBox.getChildren().add(card);
        }
    }

    private void onAjouterChapitre() {
        if (currentMatiere == null) return;
        Chapitre newChap = ChapitreFormController.showDialog(null, currentMatiere.getId(), ajouterChapitreButton.getScene().getWindow());
        if (newChap != null) {
            chapitreService.add(newChap);
            loadChapitres();
            showSuccess("Chapitre ajouté", "Le chapitre a été créé.");
        }
    }

    private void onEditChapitre(Chapitre chapitre) {
        Chapitre updated = ChapitreFormController.showDialog(chapitre, currentMatiere.getId(), ajouterChapitreButton.getScene().getWindow());
        if (updated != null) {
            chapitreService.update(updated);
            loadChapitres();
            showSuccess("Chapitre modifié", "Les modifications ont été enregistrées.");
        }
    }

    private void onDeleteChapitre(Chapitre chapitre) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer le chapitre");
        confirm.setHeaderText("Confirmer la suppression");
        confirm.setContentText("Supprimer le chapitre \"" + chapitre.getTitle() + "\" ?");
        ButtonType oui = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        confirm.getButtonTypes().setAll(oui, ButtonType.CANCEL);
        GroupUiUtils.applyDialogStyle(confirm.getDialogPane(), ChapitreListController.class);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == oui) {
            chapitreService.delete(chapitre.getId());
            loadChapitres();
            showSuccess("Chapitre supprimé", "Le chapitre a été supprimé.");
        }
    }

    private void showSuccess(String header, String content) {
        GroupUiUtils.showSuccess(chapitresListBox.getScene().getWindow(), ChapitreListController.class, header, content);
    }

    private void onResumePdf() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/matieres/ChapitreSelectionView.fxml"));
            Parent root = loader.load();
            ChapitreSelectionController controller = loader.getController();
            controller.setMatiere(currentMatiere);
            Stage stage = (Stage) chapitresListBox.getScene().getWindow();
            GroupUiUtils.switchScene(stage, root, "Résumé PDF - " + currentMatiere.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void onGenererQrCode(Chapitre chapitre) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le QR code");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG images", "*.png"));
        fileChooser.setInitialFileName("qrcode_chapitre_" + chapitre.getId() + ".png");
        File file = fileChooser.showSaveDialog(ajouterChapitreButton.getScene().getWindow());
        if (file == null) return;
        try {
            // Appel à l'API locale pour récupérer l'image (ou générer directement)
            // On utilise le service local QrCodeService
            String url = "http://localhost:4567/chapitre/" + chapitre.getId();
            byte[] qrPng = new QrCodeService().genererQrCode(url, 300, 300);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(qrPng);
            }
            GroupUiUtils.showSuccess(null, getClass(), "QR code généré", "Enregistré sous " + file.getName());
        } catch (Exception ex) {
            GroupUiUtils.showError(null, getClass(), "Erreur", ex.getMessage(), null);
        }
    }

}