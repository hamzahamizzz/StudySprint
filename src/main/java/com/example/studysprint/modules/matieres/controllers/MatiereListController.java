package com.example.studysprint.modules.matieres.controllers;

import com.example.studysprint.modules.groupes.utils.GroupUiUtils;
import com.example.studysprint.modules.matieres.models.Matiere;
import com.example.studysprint.modules.matieres.services.MatiereService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class MatiereListController {

    @FXML private Button ajouterButton;
    @FXML private TextField searchField;
    @FXML private FlowPane matieresCardsPane;
    @FXML private Label matieresCountLabel;

    private final MatiereService matiereService = new MatiereService();
    private final ObservableList<Matiere> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        ajouterButton.setGraphic(GroupUiUtils.icon("fas-plus", "create-btn-icon"));
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());
        // Différer le chargement des données jusqu'à ce que la scène soit prête
        Platform.runLater(this::loadMatieres);
    }

    private void loadMatieres() {
        try {
            data.setAll(matiereService.getAll());
            renderCards();
        } catch (Exception e) {
            showError("Chargement impossible", "Impossible de charger les matières.", e.getMessage());
        }
    }

    private void applyFilter() {
        String keyword = searchField.getText();
        try {
            if (keyword == null || keyword.isBlank()) {
                data.setAll(matiereService.getAll());
            } else {
                data.setAll(matiereService.search(keyword));
            }
            renderCards();
        } catch (Exception e) {
            showError("Recherche indisponible", "Erreur lors de la recherche.", e.getMessage());
        }
    }

    private void renderCards() {
        matieresCardsPane.getChildren().clear();
        for (Matiere matiere : data) {
            VBox card = buildMatiereCard(matiere);
            matieresCardsPane.getChildren().add(card);
        }
        matieresCountLabel.setText(String.valueOf(data.size()));
    }

    private VBox buildMatiereCard(Matiere matiere) {
        VBox card = new VBox(8);
        card.getStyleClass().add("group-card");
        card.setPrefWidth(360);
        card.setMinHeight(160);

        Label avatar = new Label(GroupUiUtils.initial(matiere.getName()));
        avatar.getStyleClass().add("group-avatar");

        String titleText = matiere.getName() + (matiere.getCode() != null ? " (" + matiere.getCode() + ")" : "");
        Label title = new Label(titleText);
        title.getStyleClass().add("group-title");
        title.setGraphic(GroupUiUtils.icon("fas-book", "group-privacy-icon"));

        String desc = GroupUiUtils.nullSafe(matiere.getDescription());
        if (desc.length() > 100) desc = desc.substring(0, 97) + "...";
        Label description = new Label(desc.isBlank() ? "Aucune description" : desc);
        description.getStyleClass().add("group-meta");
        description.setWrapText(true);

        MenuItem editItem = new MenuItem("Modifier");
        editItem.setGraphic(GroupUiUtils.icon("fas-edit", "detail-menu-icon"));
        editItem.setOnAction(e -> onEdit(matiere));

        MenuItem deleteItem = new MenuItem("Supprimer");
        deleteItem.setGraphic(GroupUiUtils.icon("fas-trash-alt", "detail-menu-danger-icon"));
        deleteItem.getStyleClass().add("danger-item");
        deleteItem.setOnAction(e -> onDelete(matiere));

        Circle dotTop = new Circle(1.7);
        Circle dotMiddle = new Circle(1.7);
        Circle dotBottom = new Circle(1.7);
        VBox dotsIcon = new VBox(2.1, dotTop, dotMiddle, dotBottom);
        dotsIcon.setAlignment(Pos.CENTER);
        dotsIcon.getStyleClass().add("menu-dot-icon");

        MenuButton menu = new MenuButton();
        menu.setGraphic(dotsIcon);
        menu.getStyleClass().add("menu-dots");
        menu.getItems().addAll(editItem, deleteItem);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topLine = new HBox(avatar, spacer, menu);
        topLine.getStyleClass().add("card-top-row");

        VBox topRow = new VBox(2, title);
        VBox cardHead = new VBox(6);
        cardHead.getChildren().addAll(topLine, topRow);

        card.getChildren().addAll(cardHead, description);
        card.setOnMouseClicked(event -> openChapitres(matiere));
        return card;
    }

    private void openChapitres(Matiere matiere) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/matieres/ChapitreListView.fxml"));
            Parent root = loader.load();
            ChapitreListController controller = loader.getController();
            controller.setMatiere(matiere);

            Stage stage = (Stage) matieresCardsPane.getScene().getWindow();
            GroupUiUtils.switchScene(stage, root, "Chapitres - " + matiere.getName());
        } catch (IOException e) {
            showError("Navigation impossible", "Impossible d'ouvrir les chapitres.", e.getMessage());
        }
    }

    @FXML
    private void onAjouter() {
        Matiere newMatiere = MatiereFormController.showDialog(null, ajouterButton.getScene().getWindow());
        if (newMatiere != null) {
            try {
                matiereService.add(newMatiere);
                loadMatieres();
                showSuccess("Matière ajoutée", "La matière a été créée avec succès.");
            } catch (Exception e) {
                showError("Erreur", "Impossible d'ajouter la matière.", e.getMessage());
            }
        }
    }

    private void onEdit(Matiere matiere) {
        Matiere updated = MatiereFormController.showDialog(matiere, ajouterButton.getScene().getWindow());
        if (updated != null) {
            try {
                matiereService.update(updated);
                loadMatieres();
                showSuccess("Matière modifiée", "Les modifications ont été enregistrées.");
            } catch (Exception e) {
                showError("Erreur", "Impossible de modifier la matière.", e.getMessage());
            }
        }
    }

    private void onDelete(Matiere matiere) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer la matière");
        confirm.setHeaderText("Confirmer la suppression");
        confirm.setContentText("Voulez-vous supprimer la matière \"" + matiere.getName() + "\" ?\nCette action est irréversible.");

        ButtonType deleteType = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(deleteType, cancelType);
        GroupUiUtils.applyDialogStyle(confirm.getDialogPane(), MatiereListController.class);

        Button deleteButton = (Button) confirm.getDialogPane().lookupButton(deleteType);
        if (deleteButton != null) {
            deleteButton.getStyleClass().add("danger-btn");
            deleteButton.setGraphic(GroupUiUtils.icon("fas-trash-alt", "detail-dialog-danger-icon"));
        }

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == deleteType) {
            try {
                matiereService.delete(matiere.getId());
                loadMatieres();
                showSuccess("Matière supprimée", "La matière a été supprimée.");
            } catch (Exception e) {
                showError("Erreur", "Impossible de supprimer la matière.", e.getMessage());
            }
        }
    }

    private void showSuccess(String header, String content) {
        GroupUiUtils.showSuccess(matieresCardsPane.getScene().getWindow(), MatiereListController.class, header, content);
    }

    private void showError(String header, String content, String details) {
        GroupUiUtils.showError(matieresCardsPane.getScene().getWindow(), MatiereListController.class, header, content, details);
    }
}