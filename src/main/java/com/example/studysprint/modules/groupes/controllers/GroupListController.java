package com.example.studysprint.modules.groupes.controllers;

import com.example.studysprint.modules.groupes.models.StudyGroup;
import com.example.studysprint.modules.groupes.services.GroupMemberService;
import com.example.studysprint.modules.groupes.services.GroupService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class GroupListController {
    private static final int CURRENT_USER_ID = 1;

    @FXML
    private Button createGroupButton;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private ComboBox<String> roleCombo;
    @FXML
    private FlowPane groupCardsPane;
    @FXML
    private Label groupsCountLabel;

    private final GroupService groupService = new GroupService();
    private final GroupMemberService memberService = new GroupMemberService();
    private final ObservableList<StudyGroup> data = FXCollections.observableArrayList();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private StudyGroup selectedGroup;

    @FXML
    public void initialize() {
        configureFilters();
        loadGroups();
    }

    private void configureFilters() {
        sortCombo.setItems(FXCollections.observableArrayList(
            "Activite recente",
            "Nom (A-Z)",
            "Nom (Z-A)",
            "Plus de membres",
            "Plus recents"
        ));
        sortCombo.getSelectionModel().select("Activite recente");

        roleCombo.setItems(FXCollections.observableArrayList("Tous les roles", "Admin", "Moderator", "Membre"));
        roleCombo.getSelectionModel().select("Tous les roles");

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        sortCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        roleCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void loadGroups() {
        try {
            data.setAll(groupService.getAll());
            applyLocalSort();
            renderCards();
        } catch (Exception e) {
            showError("Chargement impossible", "Oups, nous n'avons pas pu charger vos groupes pour le moment.", e.getMessage());
        }
    }

    private void applyFilters() {
        String keyword = searchField.getText();
        try {
            if (keyword == null || keyword.isBlank()) {
                data.setAll(groupService.getAll());
            } else {
                data.setAll(groupService.search(keyword));
            }
            applyLocalSort();
            renderCards();
        } catch (Exception e) {
            showError("Recherche/tri indisponible", "Nous n'avons pas pu appliquer vos filtres. Reessayez dans un instant.", e.getMessage());
        }
    }

    private void applyLocalSort() {
        String sortBy = sortCombo.getValue();
        Comparator<StudyGroup> comparator;
        Map<Integer, Integer> memberCountCache = new HashMap<>();

        if ("Nom (A-Z)".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(g -> nullSafe(g.getName()), String.CASE_INSENSITIVE_ORDER);
        } else if ("Nom (Z-A)".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing((StudyGroup g) -> nullSafe(g.getName()), String.CASE_INSENSITIVE_ORDER).reversed();
        } else if ("Plus de membres".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparingInt((StudyGroup g) ->
                    memberCountCache.computeIfAbsent(g.getId(), id -> memberService.countMembersForGroup(id))
            ).reversed();
        } else if ("Plus recents".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(StudyGroup::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed();
        } else if ("Activite recente".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(StudyGroup::getLastActivity, Comparator.nullsLast(Comparator.naturalOrder())).reversed();
        } else {
            comparator = Comparator.comparing(g -> nullSafe(g.getName()), String.CASE_INSENSITIVE_ORDER);
        }

        FXCollections.sort(data, comparator);
    }

    private void renderCards() {
        groupCardsPane.getChildren().clear();

        String roleFilter = roleCombo.getValue();
        int visibleCount = 0;

        for (StudyGroup group : data) {
            String inferredRole = inferRole(group);
            if (!"Tous les roles".equals(roleFilter) && !inferredRole.equalsIgnoreCase(roleFilter)) {
                continue;
            }

            VBox card = buildGroupCard(group, inferredRole);
            groupCardsPane.getChildren().add(card);
            visibleCount++;
        }

        groupCardsPane.getChildren().add(buildJoinGroupCard());
        groupsCountLabel.setText(String.valueOf(visibleCount));
    }

    private VBox buildGroupCard(StudyGroup group, String role) {
        VBox card = new VBox(8);
        card.getStyleClass().add("group-card");
        card.setPrefWidth(360);
        card.setMinHeight(188);

        Label avatar = new Label(initial(group.getName()));
        avatar.getStyleClass().add("group-avatar");

        Label title = new Label(nullSafe(group.getName()));
        title.getStyleClass().add("group-title");

        Label roleLabel = new Label(role);
        roleLabel.getStyleClass().add("role-pill");

        Label subject = new Label(nullSafe(group.getSubject()));
        subject.getStyleClass().add("group-subject");

        int memberCount = memberService.countMembersForGroup(group.getId());
        Label members = new Label("Membres: " + memberCount + "  |  " + nullSafe(group.getPrivacy()));
        members.getStyleClass().add("group-meta");

        Label activity = new Label("Derniere activite : " + formatDate(group.getLastActivity()));
        activity.getStyleClass().add("group-meta");

        MenuItem openItem = new MenuItem("📂 Ouvrir");
        openItem.setOnAction(event -> openGroup(group));

        MenuItem editItem = new MenuItem("✏ Modifier");
        editItem.setOnAction(event -> onEditSelected(group));

        MenuItem deleteItem = new MenuItem("🗑 Supprimer le groupe");
        deleteItem.setOnAction(event -> onDeleteSelected(group));

        MenuButton menu = new MenuButton("\u22EE");
        menu.getStyleClass().add("menu-dots");
        menu.getItems().addAll(openItem, editItem, deleteItem);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topLine = new HBox(avatar, spacer, menu);
        topLine.getStyleClass().add("card-top-row");

        VBox topRow = new VBox(2, title, roleLabel);
        topRow.getStyleClass().add("group-head");

        VBox cardHead = new VBox(6);
        cardHead.getChildren().addAll(topLine, topRow);

        card.getChildren().addAll(cardHead, subject, members, activity);
        card.setOnMouseClicked(event -> selectCard(group, card));

        return card;
    }

    private VBox buildJoinGroupCard() {
        VBox card = new VBox();
        card.getStyleClass().addAll("group-card", "join-card");
        card.setPrefWidth(360);
        card.setMinHeight(188);

        Label joinText = new Label("Rejoindre un groupe");
        joinText.getStyleClass().add("join-label");
        card.getChildren().add(joinText);

        return card;
    }

    private void selectCard(StudyGroup group, VBox selectedCard) {
        selectedGroup = group;
        groupCardsPane.getChildren().forEach(node -> node.getStyleClass().remove("group-card-selected"));
        selectedCard.getStyleClass().add("group-card-selected");
    }

    @FXML
    private void onAdd() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/groupes/GroupFormView.fxml"));
            loader.load();
            GroupFormController controller = loader.getController();

            StudyGroup created = controller.showDialog(null);
            if (created == null) {
                return;
            }

            groupService.add(created);
            loadGroups();
            showSuccess("Groupe cree", "Super ! Le groupe a ete ajoute avec succes.");
        } catch (Exception e) {
            showError("Erreur", "Impossible de creer le groupe.", e.getMessage());
        }
    }

    private void onEditSelected(StudyGroup selected) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/groupes/GroupFormView.fxml"));
            loader.load();
            GroupFormController controller = loader.getController();

            StudyGroup updated = controller.showDialog(selected);
            if (updated == null) {
                return;
            }

            groupService.update(updated);
            loadGroups();
            showSuccess("Groupe modifie", "Parfait, vos modifications ont bien ete enregistrees.");
        } catch (Exception e) {
            showError("Erreur", "Impossible de modifier le groupe.", e.getMessage());
        }
    }

    private void onDeleteSelected(StudyGroup selected) {
        selectedGroup = selected;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer le groupe");
        confirm.setHeaderText("Confirmer la suppression");
        confirm.setContentText("Voulez-vous supprimer le groupe \"" + selected.getName() + "\" ?\nCette action est irreversible.");

        ButtonType deleteType = new ButtonType("🗑 Supprimer");
        ButtonType cancelType = new ButtonType("✖ Annuler");
        confirm.getButtonTypes().setAll(deleteType, cancelType);
        applyDialogStyle(confirm.getDialogPane());

        Button deleteButton = (Button) confirm.getDialogPane().lookupButton(deleteType);
        if (deleteButton != null) {
            deleteButton.getStyleClass().add("danger-btn");
        }

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == deleteType) {
            try {
                groupService.delete(selected.getId());
                selectedGroup = null;
                loadGroups();
                showSuccess("Groupe supprime", "C'est fait. Le groupe a bien ete supprime.");
            } catch (Exception e) {
                showError("Delete error", "Unable to delete group", e.getMessage());
            }
        }
    }

    private void openGroup(StudyGroup group) {
        selectedGroup = group;
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Informations du groupe");
        info.setHeaderText(group.getName());
        info.setContentText("Le module detail de ce groupe arrive bientot. Merci pour votre patience.");
        applyDialogStyle(info.getDialogPane());
        info.showAndWait();
    }

    private void showWarning(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Attention");
        alert.setHeaderText(header);
        alert.setContentText(content);
        applyDialogStyle(alert.getDialogPane());
        alert.showAndWait();
    }

    private void showSuccess(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succes");
        alert.setHeaderText(header);
        alert.setContentText(content);
        applyDialogStyle(alert.getDialogPane());
        alert.showAndWait();
    }

    private void showError(String header, String content, String details) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(header);
        alert.setContentText(content + "\n\nDetails: " + details);
        applyDialogStyle(alert.getDialogPane());
        alert.showAndWait();
    }

    private void applyDialogStyle(DialogPane pane) {
        var cssUrl = getClass().getResource("/styles/groupes-light-blue.css");
        if (cssUrl == null) {
            return;
        }
        String stylesheet = cssUrl.toExternalForm();
        if (!pane.getStylesheets().contains(stylesheet)) {
            pane.getStylesheets().add(stylesheet);
        }
    }

    private String formatDate(Timestamp timestamp) {
        if (timestamp == null) {
            return "N/A";
        }
        return timestamp.toLocalDateTime().format(dateFormatter);
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String initial(String value) {
        if (value == null || value.isBlank()) {
            return "G";
        }
        return value.substring(0, 1).toUpperCase();
    }

    private String inferRole(StudyGroup group) {
        if (group.getCreatedById() != null && group.getCreatedById() == CURRENT_USER_ID) {
            return "Admin";
        }

        String roleFromMembership = memberService
                .getMemberRoleForUser(group.getId(), CURRENT_USER_ID)
                .orElse("")
                .trim()
                .toLowerCase();

        if ("moderator".equals(roleFromMembership)) {
            return "Moderator";
        }
        if ("admin".equals(roleFromMembership)) {
            return "Admin";
        }

        return "Membre";
    }
}
