package com.example.studysprint.modules.utilisateurs.controllers;

import com.example.studysprint.modules.utilisateurs.models.ReactivationRequest;
import com.example.studysprint.modules.utilisateurs.services.ReactivationService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class ReactivationRequestsController implements Initializable {

    @FXML private TableView<ReactivationRequest> requestsTable;
    @FXML private TableColumn<ReactivationRequest, String> userCol, emailCol, reasonCol, dateCol, actionsCol;

    private final ReactivationService reactivationService = new ReactivationService();
    private MainAdminController mainAdminController;

    public void setMainAdminController(MainAdminController controller) {
        this.mainAdminController = controller;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTable();
        loadRequests();
    }

    private void setupTable() {
        userCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUserDisplayName()));
        emailCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUserEmail()));
        reasonCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getReason()));
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCreatedAt().format(formatter)));

        actionsCol.setCellFactory(param -> new TableCell<>() {
            private final Button approveBtn = new Button("Accepter");
            private final Button rejectBtn = new Button("Refuser");
            private final HBox container = new HBox(10, approveBtn, rejectBtn);

            {
                approveBtn.setStyle("-fx-background-color: #55efc4; -fx-text-fill: white; -fx-cursor: hand;");
                rejectBtn.setStyle("-fx-background-color: #ff7675; -fx-text-fill: white; -fx-cursor: hand;");
                
                approveBtn.setOnAction(event -> {
                    ReactivationRequest req = getTableView().getItems().get(getIndex());
                    handleProcess(req, true);
                });
                
                rejectBtn.setOnAction(event -> {
                    ReactivationRequest req = getTableView().getItems().get(getIndex());
                    handleProcess(req, false);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(container);
                }
            }
        });
    }

    private void loadRequests() {
        requestsTable.setItems(FXCollections.observableArrayList(reactivationService.getPendingRequests()));
    }

    private void handleProcess(ReactivationRequest request, boolean approve) {
        reactivationService.processRequest(request.getId(), approve);
        loadRequests();
        if (mainAdminController != null) {
            mainAdminController.refreshBadge();
        }
    }
}
