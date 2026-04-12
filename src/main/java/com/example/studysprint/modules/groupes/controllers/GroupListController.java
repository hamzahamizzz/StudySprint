package com.example.studysprint.modules.groupes.controllers;

import com.example.studysprint.modules.groupes.models.StudyGroup;
import com.example.studysprint.modules.groupes.services.GroupMemberService;
import com.example.studysprint.modules.groupes.services.GroupService;
import com.example.studysprint.modules.groupes.utils.GroupUiUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
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
    @FXML
    private Button groupsTabButton;
    @FXML
    private Button invitationsTabButton;
    @FXML
    private Button feedbacksTabButton;
    @FXML
    private Label groupsTabCountLabel;
    @FXML
    private Label invitationsTabCountLabel;
    @FXML
    private Label feedbacksTabCountLabel;
    @FXML
    private VBox groupsContentPane;
    @FXML
    private VBox invitationsContentPane;
    @FXML
    private VBox feedbacksContentPane;

    private final GroupService groupService = new GroupService();
    private final GroupMemberService memberService = new GroupMemberService();
    private final ObservableList<StudyGroup> data = FXCollections.observableArrayList();
    private StudyGroup selectedGroup;

    // Configure default UI state and load initial data.
    @FXML
    public void initialize() {
        createGroupButton.setGraphic(GroupUiUtils.icon("fas-plus", "create-btn-icon"));
        selectTab("groups");
        configureFilters();
        loadGroups();
        updateTabCounters();
    }

    // Wire filter controls to refresh the list.
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

    // Reload groups from service and refresh counters.
    private void loadGroups() {
        try {
            data.setAll(groupService.getAll());
            sortCurrentGroups();
            renderCards();
            updateTabCounters();
        } catch (Exception e) {
            GroupUiUtils.showError(groupCardsPane.getScene().getWindow(), GroupListController.class,
                    "Chargement impossible",
                    "Oups, nous n'avons pas pu charger vos groupes pour le moment.",
                    e.getMessage());
        }
    }

    // Rebuild in-memory list from current search and sort selections.
    private void applyFilters() {
        String keyword = searchField.getText();
        try {
            if (keyword == null || keyword.isBlank()) {
                data.setAll(groupService.getAll());
            } else {
                data.setAll(groupService.search(keyword));
            }
            sortCurrentGroups();
            renderCards();
        } catch (Exception e) {
            GroupUiUtils.showError(groupCardsPane.getScene().getWindow(), GroupListController.class,
                    "Recherche/tri indisponible",
                    "Nous n'avons pas pu appliquer vos filtres. Reessayez dans un instant.",
                    e.getMessage());
        }
    }

    // Sort current in-memory groups according to selected filter.
    private void sortCurrentGroups() {
        String sortBy = sortCombo.getValue();
        Comparator<StudyGroup> comparator;
        Map<Integer, Integer> memberCountCache = new HashMap<>();

        if ("Nom (A-Z)".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(g -> GroupUiUtils.nullSafe(g.getName()), String.CASE_INSENSITIVE_ORDER);
        } else if ("Nom (Z-A)".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing((StudyGroup g) -> GroupUiUtils.nullSafe(g.getName()), String.CASE_INSENSITIVE_ORDER).reversed();
        } else if ("Plus de membres".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparingInt((StudyGroup g) ->
                    memberCountCache.computeIfAbsent(g.getId(), id -> memberService.countMembersForGroup(id))
            ).reversed();
        } else if ("Plus recents".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(StudyGroup::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed();
        } else if ("Activite recente".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(StudyGroup::getLastActivity, Comparator.nullsLast(Comparator.naturalOrder())).reversed();
        } else {
            comparator = Comparator.comparing(g -> GroupUiUtils.nullSafe(g.getName()), String.CASE_INSENSITIVE_ORDER);
        }

        FXCollections.sort(data, comparator);
    }

    // Render filtered groups as cards.
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
        groupsTabCountLabel.setText(String.valueOf(visibleCount));
    }

    // Switch to groups tab.
    @FXML
    private void onGroupsTab() {
        selectTab("groups");
    }

    // Switch to invitations tab.
    @FXML
    private void onInvitationsTab() {
        selectTab("invitations");
    }

    // Switch to feedbacks tab.
    @FXML
    private void onFeedbacksTab() {
        selectTab("feedbacks");
    }

    // Update active tab button and visible content pane.
    private void selectTab(String tab) {
        boolean groupsActive = "groups".equals(tab);
        boolean invitationsActive = "invitations".equals(tab);
        boolean feedbacksActive = "feedbacks".equals(tab);

        setTabActive(groupsTabButton, groupsActive);
        setTabActive(invitationsTabButton, invitationsActive);
        setTabActive(feedbacksTabButton, feedbacksActive);

        groupsContentPane.setVisible(groupsActive);
        groupsContentPane.setManaged(groupsActive);
        invitationsContentPane.setVisible(invitationsActive);
        invitationsContentPane.setManaged(invitationsActive);
        feedbacksContentPane.setVisible(feedbacksActive);
        feedbacksContentPane.setManaged(feedbacksActive);
    }

    // Apply tab active state class.
    private void setTabActive(Button tabButton, boolean active) {
        if (!tabButton.getStyleClass().contains("tab-btn")) {
            tabButton.getStyleClass().add("tab-btn");
        }
        tabButton.getStyleClass().remove("tab-btn-active");
        if (active) {
            tabButton.getStyleClass().add("tab-btn-active");
        }
    }

    // Refresh counters displayed in tab headers.
    private void updateTabCounters() {
        groupsTabCountLabel.setText(String.valueOf(data.size()));
        invitationsTabCountLabel.setText("0");
        feedbacksTabCountLabel.setText("0");
    }

    // Build one group card.
    private VBox buildGroupCard(StudyGroup group, String role) {
        VBox card = new VBox(8);
        card.getStyleClass().add("group-card");
        card.setPrefWidth(360);
        card.setMinHeight(188);

        Label avatar = new Label(GroupUiUtils.initial(group.getName()));
        avatar.getStyleClass().add("group-avatar");

        Label title = new Label(GroupUiUtils.nullSafe(group.getName()));
        title.getStyleClass().add("group-title");
        title.setGraphic(GroupUiUtils.icon(GroupUiUtils.privacyIconLiteral(group.getPrivacy()), "group-privacy-icon"));

        Label roleLabel = new Label(role);
        roleLabel.getStyleClass().add("role-pill");
        roleLabel.getStyleClass().add(roleBadgeClass(role));

        Label subject = new Label("Matiere : " + GroupUiUtils.nullSafe(group.getSubject()));
        subject.getStyleClass().add("group-meta");

        int memberCount = memberService.countMembersForGroup(group.getId());
        Label members = new Label("Membres: " + memberCount);
        members.getStyleClass().add("group-meta");

            Label activity = new Label("Derniere activite : " + GroupUiUtils.formatRelativeTime(group.getLastActivity()));
        activity.getStyleClass().add("group-meta");

        MenuItem openItem = new MenuItem("Ouvrir");
        openItem.setGraphic(GroupUiUtils.icon("fas-folder-open", "detail-menu-icon"));
        openItem.setOnAction(event -> openGroup(group));

        MenuItem editItem = new MenuItem("Modifier");
        editItem.setGraphic(GroupUiUtils.icon("fas-edit", "detail-menu-icon"));
        editItem.setOnAction(event -> onEditSelected(group));

        MenuItem deleteItem = new MenuItem("Supprimer le groupe");
        deleteItem.setGraphic(GroupUiUtils.icon("fas-trash-alt", "detail-menu-danger-icon"));
        deleteItem.getStyleClass().add("danger-item");
        deleteItem.setOnAction(event -> onDeleteSelected(group));
        SeparatorMenuItem deleteDivider = new SeparatorMenuItem();

        Circle dotTop = new Circle(1.7);
        Circle dotMiddle = new Circle(1.7);
        Circle dotBottom = new Circle(1.7);
        VBox dotsIcon = new VBox(2.1, dotTop, dotMiddle, dotBottom);
        dotsIcon.setAlignment(Pos.CENTER);
        dotsIcon.getStyleClass().add("menu-dot-icon");

        MenuButton menu = new MenuButton();
        menu.setGraphic(dotsIcon);
        menu.getStyleClass().add("menu-dots");
        menu.getItems().addAll(openItem, editItem, deleteDivider, deleteItem);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topLine = new HBox(avatar, spacer, menu);
        topLine.getStyleClass().add("card-top-row");

        VBox topRow = new VBox(2, title, roleLabel);
        topRow.getStyleClass().add("group-head");

        VBox cardHead = new VBox(6);
        cardHead.getChildren().addAll(topLine, topRow);

        card.getChildren().addAll(cardHead, subject, members, activity);
        card.setOnMouseClicked(event -> openGroup(group));

        return card;
    }

    // Build static "join group" card.
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

    // Highlight currently selected card.
    private void selectCard(StudyGroup group, VBox selectedCard) {
        selectedGroup = group;
        groupCardsPane.getChildren().forEach(node -> node.getStyleClass().remove("group-card-selected"));
        selectedCard.getStyleClass().add("group-card-selected");
    }

    // Open create dialog and persist a new group.
    @FXML
    private void onCreateGroup() {
        try {
            StudyGroup created = GroupFormController.showDialog(null, createGroupButton.getScene().getWindow());
            if (created == null) {
                return;
            }

            groupService.add(created);
            loadGroups();
            GroupUiUtils.showSuccess(groupCardsPane.getScene().getWindow(), GroupListController.class,
                    "Groupe cree",
                    "Super ! Le groupe a ete ajoute avec succes.");
        } catch (Exception e) {
            GroupUiUtils.showError(groupCardsPane.getScene().getWindow(), GroupListController.class,
                    "Erreur",
                    "Impossible de creer le groupe.",
                    e.getMessage());
        }
    }

    // Open edit dialog and persist updates.
    private void onEditSelected(StudyGroup selected) {
        try {
            StudyGroup updated = GroupFormController.showDialog(selected, groupCardsPane.getScene().getWindow());
            if (updated == null) {
                return;
            }

            groupService.update(updated);
            loadGroups();
            GroupUiUtils.showSuccess(groupCardsPane.getScene().getWindow(), GroupListController.class,
                    "Groupe modifie",
                    "Parfait, vos modifications ont bien ete enregistrees.");
        } catch (Exception e) {
            GroupUiUtils.showError(groupCardsPane.getScene().getWindow(), GroupListController.class,
                    "Erreur",
                    "Impossible de modifier le groupe.",
                    e.getMessage());
        }
    }

    // Confirm and delete selected group.
    private void onDeleteSelected(StudyGroup selected) {
        selectedGroup = selected;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer le groupe");
        confirm.setHeaderText("Confirmer la suppression");
        confirm.setContentText("Voulez-vous supprimer le groupe \"" + selected.getName() + "\" ?\nCette action est irreversible.");

        ButtonType deleteType = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(deleteType, cancelType);
        GroupUiUtils.applyDialogStyle(confirm.getDialogPane(), GroupListController.class);

        Button deleteButton = (Button) confirm.getDialogPane().lookupButton(deleteType);
        if (deleteButton != null) {
            deleteButton.getStyleClass().add("danger-btn");
            deleteButton.setGraphic(GroupUiUtils.icon("fas-trash-alt", "detail-dialog-danger-icon"));
        }

        Button cancelButton = (Button) confirm.getDialogPane().lookupButton(cancelType);
        if (cancelButton != null) {
            cancelButton.setGraphic(GroupUiUtils.icon("fas-times", "detail-dialog-icon"));
        }

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == deleteType) {
            try {
                groupService.delete(selected.getId());
                selectedGroup = null;
                loadGroups();
                GroupUiUtils.showSuccess(groupCardsPane.getScene().getWindow(), GroupListController.class,
                        "Groupe supprime",
                        "C'est fait. Le groupe a bien ete supprime.");
            } catch (Exception e) {
                GroupUiUtils.showError(groupCardsPane.getScene().getWindow(), GroupListController.class,
                        "Erreur",
                        "Impossible de supprimer le groupe.",
                        e.getMessage());
            }
        }
    }

    // Open detail page for selected group.
    private void openGroup(StudyGroup group) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/groupes/GroupDetailView.fxml"));
            Parent root = loader.load();
            GroupDetailController detailController = loader.getController();
            detailController.setGroup(group);

            Stage stage = (Stage) groupCardsPane.getScene().getWindow();
            GroupUiUtils.switchScene(stage, root, GroupUiUtils.nullSafe(group.getName()));
        } catch (IOException e) {
            GroupUiUtils.showError(groupCardsPane.getScene().getWindow(), GroupListController.class,
                    "Navigation impossible",
                    "Impossible d'ouvrir les details du groupe.",
                    e.getMessage());
        }
    }

    // Infer current user role in a group.
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

    // Map role label to CSS badge modifier class.
    private String roleBadgeClass(String role) {
        if (role == null) {
            return "role-pill-member";
        }

        String lower = role.trim().toLowerCase();
        if ("admin".equals(lower)) {
            return "role-pill-admin";
        }
        if ("moderator".equals(lower) || "moderateur".equals(lower)) {
            return "role-pill-moderator";
        }
        return "role-pill-member";
    }

}
