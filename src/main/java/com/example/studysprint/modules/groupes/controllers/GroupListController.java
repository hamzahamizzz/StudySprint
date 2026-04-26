package com.example.studysprint.modules.groupes.controllers;

import com.example.studysprint.modules.groupes.models.*;
import com.example.studysprint.modules.groupes.services.*;
import com.example.studysprint.modules.groupes.utils.GroupUiUtils;
import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.modules.utilisateurs.services.UtilisateurService;
import com.example.studysprint.utils.AppNavigator;
import com.example.studysprint.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class GroupListController {

    private static class FeedbackItem {
        String userName;
        String userInitials;
        String actionText;
        String secondaryText;
        String type; // COMMENT, LIKE, RATING
        String badgeText;
        String authorRole; // admin, moderator, member
        Timestamp date;
        int groupId;
        int postId;
    }
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
    private ScrollPane invitationsContentPane;
    @FXML
    private ScrollPane feedbacksScrollPane;
    @FXML
    private VBox sentInvitationsBox;
    @FXML
    private VBox sentInvitationsContainer;
    @FXML
    private VBox receivedInvitationsBox;
    @FXML
    private VBox receivedInvitationsContainer;
    @FXML
    private VBox noInvitationsBox;
    @FXML
    private Label sentInvitationsTitle;
    @FXML
    private Label receivedInvitationsTitle;
    @FXML
    private VBox feedbacksContentPane;
    @FXML
    private Label sidebarFullNameLabel;
    @FXML
    private Label sidebarRoleLabel;

    private final GroupService groupService = new GroupService();
    private final GroupMemberService memberService = new GroupMemberService();
    private final GroupInvitationService invitationService = new GroupInvitationService();
    private final UtilisateurService utilisateurService = new UtilisateurService();
    private final GroupPostService groupPostService = new GroupPostService();
    private final PostCommentService commentService = new PostCommentService();
    private final PostLikeService likeService = new PostLikeService();
    private final PostRatingService ratingService = new PostRatingService();
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
            GroupUiUtils.showNotifError("Chargement impossible", "Oups, nous n'avons pas pu charger vos groupes pour le moment.");
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
            GroupUiUtils.showNotifError("Erreur de filtrage", "Impossible de filtrer les groupes.");
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
        loadInvitations();
    }

    @FXML
    private void onFeedbacksTab() {
        selectTab("feedbacks");
        loadFeedbacks();
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
        feedbacksScrollPane.setVisible(feedbacksActive);
        feedbacksScrollPane.setManaged(feedbacksActive);
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
        int currentUserId = currentUserId();
        Utilisateur currentUser = SessionManager.getInstance().getCurrentUser();
        long invitationsCount = 0;
        if (currentUser != null) {
            List<GroupInvitation> all = invitationService.getAll();
            long count = all.stream()
                .filter(inv -> (inv.getEmail() != null && inv.getEmail().equalsIgnoreCase(currentUser.getEmail())) || 
                               (inv.getInvitedById() != null && inv.getInvitedById() == currentUserId))
                .count();
            invitationsTabCountLabel.setText(String.valueOf(count));
        }

        if (currentUser != null) {
            int userId = currentUser.getId();
            long feedbackCount = groupPostService.getAll().stream()
                .filter(p -> p.getAuthorId() == userId)
                .mapToLong(p -> commentService.getByPost(p.getId()).stream().filter(c -> c.getAuthorId() != userId).count() +
                               likeService.getByPost(p.getId()).stream().filter(l -> l.getUserId() != userId).count() +
                               ratingService.getByPost(p.getId()).stream().filter(r -> r.getUserId() != userId).count())
                .sum();
            feedbacksTabCountLabel.setText(String.valueOf(feedbackCount));
        }
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

        MenuItem inviteItem = new MenuItem("Inviter des membres");
        inviteItem.setGraphic(GroupUiUtils.icon("fas-user-plus", "detail-menu-icon"));
        inviteItem.setOnAction(event -> onInviteToGroup(group));

        MenuItem leaveItem = new MenuItem("Quitter le groupe");
        leaveItem.setGraphic(GroupUiUtils.icon("fas-sign-out-alt", "detail-menu-danger-icon"));
        leaveItem.getStyleClass().add("danger-item");
        leaveItem.setOnAction(event -> onLeaveGroup(group));

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
            menu.getItems().addAll(editItem, inviteItem);
        }

        if (!"Admin".equalsIgnoreCase(role)) {
            menu.getItems().addAll(new SeparatorMenuItem(), leaveItem);
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

        card.setCursor(javafx.scene.Cursor.HAND);
        card.setOnMouseClicked(e -> onJoinGroup());

        return card;
    }

    @FXML
    private void onJoinGroup() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Rejoindre un groupe");
        dialog.setHeaderText(null);
        dialog.initOwner(groupsContentPane.getScene().getWindow());
        VBox content = new VBox(20);
        content.setPrefWidth(540);
        content.setStyle("-fx-padding: 30; -fx-background-color: white;");

        // --- Section 1: Code d'invitation ---
        VBox codeSection = new VBox(10);
        codeSection.setStyle("-fx-background-color: #f8fafc; -fx-padding: 20; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12;");
        Label codeTitle = new Label("Rejoindre avec un code");
        codeTitle.setStyle("-fx-font-weight: 800; -fx-text-fill: #1e293b; -fx-font-size: 15px;");
        
        HBox codeInputRow = new HBox(10);
        TextField codeField = new TextField();
        codeField.setPromptText("Saisissez le code d'invitation...");
        codeField.getStyleClass().add("field-input");
        HBox.setHgrow(codeField, Priority.ALWAYS);
        
        Button joinBtn = new Button("Rejoindre");
        joinBtn.getStyleClass().add("primary-btn");
        joinBtn.setGraphic(GroupUiUtils.icon("fas-sign-in-alt", "detail-dialog-icon"));
        joinBtn.setOnAction(e -> {
            String code = codeField.getText().trim();
            if (code.isEmpty()) return;
            handleJoinByCode(code, dialog);
        });
        
        codeInputRow.getChildren().addAll(codeField, joinBtn);
        codeSection.getChildren().addAll(codeTitle, codeInputRow);

        // Separator text
        Label separatorLabel = new Label("OU");
        separatorLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: 800; -fx-font-size: 12px; -fx-padding: 10 0;");
        separatorLabel.setAlignment(Pos.CENTER);
        separatorLabel.setMaxWidth(Double.MAX_VALUE);

        // --- Section 2: Rechercher ---
        VBox searchSection = new VBox(8);
        Label searchTitle = new Label("Rechercher");
        searchTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2d3436;");
        
        TextField searchInput = new TextField();
        searchInput.setPromptText("Nom du groupe, matiere...");
        searchInput.setStyle("-fx-padding: 10; -fx-background-radius: 6; -fx-border-color: #dfe6e9; -fx-border-radius: 6;");
        
        searchSection.getChildren().addAll(searchTitle, searchInput);

        // --- Section 3: Groupes disponibles ---
        VBox publicSection = new VBox(10);
        Label publicTitle = new Label("Groupes disponibles");
        publicTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2d3436;");
        
        VBox publicGroupsList = new VBox(12);
        ScrollPane scroll = new ScrollPane(publicGroupsList);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(250);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        // Real-time search
        searchInput.textProperty().addListener((obs, oldVal, newVal) -> {
            loadAvailablePublicGroups(publicGroupsList, dialog, newVal);
        });

        loadAvailablePublicGroups(publicGroupsList, dialog, "");

        publicSection.getChildren().addAll(publicTitle, scroll);

        content.getChildren().addAll(codeSection, separatorLabel, searchSection, publicSection);

        // Footer button
        Button closeBtn = new Button("Fermer");
        closeBtn.getStyleClass().add("compose-cancel-btn");
        closeBtn.setGraphic(GroupUiUtils.icon("fas-times", "detail-dialog-icon"));
        closeBtn.setPrefWidth(120);
        closeBtn.setOnAction(e -> {
            dialog.setResult(null);
            dialog.hide();
        });
        
        HBox footer = new HBox(closeBtn);
        footer.setAlignment(Pos.CENTER);
        footer.setStyle("-fx-padding: 10 0 0 0;");
        content.getChildren().add(footer);

        dialog.getDialogPane().setContent(content);
        // Add a button type but hide it to allow the dialog to close properly
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Node closeNode = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        if (closeNode != null) {
            closeNode.setManaged(false);
            closeNode.setVisible(false);
        }
        GroupUiUtils.applyDialogStyle(dialog.getDialogPane(), GroupListController.class);

        dialog.showAndWait();
    }

    private void handleJoinByCode(String code, Dialog<?> dialog) {
        Utilisateur currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        Optional<GroupInvitation> optInv = invitationService.getAll().stream()
                .filter(inv -> inv.getCode() != null && inv.getCode().equalsIgnoreCase(code))
                .filter(inv -> "pending".equalsIgnoreCase(inv.getStatus()))
                .findFirst();

        if (optInv.isPresent()) {
            GroupInvitation inv = optInv.get();
            if (inv.getEmail() != null && !inv.getEmail().equalsIgnoreCase(currentUser.getEmail())) {
                GroupUiUtils.showNotifWarning("Code non valide", "Ce code d'invitation ne vous est pas destiné.");
                return;
            }

            dialog.setResult(null);
            dialog.hide();
            acceptInvitation(inv);
            GroupUiUtils.showNotifSuccess("Groupe rejoint", "Vous avez rejoint le groupe avec succès.");
        } else {
            GroupUiUtils.showNotifError("Code invalide", "Le code saisi est incorrect ou l'invitation a expiré.");
        }
    }

    private void loadAvailablePublicGroups(VBox container, Dialog<?> dialog, String filter) {
        container.getChildren().clear();
        Utilisateur currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        List<StudyGroup> publicGroups = groupService.filterByPrivacy("public");
        String finalFilter = filter == null ? "" : filter.toLowerCase().trim();
        
        List<StudyGroup> joinable = publicGroups.stream()
                .filter(g -> memberService.getMemberRoleForUser(g.getId(), currentUser.getId()).isEmpty())
                .filter(g -> finalFilter.isEmpty() || 
                             g.getName().toLowerCase().contains(finalFilter) || 
                             (g.getSubject() != null && g.getSubject().toLowerCase().contains(finalFilter)))
                .toList();

        if (joinable.isEmpty()) {
            Label empty = new Label(finalFilter.isEmpty() ? "Aucun groupe public disponible pour le moment." : "Aucun groupe ne correspond à votre recherche.");
            empty.setStyle("-fx-text-fill: #636e72; -fx-font-style: italic; -fx-padding: 10;");
            container.getChildren().add(empty);
            return;
        }

        for (StudyGroup group : joinable) {
            HBox row = new HBox(15);
            row.setStyle("-fx-padding: 15; -fx-background-color: white; -fx-background-radius: 12; " +
                         "-fx-alignment: center-left; -fx-border-color: #f1f5f9; -fx-border-radius: 12; " +
                         "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.03), 8, 0, 0, 4);");
            
            // Avatar (Circular blue)
            Label avatar = new Label(GroupUiUtils.initial(group.getName()));
            avatar.setStyle("-fx-background-color: #eff6ff; -fx-text-fill: #3b82f6; -fx-font-weight: 800; -fx-min-width: 44px; -fx-min-height: 44px; -fx-background-radius: 22; -fx-alignment: center;");
            
            VBox nameBox = new VBox(2);
            Label nameLabel = new Label(group.getName());
            nameLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 14px; -fx-text-fill: #1e293b;");
            
            int memberCount = memberService.countMembersForGroup(group.getId());
            Label metaLabel = new Label(GroupUiUtils.nullSafe(group.getSubject()) + " • " + memberCount + " membres");
            metaLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
            nameBox.getChildren().addAll(nameLabel, metaLabel);
            HBox.setHgrow(nameBox, Priority.ALWAYS);
            
            Button rowJoinBtn = new Button("Rejoindre");
            rowJoinBtn.getStyleClass().add("detail-outline-btn");
            rowJoinBtn.setGraphic(GroupUiUtils.icon("fas-plus", "detail-dialog-icon"));
            rowJoinBtn.setOnAction(e -> {
                dialog.setResult(null);
                dialog.hide();
                joinPublicGroup(group);
            });
            
            row.getChildren().addAll(avatar, nameBox, rowJoinBtn);
            container.getChildren().add(row);
        }
    }

    private void joinPublicGroup(StudyGroup group) {
        Utilisateur currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        GroupMember newMember = new GroupMember();
        newMember.setGroupId(group.getId());
        newMember.setUserId(currentUser.getId());
        newMember.setMemberRole("member");
        newMember.setJoinedAt(new java.sql.Timestamp(System.currentTimeMillis()));
        memberService.add(newMember);

        loadGroups();
        GroupUiUtils.showNotifSuccess("Groupe rejoint", "Vous faites maintenant partie du groupe \"" + group.getName() + "\".");
    }

    // Store the selected group and highlight its card.
    private void selectCard(StudyGroup group, VBox selectedCard) {
        selectedGroup = group;
        groupCardsPane.getChildren().forEach(node -> node.getStyleClass().remove("group-card-selected"));
        selectedCard.getStyleClass().add("group-card-selected");
    }

    private void onLeaveGroup(StudyGroup group) {
        String role = memberService.getMemberRoleForUser(group.getId(), currentUserId()).orElse("member");
        if ("admin".equals(role)) {
            GroupUiUtils.showNotifWarning("Action impossible", "En tant qu'Admin, vous ne pouvez pas quitter le groupe. Vous devez d'abord nommer un autre Admin ou supprimer le groupe.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Quitter le groupe");
        alert.setHeaderText("Quitter \"" + group.getName() + "\" ?");
        alert.setContentText("Êtes-vous sûr de vouloir quitter ce groupe ?");
        alert.initOwner(groupCardsPane.getScene().getWindow());
        GroupUiUtils.applyDialogStyle(alert.getDialogPane(), GroupListController.class);

        Button leaveButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        if (leaveButton != null) {
            leaveButton.getStyleClass().add("danger-btn");
            leaveButton.setGraphic(GroupUiUtils.icon("fas-sign-out-alt", "detail-dialog-danger-icon"));
            leaveButton.setText("Quitter");
        }

        Button cancelButton = (Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancelButton != null) {
            cancelButton.getStyleClass().add("compose-cancel-btn");
            cancelButton.setGraphic(GroupUiUtils.icon("fas-times", "detail-dialog-icon"));
        }

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            memberService.removeByGroupAndUser(group.getId(), currentUserId());
            loadGroups();
            GroupUiUtils.showNotifSuccess("Groupe quitté", "Vous avez quitté le groupe avec succès.");
        }
    }

    private void onInviteToGroup(StudyGroup group) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Inviter des membres");
        dialog.setHeaderText("Inviter à rejoindre \"" + group.getName() + "\"");
        dialog.initOwner(groupCardsPane.getScene().getWindow());

        VBox content = new VBox(15);
        content.setPrefWidth(450);
        content.setStyle("-fx-padding: 20;");

        Label labelEmails = new Label("Emails des membres (un par ligne)");
        labelEmails.setStyle("-fx-font-weight: bold;");
        TextArea emailsArea = new TextArea();
        emailsArea.setPromptText("etudiant1@exemple.com\netudiant2@exemple.com");
        emailsArea.setPrefHeight(120);

        Label labelRole = new Label("Rôle à attribuer");
        labelRole.setStyle("-fx-font-weight: bold;");
        ComboBox<String> roleComboInvite = new ComboBox<>();
        roleComboInvite.getItems().addAll("Membre", "Moderateur");
        roleComboInvite.setValue("Membre");
        roleComboInvite.setMaxWidth(Double.MAX_VALUE);

        content.getChildren().addAll(labelEmails, emailsArea, labelRole, roleComboInvite);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GroupUiUtils.applyDialogStyle(dialog.getDialogPane(), GroupListController.class);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.getStyleClass().add("primary-btn");
            okButton.setGraphic(GroupUiUtils.icon("fas-paper-plane", "detail-dialog-icon"));
            okButton.setText("Inviter");
        }

        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancelButton != null) {
            cancelButton.getStyleClass().add("compose-cancel-btn");
            cancelButton.setGraphic(GroupUiUtils.icon("fas-times", "detail-dialog-icon"));
        }

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String role = roleComboInvite.getValue().equals("Membre") ? "member" : "moderator";
            List<String> emails = Arrays.stream(emailsArea.getText().split("\n"))
                    .map(String::trim)
                    .filter(e -> !e.isEmpty() && e.contains("@"))
                    .toList();

            if (emails.isEmpty()) {
                GroupUiUtils.showNotifWarning("Aucun email", "Veuillez saisir au moins un email valide.");
                return;
            }

            int count = 0;
            for (String email : emails) {
                GroupInvitation inv = new GroupInvitation();
                inv.setGroupId(group.getId());
                inv.setInvitedById(currentUserId());
                inv.setEmail(email);
                inv.setRole(role);
                inv.setStatus("pending");
                inv.setInvitedAt(new Timestamp(System.currentTimeMillis()));
                inv.setCode("INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                invitationService.add(inv);
                count++;
            }

            GroupUiUtils.showNotifSuccess("Invitations envoyées", count + " invitation(s) ont été envoyées.");
        }
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
                GroupUiUtils.showNotifWarning("Utilisateur manquant", "Impossible de creer un groupe sans session utilisateur valide.");
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
            GroupUiUtils.showNotifSuccess("Groupe créé", "Le groupe a été créé avec succès.");
        } catch (Exception e) {
            GroupUiUtils.showNotifError("Erreur de création", "Impossible de créer le groupe : " + e.getMessage());
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
            groupService.update(updated);
            loadGroups();
            GroupUiUtils.showNotifSuccess("Groupe modifié", "Vos modifications ont bien été enregistrées.");
        } catch (Exception e) {
            GroupUiUtils.showNotifError("Erreur de modification", "Impossible de modifier le groupe : " + e.getMessage());
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
            cancelButton.getStyleClass().add("compose-cancel-btn");
            cancelButton.setGraphic(GroupUiUtils.icon("fas-times", "detail-dialog-icon"));
        }

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == deleteType) {
            try {
                groupService.delete(selected.getId());
                selectedGroup = null;
                loadGroups();
                GroupUiUtils.showNotifSuccess("Groupe supprimé", "Le groupe a bien été supprimé.");
            } catch (Exception e) {
                GroupUiUtils.showNotifError("Erreur de suppression", "Impossible de supprimer le groupe : " + e.getMessage());
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
            GroupUiUtils.showNotifError("Navigation impossible", "Impossible d'ouvrir les détails du groupe : " + e.getMessage());
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

    private void loadInvitations() {
        Utilisateur currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;
        int currentUserId = currentUser.getId();

        java.util.List<GroupInvitation> received = invitationService.getAll().stream()
                .filter(inv -> inv.getEmail() != null && inv.getEmail().equalsIgnoreCase(currentUser.getEmail()))
                .toList();

        java.util.List<GroupInvitation> sent = invitationService.getAll().stream()
                .filter(inv -> inv.getInvitedById() != null && inv.getInvitedById() == currentUserId)
                .toList();

        if (receivedInvitationsTitle != null) {
            receivedInvitationsTitle.setText("Invitations reçues (" + received.size() + ")");
            receivedInvitationsBox.getChildren().clear();
            boolean hasReceived = !received.isEmpty();
            receivedInvitationsContainer.setVisible(hasReceived);
            receivedInvitationsContainer.setManaged(hasReceived);
            for (GroupInvitation inv : received) {
                receivedInvitationsBox.getChildren().add(buildReceivedInvitationCard(inv));
            }
        }

        if (sentInvitationsTitle != null) {
            sentInvitationsTitle.setText("Invitations envoyées (" + sent.size() + ")");
            sentInvitationsBox.getChildren().clear();
            boolean hasSent = !sent.isEmpty();
            sentInvitationsContainer.setVisible(hasSent);
            sentInvitationsContainer.setManaged(hasSent);
            for (GroupInvitation inv : sent) {
                sentInvitationsBox.getChildren().add(buildSentInvitationCard(inv));
            }
        }
        
        boolean isEmpty = sent.isEmpty() && received.isEmpty();
        if (noInvitationsBox != null) {
            noInvitationsBox.setVisible(isEmpty);
            noInvitationsBox.setManaged(isEmpty);
        }
        
        updateTabCounters();
    }

    private javafx.scene.layout.HBox buildSentInvitationCard(GroupInvitation inv) {
        StudyGroup group = groupService.getById(inv.getGroupId());
        javafx.scene.layout.HBox card = new javafx.scene.layout.HBox(20);
        card.setStyle("-fx-padding: 20; -fx-alignment: center-left; -fx-background-color: white; " +
                     "-fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5); " +
                     "-fx-border-color: #f1f5f9; -fx-border-radius: 15;");
        card.setMaxWidth(Double.MAX_VALUE);

        String groupName = group != null ? GroupUiUtils.nullSafe(group.getName()) : "Inconnu";
        Label avatar = new Label(GroupUiUtils.initial(groupName));
        avatar.getStyleClass().add("group-avatar");
        avatar.setStyle("-fx-min-width: 40px; -fx-min-height: 40px; -fx-font-size: 16px;");

        javafx.scene.layout.VBox details = new javafx.scene.layout.VBox(4);
        Label emailLabel = new Label(inv.getEmail());
        emailLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2d3436;");
        
        String dateStr = GroupUiUtils.formatRelativeTime(inv.getInvitedAt());
        Label groupLabel = new Label("Groupe " + groupName + " - " + dateStr);
        groupLabel.setStyle("-fx-text-fill: #636e72; -fx-font-size: 12px;");
        
        Label codeLabel = new Label("Code : " + inv.getCode());
        codeLabel.setStyle("-fx-text-fill: #636e72; -fx-font-size: 12px;");
        
        javafx.scene.layout.HBox badges = new javafx.scene.layout.HBox(8);
        Label statusBadge = new Label(formatInvitationStatus(inv.getStatus()));
        statusBadge.setStyle(getInvitationStatusStyle(inv.getStatus()));
        String roleStr = inv.getRole() != null && !inv.getRole().isEmpty() ? (inv.getRole().substring(0, 1).toUpperCase() + inv.getRole().substring(1)) : "Membre";
        Label roleBadge = new Label("Rôle : " + roleStr);
        roleBadge.setStyle("-fx-background-color: #f1f2f6; -fx-text-fill: #2f3542; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-size: 11px;");
        badges.getChildren().addAll(statusBadge, roleBadge);
        
        details.getChildren().addAll(emailLabel, groupLabel, codeLabel, badges);

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(10);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        
        if ("pending".equalsIgnoreCase(inv.getStatus())) {
            Button cancelBtn = new Button("Annuler");
            cancelBtn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #d63031; -fx-cursor: hand; -fx-font-size: 13px;");
            cancelBtn.setOnAction(e -> cancelInvitation(inv));
            actions.getChildren().add(cancelBtn);
        }
        
        Button viewGroupBtn = new Button("Voir le groupe");
        viewGroupBtn.setStyle("-fx-background-color: white; -fx-border-color: #dfe6e9; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #2d3436; -fx-padding: 6 12; -fx-cursor: hand;");
        viewGroupBtn.setOnAction(e -> {
            if (group != null) openGroup(group);
        });
        actions.getChildren().add(viewGroupBtn);

        card.getChildren().addAll(avatar, details, spacer, actions);
        return card;
    }

    private javafx.scene.layout.HBox buildReceivedInvitationCard(GroupInvitation inv) {
        StudyGroup group = groupService.getById(inv.getGroupId());
        com.example.studysprint.modules.utilisateurs.services.UtilisateurService.UserDisplay sender = inv.getInvitedById() != null ? utilisateurService.getDisplay(inv.getInvitedById()) : null;
        
        javafx.scene.layout.HBox card = new javafx.scene.layout.HBox(20);
        card.setStyle("-fx-padding: 20; -fx-alignment: center-left; -fx-background-color: white; " +
                     "-fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5); " +
                     "-fx-border-color: #f1f5f9; -fx-border-radius: 15;");
        card.setMaxWidth(Double.MAX_VALUE);

        String groupName = group != null ? GroupUiUtils.nullSafe(group.getName()) : "Inconnu";
        Label avatar = new Label(GroupUiUtils.initial(groupName));
        avatar.getStyleClass().add("group-avatar");
        avatar.setStyle("-fx-min-width: 40px; -fx-min-height: 40px; -fx-font-size: 16px;");

        javafx.scene.layout.VBox details = new javafx.scene.layout.VBox(4);
        Label emailLabel = new Label(inv.getEmail());
        emailLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2d3436;");
        
        String dateStr = GroupUiUtils.formatRelativeTime(inv.getInvitedAt());
        String senderName = sender != null ? "Invité par " + sender.fullName() : "Invité";
        Label groupLabel = new Label("Groupe " + groupName + " - " + senderName + " - " + dateStr);
        groupLabel.setStyle("-fx-text-fill: #636e72; -fx-font-size: 12px;");
        
        javafx.scene.layout.HBox badges = new javafx.scene.layout.HBox(8);
        Label statusBadge = new Label(formatInvitationStatus(inv.getStatus()));
        statusBadge.setStyle(getInvitationStatusStyle(inv.getStatus()));
        String roleStr = inv.getRole() != null && !inv.getRole().isEmpty() ? (inv.getRole().substring(0, 1).toUpperCase() + inv.getRole().substring(1)) : "Membre";
        Label roleBadge = new Label("Rôle : " + roleStr);
        roleBadge.setStyle("-fx-background-color: #f1f2f6; -fx-text-fill: #2f3542; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-size: 11px;");
        badges.getChildren().addAll(statusBadge, roleBadge);
        
        details.getChildren().addAll(emailLabel, groupLabel, badges);

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(10);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        
        if ("pending".equalsIgnoreCase(inv.getStatus())) {
            Button refuseBtn = new Button("Refuser");
            refuseBtn.setStyle("-fx-background-color: white; -fx-border-color: #dfe6e9; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #2d3436; -fx-padding: 6 12; -fx-cursor: hand;");
            refuseBtn.setOnAction(e -> refuseInvitation(inv));
            
            Button acceptBtn = new Button("Accepter");
            acceptBtn.setStyle("-fx-background-color: #0984e3; -fx-text-fill: white; -fx-background-radius: 4; -fx-padding: 6 12; -fx-cursor: hand;");
            acceptBtn.setOnAction(e -> acceptInvitation(inv));
            
            actions.getChildren().addAll(refuseBtn, acceptBtn);
        } else {
            Button viewGroupBtn = new Button("Voir le groupe");
            viewGroupBtn.setStyle("-fx-background-color: white; -fx-border-color: #dfe6e9; -fx-border-radius: 4; -fx-background-radius: 4; -fx-text-fill: #2d3436; -fx-padding: 6 12; -fx-cursor: hand;");
            viewGroupBtn.setOnAction(e -> {
                if (group != null) openGroup(group);
            });
            actions.getChildren().add(viewGroupBtn);
        }

        card.getChildren().addAll(avatar, details, spacer, actions);
        return card;
    }

    private String formatInvitationStatus(String status) {
        if (status == null) return "Inconnu";
        return switch (status.toLowerCase()) {
            case "pending" -> "En attente";
            case "accepted" -> "Acceptée";
            case "rejected" -> "Refusée";
            case "cancelled" -> "Annulée";
            default -> status;
        };
    }
    
    private String getInvitationStatusStyle(String status) {
        if (status == null) return "-fx-background-color: #f1f2f6; -fx-text-fill: #2f3542; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-size: 11px;";
        return switch (status.toLowerCase()) {
            case "pending" -> "-fx-background-color: #fff3e0; -fx-text-fill: #e67e22; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-size: 11px;";
            case "accepted" -> "-fx-background-color: #e8f8f5; -fx-text-fill: #2ecc71; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-size: 11px;";
            case "rejected", "cancelled" -> "-fx-background-color: #fce4e4; -fx-text-fill: #e74c3c; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-size: 11px;";
            default -> "-fx-background-color: #f1f2f6; -fx-text-fill: #2f3542; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-size: 11px;";
        };
    }

    private void acceptInvitation(GroupInvitation inv) {
        Utilisateur currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;
        
        invitationService.updateStatus(inv.getId(), "accepted");
        
        GroupMember newMember = new GroupMember();
        newMember.setGroupId(inv.getGroupId());
        newMember.setUserId(currentUser.getId());
        newMember.setMemberRole(inv.getRole());
        newMember.setJoinedAt(new java.sql.Timestamp(System.currentTimeMillis()));
        memberService.add(newMember);
        
        loadInvitations();
        loadGroups();
    }

    private void loadFeedbacks() {
        Utilisateur currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        feedbacksContentPane.getChildren().clear();

        List<GroupPost> myPosts = groupPostService.getAll().stream()
                .filter(p -> p.getAuthorId() == currentUser.getId())
                .toList();

        List<FeedbackItem> items = new ArrayList<>();
        int userId = currentUser.getId();

        for (GroupPost post : myPosts) {
            StudyGroup group = groupService.getById(post.getGroupId());
            String groupName = group != null ? group.getName() : "Inconnu";

            // Comments
            commentService.getByPost(post.getId()).stream()
                    .filter(c -> c.getAuthorId() != userId)
                    .forEach(c -> {
                        FeedbackItem item = new FeedbackItem();
                        Utilisateur author = utilisateurService.getById(c.getAuthorId());
                        item.userName = author != null ? author.getFullName() : "Utilisateur";
                        item.userInitials = GroupUiUtils.initial(item.userName);
                        item.authorRole = memberService.getMemberRoleForUser(post.getGroupId(), c.getAuthorId()).orElse("member");
                        item.actionText = "A commenté votre post · " + groupName;
                        item.secondaryText = c.getBody();
                        item.type = "COMMENT";
                        item.badgeText = "Commentaire";
                        item.date = c.getCreatedAt();
                        item.groupId = post.getGroupId();
                        item.postId = post.getId();
                        items.add(item);
                    });

            // Likes
            likeService.getByPost(post.getId()).stream()
                    .filter(l -> l.getUserId() != userId)
                    .forEach(l -> {
                        FeedbackItem item = new FeedbackItem();
                        Utilisateur author = utilisateurService.getById(l.getUserId());
                        item.userName = author != null ? author.getFullName() : "Utilisateur";
                        item.userInitials = GroupUiUtils.initial(item.userName);
                        item.authorRole = memberService.getMemberRoleForUser(post.getGroupId(), l.getUserId()).orElse("member");
                        item.actionText = "A aimé votre post · " + groupName;
                        item.type = "LIKE";
                        item.badgeText = "J'aime";
                        item.date = l.getCreatedAt();
                        item.groupId = post.getGroupId();
                        item.postId = post.getId();
                        items.add(item);
                    });

            // Ratings
            ratingService.getByPost(post.getId()).stream()
                    .filter(r -> r.getUserId() != userId)
                    .forEach(r -> {
                        FeedbackItem item = new FeedbackItem();
                        Utilisateur author = utilisateurService.getById(r.getUserId());
                        item.userName = author != null ? author.getFullName() : "Utilisateur";
                        item.userInitials = GroupUiUtils.initial(item.userName);
                        item.authorRole = memberService.getMemberRoleForUser(post.getGroupId(), r.getUserId()).orElse("member");
                        item.actionText = "A noté votre post · " + groupName;
                        item.type = "RATING";
                        item.badgeText = r.getRating() + "/5";
                        item.date = r.getCreatedAt();
                        item.groupId = post.getGroupId();
                        item.postId = post.getId();
                        items.add(item);
                    });
        }

        items.sort(Comparator.comparing(f -> f.date, Comparator.reverseOrder()));

        if (items.isEmpty()) {
            Label empty = new Label("Aucun feedback reçu pour le moment.");
            empty.setStyle("-fx-text-fill: #636e72; -fx-padding: 40; -fx-font-size: 14px; -fx-alignment: center;");
            feedbacksContentPane.getChildren().add(empty);
            feedbacksContentPane.setStyle("-fx-background-color: transparent;");
        } else {
            feedbacksContentPane.setSpacing(12);
            for (FeedbackItem item : items) {
                feedbacksContentPane.getChildren().add(renderFeedbackRow(item));
            }
        }
        
        feedbacksTabCountLabel.setText(String.valueOf(items.size()));
    }

    private Node renderFeedbackRow(FeedbackItem item) {
        HBox row = new HBox(20);
        row.setStyle("-fx-padding: 20; -fx-alignment: center-left; -fx-background-color: white; " +
                     "-fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5); " +
                     "-fx-border-color: #f1f5f9; -fx-border-radius: 15;");

        // Avatar
        Label avatar = new Label(item.userInitials);
        avatar.getStyleClass().add("detail-small-avatar");
        
        String bgColor = switch (item.authorRole.toLowerCase()) {
            case "admin" -> "#3b82f6";
            case "moderator", "moderateur" -> "#d9ecff";
            default -> "#eff6ff";
        };
        String textColor = switch (item.authorRole.toLowerCase()) {
            case "admin" -> "#ffffff";
            case "moderator", "moderateur" -> "#2563eb";
            default -> "#3b82f6";
        };
        
        avatar.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: " + textColor + "; -fx-font-weight: 800; -fx-font-size: 13px; -fx-min-width: 44px; -fx-min-height: 44px; -fx-background-radius: 22; -fx-alignment: center;");

        // Content Area
        VBox content = new VBox(4);
        Label name = new Label(item.userName);
        name.setStyle("-fx-text-fill: #1e293b; -fx-font-weight: 700; -fx-font-size: 14px;");
        
        Label action = new Label(item.actionText);
        action.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");
        
        content.getChildren().addAll(name, action);
        
        if (item.secondaryText != null && !item.secondaryText.isBlank()) {
            Label secondary = new Label(item.secondaryText);
            secondary.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px; -fx-padding: 4 0 0 0;");
            content.getChildren().add(secondary);
        }
        
        HBox.setHgrow(content, Priority.ALWAYS);

        // Right side (Badge & Time)
        VBox right = new VBox(8);
        right.setAlignment(Pos.CENTER_RIGHT);
        
        Label badge = new Label(item.badgeText);
        String badgeStyle = switch (item.type) {
            case "COMMENT" -> "-fx-background-color: #dbeafe; -fx-text-fill: #1e40af;";
            case "LIKE" -> "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b;";
            case "RATING" -> "-fx-background-color: #fef3c7; -fx-text-fill: #92400e;";
            default -> "-fx-background-color: #f1f5f9; -fx-text-fill: #475569;";
        };
        badge.setStyle(badgeStyle + " -fx-font-size: 11px; -fx-padding: 3 12; -fx-background-radius: 99; -fx-font-weight: 600;");
        
        Label time = new Label(GroupUiUtils.formatRelativeTime(item.date));
        time.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        
        right.getChildren().addAll(badge, time);

        row.getChildren().addAll(avatar, content, right);
        return row;
    }
    
    private void refuseInvitation(GroupInvitation inv) {
        invitationService.updateStatus(inv.getId(), "rejected");
        loadInvitations();
    }
    
    private void cancelInvitation(GroupInvitation inv) {
        invitationService.updateStatus(inv.getId(), "cancelled");
        loadInvitations();
    }

}
