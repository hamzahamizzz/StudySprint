package com.projet.controller;

import com.projet.entity.Objectif;
import com.projet.service.ObjectifService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.geometry.Pos;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ObjectifController {
    @FXML private FlowPane cardsContainer;

    @FXML private TextField txtTitre;
    @FXML private TextArea txtDescription;
    @FXML private DatePicker dpDateDebut;
    @FXML private DatePicker dpDateFin;
    @FXML private ComboBox<String> cbStatut;

    private ObjectifService objectifService;
    private Objectif selectedObjectif = null;

    @FXML
    public void initialize() {
        objectifService = new ObjectifService();
        cbStatut.setItems(FXCollections.observableArrayList("En cours", "Terminé", "Annulé"));
        cbStatut.getSelectionModel().selectFirst();
        
        loadData();
    }

    private void loadData() {
        cardsContainer.getChildren().clear();
        List<Objectif> objectifsList = objectifService.findAll();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (Objectif o : objectifsList) {
            VBox card = new VBox(8);
            card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 5);");
            card.setPrefWidth(240);

            Label lblTitre = new Label(o.getTitre());
            lblTitre.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");

            Label lblDesc = new Label(o.getDescription() != null ? o.getDescription() : "Aucune description");
            lblDesc.setStyle("-fx-text-fill: #555555;");
            lblDesc.setWrapText(true);

            String debutStr = o.getDateDebut() != null ? o.getDateDebut().toLocalDate().format(formatter) : "N/A";
            String finStr = o.getDateFin() != null ? o.getDateFin().toLocalDate().format(formatter) : "N/A";
            
            Label lblDates = new Label("Du: " + debutStr + " au " + finStr);
            lblDates.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

            Label lblStatut = new Label("Statut: " + o.getStatut());
            if ("En cours".equals(o.getStatut())) lblStatut.setTextFill(Color.ORANGE);
            else if ("Terminé".equals(o.getStatut())) lblStatut.setTextFill(Color.GREEN);
            else lblStatut.setTextFill(Color.RED);

            HBox actions = new HBox(10);
            actions.setAlignment(Pos.CENTER_LEFT);
            actions.setStyle("-fx-padding: 10 0 0 0;");
            
            Button btnEdit = new Button("Modifier");
            btnEdit.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand;");
            btnEdit.setOnAction(e -> fillForm(o));

            Button btnDelete = new Button("Supprimer");
            btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand;");
            btnDelete.setOnAction(e -> deleteObjectif(o));

            actions.getChildren().addAll(btnEdit, btnDelete);

            card.getChildren().addAll(lblTitre, lblDesc, lblDates, lblStatut, actions);
            cardsContainer.getChildren().add(card);
        }
    }

    private void fillForm(Objectif o) {
        selectedObjectif = o;
        txtTitre.setText(o.getTitre());
        txtDescription.setText(o.getDescription() != null ? o.getDescription() : "");
        if (o.getDateDebut() != null) dpDateDebut.setValue(o.getDateDebut().toLocalDate());
        if (o.getDateFin() != null) dpDateFin.setValue(o.getDateFin().toLocalDate());
        cbStatut.setValue(o.getStatut());
    }

    private void deleteObjectif(Objectif o) {
        objectifService.delete(o.getId());
        if (selectedObjectif != null && selectedObjectif.getId().equals(o.getId())) {
            clearForm();
        }
        loadData();
    }

    @FXML
    public void handleAjouter() {
        if (!validateInput()) return;
        
        Objectif o = new Objectif(
            null,
            txtTitre.getText(),
            txtDescription.getText(),
            Date.valueOf(dpDateDebut.getValue()),
            Date.valueOf(dpDateFin.getValue()),
            cbStatut.getValue(),
            1 // Default etudiantId
        );
        objectifService.create(o);
        loadData();
        clearForm();
    }

    @FXML
    public void handleModifier() {
        if (selectedObjectif == null) {
            showAlert("Veuillez d'abord cliquer sur 'Modifier' sur une des cartes.");
            return;
        }
        if (!validateInput()) return;
        
        selectedObjectif.setTitre(txtTitre.getText());
        selectedObjectif.setDescription(txtDescription.getText());
        selectedObjectif.setDateDebut(Date.valueOf(dpDateDebut.getValue()));
        selectedObjectif.setDateFin(Date.valueOf(dpDateFin.getValue()));
        selectedObjectif.setStatut(cbStatut.getValue());
        
        objectifService.update(selectedObjectif);
        loadData();
        clearForm();
    }

    @FXML
    public void clearForm() {
        selectedObjectif = null;
        txtTitre.clear();
        txtDescription.clear();
        dpDateDebut.setValue(null);
        dpDateFin.setValue(null);
        cbStatut.getSelectionModel().selectFirst();
    }

    private boolean validateInput() {
        if (txtTitre.getText() == null || txtTitre.getText().trim().isEmpty()) {
            showAlert("Le titre ne doit pas être vide.");
            return false;
        }
        if (dpDateDebut.getValue() == null) {
            showAlert("La date de début est obligatoire.");
            return false;
        }
        if (dpDateFin.getValue() == null) {
            showAlert("La date de fin est obligatoire.");
            return false;
        }
        if (dpDateFin.getValue().isBefore(dpDateDebut.getValue())) {
            showAlert("La date de fin doit être postérieure ou égale à la date de début.");
            return false;
        }
        return true;
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur de validation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
