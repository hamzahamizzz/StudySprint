package com.example.studysprint.modules.utilisateurs.controllers;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.modules.utilisateurs.services.UtilisateurService;
import com.example.studysprint.utils.JpaUtils;
import com.example.studysprint.utils.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class UsersListController implements Initializable {

    @FXML private TableView<Utilisateur> usersTable;
    @FXML private TableColumn<Utilisateur, Integer> idCol;
    @FXML private TableColumn<Utilisateur, String> nomCol;
    @FXML private TableColumn<Utilisateur, String> prenomCol;
    @FXML private TableColumn<Utilisateur, String> emailCol;
    @FXML private TableColumn<Utilisateur, String> roleCol;
    @FXML private TableColumn<Utilisateur, String> statutCol;
    @FXML private TableColumn<Utilisateur, Void> actionsCol;
    @FXML private TextField searchField;

    private final UtilisateurService userService = new UtilisateurService();
    private ObservableList<Utilisateur> masterData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTable();
        loadData();
        setupSearch();
    }

    private void setupTable() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nomCol.setCellValueFactory(new PropertyValueFactory<>("nom"));
        prenomCol.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        statutCol.setCellValueFactory(new PropertyValueFactory<>("statut"));

        setupActionsColumn();
    }

    private void setupActionsColumn() {
        actionsCol.setCellFactory(new Callback<>() {
            @Override
            public TableCell<Utilisateur, Void> call(TableColumn<Utilisateur, Void> param) {
                return new TableCell<>() {
                    private final Button editBtn = new Button("Modifier");
                    private final Button deleteBtn = new Button("Supprimer");
                    private final Button statusBtn = new Button();
                    private final HBox container = new HBox(10, editBtn, statusBtn, deleteBtn);

                    {
                        editBtn.setStyle("-fx-background-color: #f1c40f;");
                        deleteBtn.setStyle("-fx-background-color: #e74c3c;");
                        
                        editBtn.setOnAction(event -> {
                            Utilisateur u = getTableView().getItems().get(getIndex());
                            handleEditUser(u);
                        });
                        
                        deleteBtn.setOnAction(event -> {
                            Utilisateur u = getTableView().getItems().get(getIndex());
                            handleDeleteUser(u);
                        });

                        statusBtn.setOnAction(event -> {
                            Utilisateur u = getTableView().getItems().get(getIndex());
                            handleToggleStatus(u);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        // Guard against invalid index (common JavaFX bug causing missing buttons)
                        if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                            setGraphic(null);
                            return;
                        }
                        Utilisateur u = getTableView().getItems().get(getIndex());
                        if (u == null) { setGraphic(null); return; }

                        boolean isActive = u.getStatut() == null || u.getStatut().isEmpty()
                                        || "actif".equalsIgnoreCase(u.getStatut());

                        if (isActive) {
                            statusBtn.setText("Désactiver");
                            statusBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white;");
                        } else {
                            statusBtn.setText("Activer");
                            statusBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white;");
                        }
                        setGraphic(container);
                    }
                };
            }
        });
    }

    private void handleToggleStatus(Utilisateur u) {
        boolean isActive = u.getStatut() == null || u.getStatut().isEmpty() || "actif".equalsIgnoreCase(u.getStatut());
        String nextStatus = isActive ? "inactif" : "actif";
        String actionName = isActive ? "désactiver" : "activer";

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(actionName.substring(0, 1).toUpperCase() + actionName.substring(1) + " le compte");
        alert.setContentText("Voulez-vous vraiment " + actionName + " le compte de " + u.getFullName() + " ?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            u.setStatut(nextStatus);
            userService.update(u);
            loadData(); // Refresh table
        }
    }

    private void loadData() {
        List<Utilisateur> list = userService.getAll();
        masterData.setAll(list);
        usersTable.setItems(masterData);
    }

    private void setupSearch() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            String query = newValue == null ? "" : newValue.toLowerCase();
            if (query.isEmpty()) {
                usersTable.setItems(masterData);
                usersTable.refresh(); // Force redraw of all cells
                return;
            }
            // Null-safe search using Stream API
            List<Utilisateur> filtered = masterData.stream()
                    .filter(u -> safe(u.getNom()).contains(query)
                              || safe(u.getPrenom()).contains(query)
                              || safe(u.getEmail()).contains(query))
                    .toList();
            usersTable.setItems(FXCollections.observableArrayList(filtered));
            usersTable.refresh(); // Force redraw of all cells
        });
    }

    @FXML
    private void handleResetSearch() {
        searchField.clear();
        usersTable.setItems(masterData);
        usersTable.refresh();
    }

    @FXML
    private void handleAddUser() {
        showForm(null);
    }

    private void handleEditUser(Utilisateur u) {
        showForm(u);
    }

    private void handleDeleteUser(Utilisateur u) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer l'utilisateur " + u.getFullName());
        alert.setContentText("Êtes-vous sûr de vouloir supprimer cet utilisateur ?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            userService.delete(u.getId());
            loadData();
        }
    }

    private void showForm(Utilisateur u) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/utilisateurs/user-form.fxml"));
            Parent root = loader.load();
            
            UserFormController controller = loader.getController();
            controller.setUtilisateur(u);
            controller.setParentController(this);

            Stage stage = new Stage();
            stage.setTitle(u == null ? "Ajouter un Utilisateur" : "Modifier un Utilisateur");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
            loadData(); // Refresh table after form close
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de chargement");
            alert.setHeaderText("Chargement du formulaire échoué");
            alert.setContentText("Cause : " + e.toString());
            alert.showAndWait();
        }
    }
    
    public void refreshTable() {
        loadData();
    }

    @FXML
    private void handleMyProfile() {
        switchScene("/fxml/utilisateurs/admin-profile.fxml", "Mon Profil - StudySprint");
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        switchScene("/fxml/auth/login.fxml", "Connexion - StudySprint");
    }

    private void switchScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) usersTable.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Returns empty string if value is null, otherwise lowercased value. */
    private String safe(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}
