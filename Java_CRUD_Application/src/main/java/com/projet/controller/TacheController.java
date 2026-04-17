package com.projet.controller;

import com.projet.entity.Objectif;
import com.projet.entity.Tache;
import com.projet.service.ObjectifService;
import com.projet.service.TacheService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

public class TacheController {
    @FXML private TableView<Tache> tableTaches;
    @FXML private TableColumn<Tache, String> colTitre;
    @FXML private TableColumn<Tache, Integer> colDuree;
    @FXML private TableColumn<Tache, String> colPriorite;
    @FXML private TableColumn<Tache, String> colStatut;
    @FXML private TableColumn<Tache, Integer> colObjectif;

    @FXML private TextField txtTitre;
    @FXML private TextField txtDuree;
    @FXML private ComboBox<String> cbPriorite;
    @FXML private ComboBox<String> cbStatut;
    @FXML private ComboBox<Objectif> cbObjectif;

    private TacheService tacheService;
    private ObjectifService objectifService;
    private ObservableList<Tache> tachesList;
    private ObservableList<Objectif> objectifsList;

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

        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colDuree.setCellValueFactory(new PropertyValueFactory<>("duree"));
        colPriorite.setCellValueFactory(new PropertyValueFactory<>("priorite"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        
        // This is where we map the raw ID field into a beautifully displayed Title!
        colObjectif.setCellValueFactory(new PropertyValueFactory<>("objectifId"));
        colObjectif.setCellFactory(column -> new TableCell<Tache, Integer>() {
            @Override
            protected void updateItem(Integer objectifId, boolean empty) {
                super.updateItem(objectifId, empty);
                if (empty || objectifId == null) {
                    setText(null);
                } else {
                    String title = objectifsList.stream()
                            .filter(o -> o.getId().equals(objectifId))
                            .findFirst()
                            .map(Objectif::getTitre)
                            .orElse("ID: " + objectifId);
                    setText(title);
                }
            }
        });

        loadData();

        tableTaches.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                txtTitre.setText(newSelection.getTitre());
                txtDuree.setText(String.valueOf(newSelection.getDuree()));
                cbPriorite.setValue(newSelection.getPriorite());
                cbStatut.setValue(newSelection.getStatut());
                
                Objectif assigned = objectifsList.stream()
                        .filter(o -> o.getId().equals(newSelection.getObjectifId()))
                        .findFirst()
                        .orElse(null);
                cbObjectif.setValue(assigned);
            }
        });
    }

    private void loadData() {
        tachesList = FXCollections.observableArrayList(tacheService.findAll());
        tableTaches.setItems(tachesList);
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
        Tache selected = tableTaches.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Veuillez sélectionner une tâche à modifier.");
            return;
        }
        if (!validateInput()) return;

        selected.setTitre(txtTitre.getText());
        selected.setDuree(Integer.parseInt(txtDuree.getText()));
        selected.setPriorite(cbPriorite.getValue());
        selected.setStatut(cbStatut.getValue());
        selected.setObjectifId(cbObjectif.getValue().getId());

        tacheService.update(selected);
        loadData();
        clearForm();
    }

    @FXML
    public void handleSupprimer() {
        Tache selected = tableTaches.getSelectionModel().getSelectedItem();
        if (selected != null) {
            tacheService.delete(selected.getId());
            loadData();
            clearForm();
        } else {
            showAlert("Veuillez sélectionner une tâche à supprimer.");
        }
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
        alert.setTitle("Erreur de validation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void clearForm() {
        txtTitre.clear();
        txtDuree.clear();
        cbObjectif.getSelectionModel().clearSelection();
        cbPriorite.getSelectionModel().selectFirst();
        cbStatut.getSelectionModel().selectFirst();
        tableTaches.getSelectionModel().clearSelection();
    }
}
