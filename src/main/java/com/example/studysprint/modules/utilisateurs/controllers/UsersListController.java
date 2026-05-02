package com.example.studysprint.modules.utilisateurs.controllers;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.modules.utilisateurs.services.UtilisateurService;
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
    @FXML private TableColumn<Utilisateur, String> nomCol;
    @FXML private TableColumn<Utilisateur, String> prenomCol;
    @FXML private TableColumn<Utilisateur, String> emailCol;
    @FXML private TableColumn<Utilisateur, String> roleCol;
    @FXML private TableColumn<Utilisateur, String> statutCol;
    @FXML private TableColumn<Utilisateur, Void> actionsCol;
    @FXML private TextField searchField, expMinField;
    @FXML private ComboBox<String> roleFilter, statusFilter, sortFilter, orderFilter, specialtyFilter;
    @FXML private HBox profFilterBar;

    private final UtilisateurService userService = new UtilisateurService();
    private ObservableList<Utilisateur> masterData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTable();
        setupFilters();
        loadData();
    }

    private void setupTable() {
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
            
            // Send email if deactivated
            if (nextStatus.equals("inactif")) {
                new Thread(() -> {
                    try {
                        com.example.studysprint.utils.MailerService.sendAccountDeactivationNotice(u.getEmail(), u.getFullName());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
            
            loadData(); // Refresh table
        }
    }

    private void loadData() {
        List<Utilisateur> list = userService.getAll();
        masterData.setAll(list);
        applyFilters(); // Apply current filters to loaded data
    }

    private void setupFilters() {
        // 1. Roles
        roleFilter.getItems().setAll("Tous les rôles", "Administrateur", "Professeur", "Étudiant");
        roleFilter.setValue("Tous les rôles");

        // 2. Status
        statusFilter.getItems().setAll("Tous les statuts", "Actif", "Inactif");
        statusFilter.setValue("Tous les statuts");

        // 3. Sort & Order
        sortFilter.getItems().setAll("Nom", "Date Inscription", "Années Expérience");
        sortFilter.setValue("Nom");
        
        orderFilter.getItems().setAll("Croissant ↑", "Décroissant ↓");
        orderFilter.setValue("Croissant ↑");

        // 4. Specialty (API)
        specialtyFilter.getItems().add("Toutes les spécialités");
        specialtyFilter.setValue("Toutes les spécialités");
        com.example.studysprint.utils.ExternalApiService.fetchSpecialites().thenAccept(specs -> 
            Platform.runLater(() -> specialtyFilter.getItems().addAll(specs)));

        // 5. Connect Listeners (Live Search)
        searchField.textProperty().addListener((obs, old, val) -> applyFilters());
        expMinField.textProperty().addListener((obs, old, val) -> applyFilters());
        roleFilter.valueProperty().addListener((obs, old, val) -> {
            applyFilters();
            // Show/Hide prof bar based on role
            profFilterBar.setDisable(!"Professeur".equals(val) && !"Tous les rôles".equals(val));
        });
        statusFilter.valueProperty().addListener((obs, old, val) -> applyFilters());
        sortFilter.valueProperty().addListener((obs, old, val) -> applyFilters());
        orderFilter.valueProperty().addListener((obs, old, val) -> applyFilters());
        specialtyFilter.valueProperty().addListener((obs, old, val) -> applyFilters());
    }

    @FXML
    private void applyFilters() {
        String query = safe(searchField.getText());
        String roleStr = roleFilter.getValue();
        String statusStr = statusFilter.getValue();
        String sortStr = sortFilter.getValue();
        String orderStr = orderFilter.getValue();
        String specStr = specialtyFilter.getValue();
        String expStr = expMinField.getText();

        java.util.stream.Stream<Utilisateur> stream = masterData.stream();

        // --- FILTERING ---
        // Text Search
        if (!query.isEmpty()) {
            stream = stream.filter(u -> safe(u.getNom()).contains(query)
                                     || safe(u.getPrenom()).contains(query)
                                     || safe(u.getEmail()).contains(query));
        }

        // Role
        if (roleStr != null && !"Tous les rôles".equals(roleStr)) {
            String roleCode = roleStr.equals("Administrateur") ? "ROLE_ADMIN" : 
                             roleStr.equals("Professeur") ? "ROLE_PROFESSOR" : "ROLE_STUDENT";
            stream = stream.filter(u -> roleCode.equals(u.getRole()));
        }

        // Status
        if (statusStr != null && !"Tous les statuts".equals(statusStr)) {
            stream = stream.filter(u -> {
                boolean isActive = u.getStatut() == null || u.getStatut().isEmpty() || "actif".equalsIgnoreCase(u.getStatut());
                return statusStr.equals("Actif") ? isActive : !isActive;
            });
        }

        // Professor Specifics (Specialty)
        if (specStr != null && !"Toutes les spécialités".equals(specStr)) {
            stream = stream.filter(u -> specStr.equals(u.getSpecialite()));
        }

        // Experience Exact Match
        if (expStr != null && !expStr.trim().isEmpty()) {
            try {
                int exact = Integer.parseInt(expStr.trim());
                stream = stream.filter(u -> u.getAnneesExperience() != null && u.getAnneesExperience() == exact);
            } catch (NumberFormatException ignored) {}
        }

        // --- SORTING ---
        if (sortStr != null) {
            java.util.Comparator<Utilisateur> comparator;
            boolean isAsc = "Croissant ↑".equals(orderStr);

            switch (sortStr) {
                case "Date Inscription":
                    comparator = (u1, u2) -> {
                        if (u1.getDateInscription() == null) return 1;
                        if (u2.getDateInscription() == null) return -1;
                        return isAsc ? u1.getDateInscription().compareTo(u2.getDateInscription())
                                     : u2.getDateInscription().compareTo(u1.getDateInscription());
                    };
                    break;
                case "Années Expérience":
                    comparator = (u1, u2) -> {
                        int e1 = u1.getAnneesExperience() == null ? 0 : u1.getAnneesExperience();
                        int e2 = u2.getAnneesExperience() == null ? 0 : u2.getAnneesExperience();
                        return isAsc ? Integer.compare(e1, e2) : Integer.compare(e2, e1);
                    };
                    break;
                default: // Nom
                    comparator = (u1, u2) -> {
                        int res = safe(u1.getNom()).compareTo(safe(u2.getNom()));
                        return isAsc ? res : -res;
                    };
                    break;
            }
            stream = stream.sorted(comparator);
        }

        List<Utilisateur> filtered = stream.toList();
        usersTable.setItems(FXCollections.observableArrayList(filtered));
        usersTable.refresh();
    }

    @FXML
    private void handleResetSearch() {
        searchField.clear();
        expMinField.clear();
        roleFilter.setValue("Tous les rôles");
        statusFilter.setValue("Tous les statuts");
        sortFilter.setValue("Nom");
        orderFilter.setValue("Croissant ↑");
        specialtyFilter.setValue("Toutes les spécialités");
        applyFilters();
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
