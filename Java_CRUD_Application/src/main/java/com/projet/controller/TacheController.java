package com.projet.controller;

import com.projet.entity.Objectif;
import com.projet.entity.Tache;
import com.projet.service.ObjectifService;
import com.projet.service.TacheService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.geometry.Pos;
import javafx.util.StringConverter;
import java.util.List;

public class TacheController {
    @FXML private FlowPane cardsContainer;

    @FXML private TextField txtTitre;
    @FXML private TextField txtDuree;
    @FXML private ComboBox<String> cbPriorite;
    @FXML private ComboBox<String> cbStatut;
    @FXML private ComboBox<Objectif> cbObjectif;

    private TacheService tacheService;
    private ObjectifService objectifService;
    private ObservableList<Objectif> objectifsList;
    
    // Tracks which Tache is currently selected for editing
    private Tache selectedTache = null;

    @FXML
    public void initialize() {
        tacheService = new TacheService();
        objectifService = new ObjectifService();
        
        cbPriorite.setItems(FXCollections.observableArrayList("Basse", "Moyenne", "Haute"));
        cbPriorite.getSelectionModel().selectFirst();
        cbStatut.setItems(FXCollections.observableArrayList("A faire", "En cours", "Terminée"));
        cbStatut.getSelectionModel().selectFirst();

        objectifsList = FXCollections.observableArrayList(objectifService.findAll());
        cbObjectif.setItems(objectifsList);
        cbObjectif.setConverter(new StringConverter<Objectif>() {
            @Override
            public String toString(Objectif o) {
                return o == null ? "" : o.getTitre();
            }
            @Override
            public Objectif fromString(String string) {
                return null;
            }
        });

        loadData();
    }

    private void loadData() {
        cardsContainer.getChildren().clear();
        List<Tache> taches = tacheService.findAll();
        
        for (Tache t : taches) {
            VBox card = new VBox(8);
            card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 5);");
            card.setPrefWidth(220);

            Label lblTitre = new Label(t.getTitre());
            lblTitre.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");

            Label lblDuree = new Label("Durée : " + t.getDuree() + "h");
            
            Label lblPriorite = new Label("Priorité : " + t.getPriorite());
            if ("Haute".equals(t.getPriorite())) lblPriorite.setTextFill(Color.RED);
            else if ("Moyenne".equals(t.getPriorite())) lblPriorite.setTextFill(Color.ORANGE);
            else lblPriorite.setTextFill(Color.GREEN);

            Label lblStatut = new Label("Statut : " + t.getStatut());

            String objName = objectifsList.stream()
                .filter(o -> o.getId().equals(t.getObjectifId()))
                .findFirst()
                .map(Objectif::getTitre)
                .orElse("Objectif inconnu");
            Label lblObj = new Label("Objectif : " + objName);
            lblObj.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic;");

            HBox actions = new HBox(10);
            actions.setAlignment(Pos.CENTER_LEFT);
            actions.setStyle("-fx-padding: 10 0 0 0;");
            
            Button btnEdit = new Button("Modifier");
            btnEdit.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand;");
            btnEdit.setOnAction(e -> fillForm(t));

            Button btnDelete = new Button("Supprimer");
            btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand;");
            btnDelete.setOnAction(e -> deleteTache(t));

            actions.getChildren().addAll(btnEdit, btnDelete);

            card.getChildren().addAll(lblTitre, lblDuree, lblPriorite, lblStatut, lblObj, actions);
            cardsContainer.getChildren().add(card);
        }
    }

    private void fillForm(Tache t) {
        selectedTache = t;
        txtTitre.setText(t.getTitre());
        txtDuree.setText(String.valueOf(t.getDuree()));
        cbPriorite.setValue(t.getPriorite());
        cbStatut.setValue(t.getStatut());
        
        Objectif assigned = objectifsList.stream()
                .filter(o -> o.getId().equals(t.getObjectifId()))
                .findFirst()
                .orElse(null);
        cbObjectif.setValue(assigned);
    }

    private void deleteTache(Tache t) {
        tacheService.delete(t.getId());
        if (selectedTache != null && selectedTache.getId().equals(t.getId())) {
            clearForm();
        }
        loadData();
    }

    @FXML
    public void handleAjouter() {
        if (!validateInput()) return;
        Tache t = new Tache(
            null,
            txtTitre.getText(),
            Integer.parseInt(txtDuree.getText()),
            cbPriorite.getValue(),
            cbStatut.getValue(),
            cbObjectif.getValue().getId()
        );
        tacheService.create(t);
        loadData();
        clearForm();
    }

    @FXML
    public void handleModifier() {
        if (selectedTache == null) {
            showAlert("Veuillez d'abord cliquer sur 'Modifier' sur une des cartes.");
            return;
        }
        if (!validateInput()) return;

        selectedTache.setTitre(txtTitre.getText());
        selectedTache.setDuree(Integer.parseInt(txtDuree.getText()));
        selectedTache.setPriorite(cbPriorite.getValue());
        selectedTache.setStatut(cbStatut.getValue());
        selectedTache.setObjectifId(cbObjectif.getValue().getId());

        tacheService.update(selectedTache);
        loadData();
        clearForm();
    }

    @FXML
    public void clearForm() {
        selectedTache = null;
        txtTitre.clear();
        txtDuree.clear();
        cbObjectif.getSelectionModel().clearSelection();
        cbPriorite.getSelectionModel().selectFirst();
        cbStatut.getSelectionModel().selectFirst();
    }

    private boolean validateInput() {
        if (txtTitre.getText() == null || txtTitre.getText().trim().isEmpty()) {
            showAlert("Le titre ne doit pas être vide.");
            return false;
        }
        int duree;
        try {
            duree = Integer.parseInt(txtDuree.getText());
            if (duree <= 0) {
                showAlert("La durée doit être supérieure à 0.");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("La durée doit être un nombre valide.");
            return false;
        }
        if (cbObjectif.getValue() == null) {
            showAlert("Veuillez sélectionner un Objectif !");
            return false;
        }
        return true;
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
