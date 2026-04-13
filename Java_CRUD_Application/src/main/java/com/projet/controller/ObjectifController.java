package com.projet.controller;

import com.projet.entity.Objectif;
import com.projet.service.ObjectifService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.sql.Date;

public class ObjectifController {

    @FXML private TableView<Objectif> tableObjectifs;
    @FXML private TableColumn<Objectif, Integer> colId;
    @FXML private TableColumn<Objectif, String> colTitre;
    @FXML private TableColumn<Objectif, String> colDescription;
    @FXML private TableColumn<Objectif, Date> colDateDebut;
    @FXML private TableColumn<Objectif, Date> colDateFin;
    @FXML private TableColumn<Objectif, String> colStatut;

    @FXML private TextField txtTitre;
    @FXML private TextArea txtDescription;
    @FXML private DatePicker dpDateDebut;
    @FXML private DatePicker dpDateFin;
    @FXML private ComboBox<String> cbStatut;

    private ObjectifService objectifService;
    private ObservableList<Objectif> objectifsList;

    @FXML
    public void initialize() {
        objectifService = new ObjectifService();
        cbStatut.setItems(FXCollections.observableArrayList("En cours", "Terminé", "Annulé"));
        cbStatut.getSelectionModel().selectFirst();
        
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colDateDebut.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        colDateFin.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        
        loadData();
        
        tableObjectifs.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                txtTitre.setText(newSelection.getTitre());
                txtDescription.setText(newSelection.getDescription() != null ? newSelection.getDescription() : "");
                if (newSelection.getDateDebut() != null) dpDateDebut.setValue(newSelection.getDateDebut().toLocalDate());
                if (newSelection.getDateFin() != null) dpDateFin.setValue(newSelection.getDateFin().toLocalDate());
                cbStatut.setValue(newSelection.getStatut());
            }
        });
    }

    private void loadData() {
        objectifsList = FXCollections.observableArrayList(objectifService.findAll());
        tableObjectifs.setItems(objectifsList);
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
        Objectif selected = tableObjectifs.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Veuillez sélectionner un objectif à modifier.");
            return;
        }
        if (!validateInput()) return;
        
        selected.setTitre(txtTitre.getText());
        selected.setDescription(txtDescription.getText());
        selected.setDateDebut(Date.valueOf(dpDateDebut.getValue()));
        selected.setDateFin(Date.valueOf(dpDateFin.getValue()));
        selected.setStatut(cbStatut.getValue());
        
        objectifService.update(selected);
        loadData();
        clearForm();
    }

    @FXML
    public void handleSupprimer() {
        Objectif selected = tableObjectifs.getSelectionModel().getSelectedItem();
        if (selected != null) {
            objectifService.delete(selected.getId());
            loadData();
            clearForm();
        } else {
            showAlert("Veuillez sélectionner un objectif à supprimer.");
        }
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

    private void clearForm() {
        txtTitre.clear();
        txtDescription.clear();
        dpDateDebut.setValue(null);
        dpDateFin.setValue(null);
        cbStatut.getSelectionModel().selectFirst();
        tableObjectifs.getSelectionModel().clearSelection();
    }
}
