package com.example.studysprint.modules.matieres.controllers;

import com.example.studysprint.modules.groupes.utils.GroupUiUtils;
import com.example.studysprint.modules.matieres.models.Chapitre;
import com.example.studysprint.modules.matieres.models.Matiere;
import com.example.studysprint.modules.matieres.services.ChapitreService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Optional;

public class ChapitreListController {

    @FXML private Label matiereNomLabel;
    @FXML private Button ajouterChapitreButton;
    @FXML private VBox chapitresListBox;

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
        ajouterChapitreButton.setGraphic(GroupUiUtils.icon("fas-plus", "create-btn-icon"));
        ajouterChapitreButton.setOnAction(e -> onAjouterChapitre());
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

            actions.getChildren().addAll(editBtn, deleteBtn);
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
}