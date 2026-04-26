package com.example.studysprint.modules.groupes.controllers;

import com.example.studysprint.modules.groupes.models.StudyGroup;
import com.example.studysprint.modules.groupes.models.GroupMember;
import com.example.studysprint.modules.groupes.services.GroupMemberService;
import com.example.studysprint.modules.groupes.services.GroupService;
import com.example.studysprint.modules.groupes.utils.GroupUiUtils;
import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.utils.AppNavigator;
import com.example.studysprint.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
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
import java.util.Set;
import java.util.stream.Collectors;

public class GroupListController {
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
    @FXML
    private Label sidebarFullNameLabel;
    @FXML
    private Label sidebarRoleLabel;

    private final GroupService groupService = new GroupService();
    private final GroupMemberService memberService = new GroupMemberService();
    private final ObservableList<StudyGroup> data = FXCollections.observableArrayList();
    private StudyGroup selectedGroup;

    // Prepare the page and load the first group list.
    @FXML
    public void initialize() {
        createGroupButton.setGraphic(GroupUiUtils.icon("fas-plus", "create-btn-icon"));
        populateSidebarUser();
        selectTab("groups");
        configureFilters();
        loadGroups();
        updateTabCounters();
    }

    private void populateSidebarUser() {
        Utilisateur currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }
        sidebarFullNameLabel.setText(currentUser.getFullName());
        sidebarRoleLabel.setText(formatRole(currentUser));
    }

    private void configureFilters() {
        sortCombo.setItems(FXCollections.observableArrayList(
                "Activite recente",
                "Nom (A-Z)",
                "Nom (Z-A)",
                "Plus de membres",
                "Plus recents"));
        sortCombo.getSelectionModel().select("Activite recente");

        roleCombo.setItems(FXCollections.observableArrayList("Tous les roles", "Admin", "Moderateur", "Membre"));
        roleCombo.getSelectionModel().select("Tous les roles");

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        sortCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        roleCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void loadGroups() {
        try {
            data.setAll(filterAccessibleGroups(groupService.getAll()));
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

    private void applyFilters() {
        String keyword = searchField.getText();
        try {
            if (keyword == null || keyword.isBlank()) {
                data.setAll(filterAccessibleGroups(groupService.getAll()));
            } else {
                data.setAll(filterAccessibleGroups(groupService.search(keyword)));
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

    private void sortCurrentGroups() {
        String sortBy = sortCombo.getValue();
        Comparator<StudyGroup> comparator;
        Map<Integer, Integer> memberCountCache = new HashMap<>();

        if ("Nom (A-Z)".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(g -> GroupUiUtils.nullSafe(g.getName()), String.CASE_INSENSITIVE_ORDER);
        } else if ("Nom (Z-A)".equalsIgnoreCase(sortBy)) {
            comparator = Comparator
                    .comparing((StudyGroup g) -> GroupUiUtils.nullSafe(g.getName()), String.CASE_INSENSITIVE_ORDER)
                    .reversed();
        } else if ("Plus de membres".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparingInt((StudyGroup g) -> memberCountCache.computeIfAbsent(g.getId(),
                    id -> memberService.countMembersForGroup(id))).reversed();
        } else if ("Plus recents".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(StudyGroup::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                    .reversed();
        } else if ("Activite recente".equalsIgnoreCase(sortBy)) {
            comparator = Comparator
                    .comparing(StudyGroup::getLastActivity, Comparator.nullsLast(Comparator.naturalOrder())).reversed();
        } else {
            comparator = Comparator.comparing(g -> GroupUiUtils.nullSafe(g.getName()), String.CASE_INSENSITIVE_ORDER);
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
        groupsTabCountLabel.setText(String.valueOf(visibleCount));
    }

    @FXML
    private void onGroupsTab() {
        selectTab("groups");
    }

    @FXML
    private void onInvitationsTab() {
        selectTab("invitations");
    }

    @FXML
    private void onFeedbacksTab() {
        selectTab("feedbacks");
    }

    @FXML
    private void onBackHome() {
        Stage stage = (Stage) groupCardsPane.getScene().getWindow();
        if (!AppNavigator.switchTo(stage, AppNavigator.HOME_FXML, AppNavigator.HOME_TITLE, getClass())) {
            GroupUiUtils.showError(groupCardsPane.getScene().getWindow(), GroupListController.class,
                    "Navigation impossible",
                    "Impossible de retourner vers l'accueil.",
                    "Chargement de la vue échoué.");
        }
    }

    @FXML
    private void onOpenProfile() {
        Stage stage = (Stage) groupCardsPane.getScene().getWindow();
        AppNavigator.switchTo(stage, "/fxml/auth/profile.fxml", "Mon Profil - StudySprint", getClass());
    }

    @FXML
    private void onOpenChangePassword() {
        onOpenProfile();
    }

    @FXML
    private void onLogout() {
        SessionManager.getInstance().logout();
        Stage stage = (Stage) groupCardsPane.getScene().getWindow();
        AppNavigator.switchTo(stage, AppNavigator.LOGIN_FXML, AppNavigator.LOGIN_TITLE, getClass());
    }

    private String formatRole(Utilisateur user) {
        if (user == null || user.getRole() == null) {
            return "Utilisateur";
        }
        return switch (user.getRole().toUpperCase()) {
            case "ROLE_STUDENT" -> "Etudiant";
            case "ROLE_PROFESSOR" -> "Professeur";
            case "ROLE_ADMIN" -> "Administrateur";
            default -> "Utilisateur";
        };
    }

    // Show or hide the content area linked to the selected tab.
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

    private void setTabActive(Button tabButton, boolean active) {
        if (!tabButton.getStyleClass().contains("tab-btn")) {
            tabButton.getStyleClass().add("tab-btn");
        }
        tabButton.getStyleClass().remove("tab-btn-active");
        if (active) {
            tabButton.getStyleClass().add("tab-btn-active");
        }
    }

    private void updateTabCounters() {
        groupsTabCountLabel.setText(String.valueOf(data.size()));
        invitationsTabCountLabel.setText("0");
        feedbacksTabCountLabel.setText("0");
    }

    // Build the visual card used to display one group.
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
        
        menu.getItems().add(openItem);
        
        if ("Admin".equalsIgnoreCase(role) || "Moderateur".equalsIgnoreCase(role)) {
            menu.getItems().add(editItem);
        }
        
        if ("Admin".equalsIgnoreCase(role)) {
            menu.getItems().addAll(deleteDivider, deleteItem);
        }

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

    // Build the card that opens the group creation flow.
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

    // Store the selected group and highlight its card.
    private void selectCard(StudyGroup group, VBox selectedCard) {
        selectedGroup = group;
        groupCardsPane.getChildren().forEach(node -> node.getStyleClass().remove("group-card-selected"));
        selectedCard.getStyleClass().add("group-card-selected");
    }

    @FXML
    private void onCreateGroup() {
        try {
            StudyGroup created = GroupFormController.showDialog(null, createGroupButton.getScene().getWindow());
            if (created == null) {
                return;
            }

            var currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) {
                GroupUiUtils.showWarning(groupCardsPane.getScene().getWindow(), GroupListController.class,
                        "Utilisateur manquant",
                        "Impossible de creer un groupe sans session utilisateur valide.");
                return;
            }

            created.setCreatedById(currentUser.getId());
            int groupId = groupService.add(created);

            GroupMember creatorMember = new GroupMember();
            creatorMember.setGroupId(groupId);
            creatorMember.setUserId(currentUser.getId());
            creatorMember.setMemberRole("admin");
            memberService.add(creatorMember);
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

    private void onDeleteSelected(StudyGroup selected) {
        selectedGroup = selected;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer le groupe");
        confirm.setHeaderText("Confirmer la suppression");
        confirm.setContentText(
                "Voulez-vous supprimer le groupe \"" + selected.getName() + "\" ?\nCette action est irreversible.");

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

    // Open the detail view for the selected group.
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

    // Infer the current user's visible role inside a group.
    // Toujours base sur la table des membres (source de verite), pas sur createdById.
    private String inferRole(StudyGroup group) {
        int currentUserId = currentUserId();

        String roleFromMembership = memberService
                .getMemberRoleForUser(group.getId(), currentUserId)
                .orElse("")
                .trim()
                .toLowerCase();

        if ("admin".equals(roleFromMembership)) {
            return "Admin";
        }
        if ("moderator".equals(roleFromMembership)) {
            return "Moderateur";
        }

        return "Membre";
    }

    private java.util.List<StudyGroup> filterAccessibleGroups(java.util.List<StudyGroup> source) {
        int currentUserId = currentUserId();

        Set<Integer> memberGroupIds = memberService.getAll().stream()
                .filter(member -> member.getUserId() != null && member.getUserId() == currentUserId)
                .map(member -> member.getGroupId())
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        return source.stream()
                .filter(group -> group.getId() != null)
                .filter(group -> memberGroupIds.contains(group.getId()))
                .toList();
    }

    private int currentUserId() {
        Utilisateur currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            return -1;
        }
        return currentUser.getId();
    }

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
