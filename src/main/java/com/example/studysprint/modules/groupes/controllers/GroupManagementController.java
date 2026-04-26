package com.example.studysprint.modules.groupes.controllers;

import com.example.studysprint.modules.groupes.models.GroupMember;
import com.example.studysprint.modules.groupes.models.GroupPost;
import com.example.studysprint.modules.groupes.models.StudyGroup;
import com.example.studysprint.modules.groupes.services.GroupMemberService;
import com.example.studysprint.modules.groupes.services.GroupPostService;
import com.example.studysprint.modules.groupes.services.GroupService;
import com.example.studysprint.modules.groupes.utils.GroupUiUtils;
import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.modules.utilisateurs.services.UtilisateurService;
import com.example.studysprint.utils.SessionManager;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Window;

import java.net.URL;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

public class GroupManagementController implements Initializable {

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> privacyFilter;
    @FXML
    private ComboBox<String> sortFilter;
    @FXML
    private ComboBox<String> orderFilter;
    @FXML
    private Label groupsCountLabel;

    @FXML
    private TableView<StudyGroup> groupsTable;
    @FXML
    private TableColumn<StudyGroup, String> groupsNameCol;
    @FXML
    private TableColumn<StudyGroup, String> groupsSubjectCol;
    @FXML
    private TableColumn<StudyGroup, String> groupsPrivacyCol;
    @FXML
    private TableColumn<StudyGroup, String> groupsCreatorCol;
    @FXML
    private TableColumn<StudyGroup, Number> groupsMembersCol;
    @FXML
    private TableColumn<StudyGroup, Number> groupsPostsCol;
    @FXML
    private TableColumn<StudyGroup, String> groupsActivityCol;

    @FXML
    private Label selectedGroupLabel;
    @FXML
    private Label membersCountLabel;
    @FXML
    private Label postsCountLabel;
    @FXML
    private TextField memberSearchField;
    @FXML
    private ComboBox<String> memberSortFilter;
    @FXML
    private TextField postSearchField;
    @FXML
    private ComboBox<String> postSortFilter;

    @FXML
    private TableView<GroupMember> membersTable;
    @FXML
    private TableColumn<GroupMember, String> memberUserCol;
    @FXML
    private TableColumn<GroupMember, String> memberRoleCol;
    @FXML
    private TableColumn<GroupMember, String> memberJoinedCol;

    @FXML
    private TableView<GroupPost> postsTable;
    @FXML
    private TableColumn<GroupPost, String> postTypeCol;
    @FXML
    private TableColumn<GroupPost, String> postTitleCol;
    @FXML
    private TableColumn<GroupPost, String> postAuthorCol;
    @FXML
    private TableColumn<GroupPost, String> postCreatedCol;

    private final GroupService groupService = new GroupService();
    private final GroupMemberService memberService = new GroupMemberService();
    private final GroupPostService postService = new GroupPostService();
    private final UtilisateurService userService = new UtilisateurService();

    private final ObservableList<StudyGroup> groupsMaster = FXCollections.observableArrayList();
    private final ObservableList<GroupMember> membersMaster = FXCollections.observableArrayList();
    private final ObservableList<GroupPost> postsMaster = FXCollections.observableArrayList();
    private final ObservableList<GroupMember> membersData = FXCollections.observableArrayList();
    private final ObservableList<GroupPost> postsData = FXCollections.observableArrayList();

    private final Map<Integer, Long> memberCountByGroup = new HashMap<>();
    private final Map<Integer, Long> postCountByGroup = new HashMap<>();
    private final Map<Integer, Utilisateur> usersById = new HashMap<>();

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private StudyGroup selectedGroup;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTables();
        setupFilters();
        setupDetailFilters();
        loadUsersIndex();
        loadGroups();

        groupsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedGroup = newVal;
            loadSelectedGroupDetails();
        });

        clearRightPanel();
    }

    private void setupTables() {
        groupsNameCol.setCellValueFactory(cell -> new SimpleStringProperty(nullSafe(cell.getValue().getName())));
        groupsSubjectCol.setCellValueFactory(cell -> new SimpleStringProperty(nullSafe(cell.getValue().getSubject())));
        groupsPrivacyCol
                .setCellValueFactory(cell -> new SimpleStringProperty(formatPrivacy(cell.getValue().getPrivacy())));
        groupsCreatorCol.setCellValueFactory(
                cell -> new SimpleStringProperty(displayUserName(cell.getValue().getCreatedById())));
        groupsMembersCol.setCellValueFactory(cell -> new SimpleIntegerProperty(countMembers(cell.getValue().getId())));
        groupsPostsCol.setCellValueFactory(cell -> new SimpleIntegerProperty(countPosts(cell.getValue().getId())));
        groupsActivityCol.setCellValueFactory(
                cell -> new SimpleStringProperty(formatTimestamp(cell.getValue().getLastActivity())));

        memberUserCol
                .setCellValueFactory(cell -> new SimpleStringProperty(displayUserName(cell.getValue().getUserId())));
        memberRoleCol.setCellValueFactory(
                cell -> new SimpleStringProperty(formatMemberRole(cell.getValue().getMemberRole())));
        memberJoinedCol
                .setCellValueFactory(cell -> new SimpleStringProperty(formatTimestamp(cell.getValue().getJoinedAt())));

        postTypeCol
                .setCellValueFactory(cell -> new SimpleStringProperty(formatPostType(cell.getValue().getPostType())));
        postTitleCol.setCellValueFactory(cell -> new SimpleStringProperty(displayPostTitle(cell.getValue())));
        postAuthorCol
                .setCellValueFactory(cell -> new SimpleStringProperty(displayUserName(cell.getValue().getAuthorId())));
        postCreatedCol
                .setCellValueFactory(cell -> new SimpleStringProperty(formatTimestamp(cell.getValue().getCreatedAt())));

        membersTable.setItems(membersData);
        postsTable.setItems(postsData);
    }

    private void setupFilters() {
        privacyFilter.setItems(FXCollections.observableArrayList("Tous", "Public", "Privé"));
        privacyFilter.getSelectionModel().select("Tous");

        sortFilter.setItems(FXCollections.observableArrayList("Activité", "Nom", "Date création", "Membres", "Posts"));
        sortFilter.getSelectionModel().select("Activité");

        orderFilter.setItems(FXCollections.observableArrayList("Décroissant", "Croissant"));
        orderFilter.getSelectionModel().select("Décroissant");

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyGroupFilters());
        privacyFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyGroupFilters());
        sortFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyGroupFilters());
        orderFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyGroupFilters());
    }

    private void setupDetailFilters() {
        memberSortFilter.setItems(FXCollections.observableArrayList("Date récent", "Nom", "Rôle"));
        memberSortFilter.getSelectionModel().select("Date récent");
        memberSearchField.textProperty().addListener((obs, oldVal, newVal) -> applyMemberFilters());
        memberSortFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyMemberFilters());

        postSortFilter.setItems(FXCollections.observableArrayList("Date récent", "Titre", "Auteur"));
        postSortFilter.getSelectionModel().select("Date récent");
        postSearchField.textProperty().addListener((obs, oldVal, newVal) -> applyPostFilters());
        postSortFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyPostFilters());
    }

    private void loadUsersIndex() {
        usersById.clear();
        for (Utilisateur user : userService.getAll()) {
            usersById.put(user.getId(), user);
        }
    }

    private void loadGroups() {
        try {
            refreshGroupStats();
            groupsMaster.setAll(groupService.getAll());
            applyGroupFilters();
            refreshSelectedGroupAfterReload();
        } catch (Exception ex) {
            showError("Chargement impossible", "Impossible de charger les groupes.", ex.getMessage());
        }
    }

    private void refreshGroupStats() {
        memberCountByGroup.clear();
        postCountByGroup.clear();

        for (GroupMember member : memberService.getAll()) {
            if (member.getGroupId() != null) {
                memberCountByGroup.merge(member.getGroupId(), 1L, Long::sum);
            }
        }

        for (GroupPost post : postService.getAll()) {
            if (post.getGroupId() != null) {
                postCountByGroup.merge(post.getGroupId(), 1L, Long::sum);
            }
        }
    }

    private void applyGroupFilters() {
        String query = lower(searchField.getText());
        String privacyValue = privacyFilter.getValue();

        Comparator<StudyGroup> comparator = comparatorFor(sortFilter.getValue());
        if ("Décroissant".equalsIgnoreCase(orderFilter.getValue())) {
            comparator = comparator.reversed();
        }

        List<StudyGroup> filtered = groupsMaster.stream()
                .filter(group -> filterByQuery(group, query))
                .filter(group -> filterByPrivacy(group, privacyValue))
                .sorted(comparator)
                .toList();

        groupsTable.setItems(FXCollections.observableArrayList(filtered));
        groupsCountLabel.setText(filtered.size() + " groupe" + (filtered.size() > 1 ? "s" : ""));
    }

    private void refreshSelectedGroupAfterReload() {
        if (selectedGroup == null || selectedGroup.getId() == null) {
            return;
        }

        Integer targetId = selectedGroup.getId();
        StudyGroup reloaded = groupsTable.getItems().stream()
                .filter(group -> targetId.equals(group.getId()))
                .findFirst()
                .orElse(null);

        if (reloaded == null) {
            selectedGroup = null;
            groupsTable.getSelectionModel().clearSelection();
            clearRightPanel();
            return;
        }

        groupsTable.getSelectionModel().select(reloaded);
        selectedGroup = reloaded;
        loadSelectedGroupDetails();
    }

    private void loadSelectedGroupDetails() {
        if (selectedGroup == null || selectedGroup.getId() == null) {
            clearRightPanel();
            return;
        }

        selectedGroupLabel.setText(nullSafe(selectedGroup.getName()));

        try {
            membersMaster.setAll(memberService.getByGroup(selectedGroup.getId()));
            postsMaster.setAll(postService.getByGroup(selectedGroup.getId()));
            applyMemberFilters();
            applyPostFilters();
        } catch (Exception ex) {
            showError("Chargement impossible", "Impossible de charger les détails du groupe.", ex.getMessage());
        }
    }

    private void applyMemberFilters() {
        String query = lower(memberSearchField.getText());
        Comparator<GroupMember> comparator = comparatorForMembers(memberSortFilter.getValue());

        List<GroupMember> filtered = membersMaster.stream()
                .filter(member -> filterMember(member, query))
                .sorted(comparator)
                .toList();

        membersData.setAll(filtered);
        membersCountLabel.setText(filtered.size() + " membre" + (filtered.size() > 1 ? "s" : ""));
    }

    private void applyPostFilters() {
        String query = lower(postSearchField.getText());
        Comparator<GroupPost> comparator = comparatorForPosts(postSortFilter.getValue());

        List<GroupPost> filtered = postsMaster.stream()
                .filter(post -> filterPost(post, query))
                .sorted(comparator)
                .toList();

        postsData.setAll(filtered);
        postsCountLabel.setText(filtered.size() + " post" + (filtered.size() > 1 ? "s" : ""));
    }

    private void clearRightPanel() {
        selectedGroupLabel.setText("Aucun");
        membersMaster.clear();
        postsMaster.clear();
        membersData.clear();
        postsData.clear();
        if (memberSearchField != null) {
            memberSearchField.clear();
        }
        if (postSearchField != null) {
            postSearchField.clear();
        }
        membersCountLabel.setText("0 membre");
        postsCountLabel.setText("0 post");
    }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        privacyFilter.getSelectionModel().select("Tous");
        sortFilter.getSelectionModel().select("Activité");
        orderFilter.getSelectionModel().select("Décroissant");
        applyGroupFilters();
    }

    @FXML
    private void handleAddGroup() {
        StudyGroup created = GroupFormController.showDialog(null, ownerWindow());
        if (created == null) {
            return;
        }

        if (created.getCreatedById() == null) {
            var current = SessionManager.getInstance().getCurrentUser();
            created.setCreatedById(current != null ? current.getId() : null);
        }

        try {
            int groupId = groupService.add(created);

            var current = SessionManager.getInstance().getCurrentUser();
            if (current != null) {
                GroupMember creatorMember = new GroupMember();
                creatorMember.setGroupId(groupId);
                creatorMember.setUserId(current.getId());
                creatorMember.setMemberRole("admin");
                memberService.add(creatorMember);
            }

            loadGroups();
            showInfo("Groupe ajouté", "Le groupe a été créé avec succès.");
        } catch (Exception ex) {
            showError("Ajout impossible", "Impossible d'ajouter le groupe.", ex.getMessage());
        }
    }

    @FXML
    private void handleEditGroup() {
        if (!ensureGroupSelected()) {
            return;
        }

        StudyGroup updated = GroupFormController.showDialog(selectedGroup, ownerWindow());
        if (updated == null) {
            return;
        }

        try {
            groupService.update(updated);
            loadGroups();
            showInfo("Groupe modifié", "Les informations du groupe ont été mises à jour.");
        } catch (Exception ex) {
            showError("Mise à jour impossible", "Impossible de modifier le groupe.", ex.getMessage());
        }
    }

    @FXML
    private void handleDeleteGroup() {
        if (!ensureGroupSelected()) {
            return;
        }

        if (!confirmAction("Supprimer le groupe", "Voulez-vous vraiment supprimer ce groupe ?")) {
            return;
        }

        try {
            groupService.delete(selectedGroup.getId());
            selectedGroup = null;
            loadGroups();
            showInfo("Groupe supprimé", "Le groupe a été supprimé.");
        } catch (Exception ex) {
            showError("Suppression impossible", "Impossible de supprimer le groupe.", ex.getMessage());
        }
    }

    @FXML
    private void handleAddMember() {
        if (!ensureGroupSelected()) {
            return;
        }

        Set<Integer> existingMemberIds = membersData.stream()
                .map(GroupMember::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        List<Utilisateur> candidates = usersById.values().stream()
                .filter(user -> !existingMemberIds.contains(user.getId()))
                .sorted(Comparator.comparing(Utilisateur::getFullName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        if (candidates.isEmpty()) {
            showWarning("Aucun utilisateur disponible", "Tous les utilisateurs sont déjà membres de ce groupe.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un membre");
        dialog.setHeaderText("Sélectionnez un utilisateur et un rôle.");
        dialog.initOwner(ownerWindow());

        ComboBox<Utilisateur> userCombo = new ComboBox<>(FXCollections.observableArrayList(candidates));
        userCombo.setPromptText("Utilisateur");
        userCombo.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> roleCombo = new ComboBox<>(FXCollections.observableArrayList("member", "moderator", "admin"));
        roleCombo.getSelectionModel().select("member");
        roleCombo.setMaxWidth(Double.MAX_VALUE);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Utilisateur"), 0, 0);
        grid.add(userCombo, 1, 0);
        grid.add(new Label("Rôle"), 0, 1);
        grid.add(roleCombo, 1, 1);
        GridPane.setHgrow(userCombo, Priority.ALWAYS);
        GridPane.setHgrow(roleCombo, Priority.ALWAYS);

        DialogPane pane = dialog.getDialogPane();
        pane.setContent(grid);
        styleDialog(pane);

        ButtonType saveType = new ButtonType("Ajouter", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != saveType) {
            return;
        }

        Utilisateur selectedUser = userCombo.getValue();
        if (selectedUser == null) {
            showWarning("Utilisateur requis", "Veuillez choisir un utilisateur.");
            return;
        }

        GroupMember member = new GroupMember();
        member.setGroupId(selectedGroup.getId());
        member.setUserId(selectedUser.getId());
        member.setMemberRole(roleCombo.getValue());

        try {
            memberService.add(member);
            loadGroups();
            showInfo("Membre ajouté", "Le membre a été ajouté au groupe.");
        } catch (Exception ex) {
            showError("Ajout impossible", "Impossible d'ajouter ce membre.", ex.getMessage());
        }
    }

    @FXML
    private void handleUpdateMemberRole() {
        if (!ensureGroupSelected()) {
            return;
        }

        GroupMember selectedMember = membersTable.getSelectionModel().getSelectedItem();
        if (selectedMember == null) {
            showWarning("Membre requis", "Sélectionnez un membre dans la table.");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(
                normalizeRole(selectedMember.getMemberRole()),
                FXCollections.observableArrayList("member", "moderator", "admin"));
        dialog.setTitle("Changer le rôle");
        dialog.setHeaderText("Choisissez le nouveau rôle du membre.");
        dialog.setContentText("Rôle:");
        dialog.initOwner(ownerWindow());
        styleDialog(dialog.getDialogPane());

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        try {
            memberService.updateRole(selectedMember.getId(), result.get());
            loadGroups();
            showInfo("Rôle mis à jour", "Le rôle du membre a été modifié.");
        } catch (Exception ex) {
            showError("Mise à jour impossible", "Impossible de modifier le rôle.", ex.getMessage());
        }
    }

    @FXML
    private void handleRemoveMember() {
        if (!ensureGroupSelected()) {
            return;
        }

        GroupMember selectedMember = membersTable.getSelectionModel().getSelectedItem();
        if (selectedMember == null) {
            showWarning("Membre requis", "Sélectionnez un membre dans la table.");
            return;
        }

        if (!confirmAction("Retirer un membre", "Voulez-vous retirer ce membre du groupe ?")) {
            return;
        }

        try {
            memberService.delete(selectedMember.getId());
            loadGroups();
            showInfo("Membre retiré", "Le membre a été retiré du groupe.");
        } catch (Exception ex) {
            showError("Suppression impossible", "Impossible de retirer ce membre.", ex.getMessage());
        }
    }

    @FXML
    private void handleAddPost() {
        if (!ensureGroupSelected()) {
            return;
        }

        GroupPost post = showPostDialog(null);
        if (post == null) {
            return;
        }

        post.setGroupId(selectedGroup.getId());

        try {
            postService.add(post);
            loadGroups();
            showInfo("Post ajouté", "Le post a été créé avec succès.");
        } catch (Exception ex) {
            showError("Ajout impossible", "Impossible d'ajouter ce post.", ex.getMessage());
        }
    }

    @FXML
    private void handleEditPost() {
        if (!ensureGroupSelected()) {
            return;
        }

        GroupPost selectedPost = postsTable.getSelectionModel().getSelectedItem();
        if (selectedPost == null) {
            showWarning("Post requis", "Sélectionnez un post dans la table.");
            return;
        }

        GroupPost edited = showPostDialog(selectedPost);
        if (edited == null) {
            return;
        }

        edited.setGroupId(selectedGroup.getId());

        try {
            postService.update(edited);
            loadGroups();
            showInfo("Post modifié", "Le post a été mis à jour.");
        } catch (Exception ex) {
            showError("Mise à jour impossible", "Impossible de modifier ce post.", ex.getMessage());
        }
    }

    @FXML
    private void handleDeletePost() {
        if (!ensureGroupSelected()) {
            return;
        }

        GroupPost selectedPost = postsTable.getSelectionModel().getSelectedItem();
        if (selectedPost == null) {
            showWarning("Post requis", "Sélectionnez un post dans la table.");
            return;
        }

        if (!confirmAction("Supprimer le post", "Voulez-vous vraiment supprimer ce post ?")) {
            return;
        }

        try {
            postService.delete(selectedPost.getId());
            loadGroups();
            showInfo("Post supprimé", "Le post a été supprimé.");
        } catch (Exception ex) {
            showError("Suppression impossible", "Impossible de supprimer ce post.", ex.getMessage());
        }
    }

    private GroupPost showPostDialog(GroupPost existing) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Ajouter un post" : "Modifier le post");
        dialog.setHeaderText(
                existing == null ? "Créer un nouveau post pour ce groupe." : "Mettre à jour le post sélectionné.");
        dialog.initOwner(ownerWindow());

        ComboBox<String> typeCombo = new ComboBox<>(FXCollections.observableArrayList("text", "file", "link"));
        typeCombo.getSelectionModel().select(existing == null ? "text" : normalizePostType(existing.getPostType()));
        typeCombo.setMaxWidth(Double.MAX_VALUE);

        List<Utilisateur> users = new ArrayList<>(usersById.values());
        users.sort(Comparator.comparing(Utilisateur::getFullName, String.CASE_INSENSITIVE_ORDER));
        ComboBox<Utilisateur> authorCombo = new ComboBox<>(FXCollections.observableArrayList(users));
        authorCombo.setPromptText("Auteur");
        authorCombo.setMaxWidth(Double.MAX_VALUE);

        if (existing != null && existing.getAuthorId() != null) {
            authorCombo.getItems().stream()
                    .filter(user -> user.getId() == existing.getAuthorId())
                    .findFirst()
                    .ifPresent(authorCombo::setValue);
        }

        TextField titleField = new TextField(existing == null ? "" : nullSafe(existing.getTitle()));
        TextArea bodyArea = new TextArea(existing == null ? "" : nullSafe(existing.getBody()));
        bodyArea.setPrefRowCount(4);
        TextField attachmentField = new TextField(existing == null ? "" : nullSafe(existing.getAttachmentUrl()));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Type"), 0, 0);
        grid.add(typeCombo, 1, 0);
        grid.add(new Label("Auteur"), 0, 1);
        grid.add(authorCombo, 1, 1);
        grid.add(new Label("Titre"), 0, 2);
        grid.add(titleField, 1, 2);
        grid.add(new Label("Contenu"), 0, 3);
        grid.add(bodyArea, 1, 3);
        grid.add(new Label("Pièce jointe (URL/chemin)"), 0, 4);
        grid.add(attachmentField, 1, 4);

        GridPane.setHgrow(typeCombo, Priority.ALWAYS);
        GridPane.setHgrow(authorCombo, Priority.ALWAYS);
        GridPane.setHgrow(titleField, Priority.ALWAYS);
        GridPane.setHgrow(bodyArea, Priority.ALWAYS);
        GridPane.setHgrow(attachmentField, Priority.ALWAYS);

        DialogPane pane = dialog.getDialogPane();
        pane.setContent(grid);
        styleDialog(pane);

        ButtonType saveType = new ButtonType(existing == null ? "Ajouter" : "Enregistrer",
                ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != saveType) {
            return null;
        }

        if (authorCombo.getValue() == null) {
            showWarning("Auteur requis", "Veuillez choisir un auteur pour ce post.");
            return null;
        }

        String body = nullSafe(bodyArea.getText()).trim();
        String title = nullSafe(titleField.getText()).trim();
        if (title.isBlank() && body.isBlank()) {
            showWarning("Contenu requis", "Veuillez renseigner un titre ou un contenu pour le post.");
            return null;
        }

        GroupPost post = existing == null ? new GroupPost() : existing;
        post.setPostType(typeCombo.getValue());
        post.setAuthorId(authorCombo.getValue().getId());
        post.setTitle(title);
        post.setBody(body);
        post.setAttachmentUrl(nullSafe(attachmentField.getText()).trim());

        if (post.getCreatedAt() == null) {
            post.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        }

        return post;
    }

    private Comparator<StudyGroup> comparatorFor(String sortBy) {
        if ("Nom".equalsIgnoreCase(sortBy)) {
            return Comparator.comparing(group -> lower(group.getName()));
        }
        if ("Date création".equalsIgnoreCase(sortBy)) {
            return Comparator.comparing(StudyGroup::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        }
        if ("Membres".equalsIgnoreCase(sortBy)) {
            return Comparator.comparingInt(group -> countMembers(group.getId()));
        }
        if ("Posts".equalsIgnoreCase(sortBy)) {
            return Comparator.comparingInt(group -> countPosts(group.getId()));
        }

        return Comparator.comparing(StudyGroup::getLastActivity, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private Comparator<GroupMember> comparatorForMembers(String sortBy) {
        if ("Nom".equalsIgnoreCase(sortBy)) {
            return Comparator.comparing(member -> lower(displayUserName(member.getUserId())));
        }
        if ("Rôle".equalsIgnoreCase(sortBy)) {
            return Comparator.comparing(member -> lower(formatMemberRole(member.getMemberRole())));
        }
        return Comparator.comparing(GroupMember::getJoinedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private Comparator<GroupPost> comparatorForPosts(String sortBy) {
        if ("Titre".equalsIgnoreCase(sortBy)) {
            return Comparator.comparing(post -> lower(displayPostTitle(post)));
        }
        if ("Auteur".equalsIgnoreCase(sortBy)) {
            return Comparator.comparing(post -> lower(displayUserName(post.getAuthorId())));
        }
        return Comparator.comparing(GroupPost::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private boolean filterByQuery(StudyGroup group, String query) {
        if (query.isBlank()) {
            return true;
        }

        return lower(group.getName()).contains(query)
                || lower(group.getSubject()).contains(query)
                || lower(group.getDescription()).contains(query);
    }

    private boolean filterByPrivacy(StudyGroup group, String privacyValue) {
        if (privacyValue == null || "Tous".equalsIgnoreCase(privacyValue)) {
            return true;
        }

        String value = lower(group.getPrivacy());
        if ("Public".equalsIgnoreCase(privacyValue)) {
            return "public".equals(value);
        }
        if ("Privé".equalsIgnoreCase(privacyValue)) {
            return "private".equals(value);
        }
        return true;
    }

    private boolean filterMember(GroupMember member, String query) {
        if (query.isBlank()) {
            return true;
        }

        return lower(displayUserName(member.getUserId())).contains(query)
                || lower(formatMemberRole(member.getMemberRole())).contains(query);
    }

    private boolean filterPost(GroupPost post, String query) {
        if (query.isBlank()) {
            return true;
        }

        return lower(displayPostTitle(post)).contains(query)
                || lower(nullSafe(post.getBody())).contains(query)
                || lower(displayUserName(post.getAuthorId())).contains(query);
    }

    private int countMembers(Integer groupId) {
        if (groupId == null) {
            return 0;
        }
        return memberCountByGroup.getOrDefault(groupId, 0L).intValue();
    }

    private int countPosts(Integer groupId) {
        if (groupId == null) {
            return 0;
        }
        return postCountByGroup.getOrDefault(groupId, 0L).intValue();
    }

    private boolean ensureGroupSelected() {
        if (selectedGroup == null || selectedGroup.getId() == null) {
            showWarning("Groupe requis", "Sélectionnez un groupe dans la table.");
            return false;
        }
        return true;
    }

    private String displayUserName(Integer userId) {
        if (userId == null) {
            return "-";
        }

        Utilisateur user = usersById.get(userId);
        if (user == null) {
            return "Utilisateur #" + userId;
        }
        return user.getFullName();
    }

    private String displayPostTitle(GroupPost post) {
        String title = nullSafe(post.getTitle()).trim();
        if (!title.isBlank()) {
            return title;
        }
        String body = nullSafe(post.getBody()).trim();
        if (body.isBlank()) {
            return "(Sans titre)";
        }
        return body.length() > 48 ? body.substring(0, 48) + "..." : body;
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return "-";
        }
        return dateFormatter.format(timestamp.toLocalDateTime());
    }

    private String formatPrivacy(String privacy) {
        if (privacy == null) {
            return "-";
        }
        return "private".equalsIgnoreCase(privacy) ? "Privé" : "Public";
    }

    private String formatMemberRole(String role) {
        if (role == null) {
            return "Membre";
        }
        return switch (role.toLowerCase()) {
            case "admin" -> "Admin";
            case "moderator", "moderateur" -> "Modérateur";
            default -> "Membre";
        };
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "member";
        }
        String lower = role.trim().toLowerCase();
        if ("moderateur".equals(lower)) {
            return "moderator";
        }
        return lower;
    }

    private String formatPostType(String type) {
        if (type == null || type.isBlank()) {
            return "Texte";
        }
        return switch (type.toLowerCase()) {
            case "file" -> "Fichier";
            case "link" -> "Lien";
            default -> "Texte";
        };
    }

    private String normalizePostType(String type) {
        if (type == null || type.isBlank()) {
            return "text";
        }
        return switch (type.toLowerCase()) {
            case "file" -> "file";
            case "link" -> "link";
            default -> "text";
        };
    }

    private Window ownerWindow() {
        return groupsTable != null && groupsTable.getScene() != null ? groupsTable.getScene().getWindow() : null;
    }

    private void styleDialog(DialogPane pane) {
        GroupUiUtils.applyDialogStyle(pane, GroupManagementController.class);
    }

    private boolean confirmAction(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(header);
        alert.setContentText(content);
        if (ownerWindow() != null) {
            alert.initOwner(ownerWindow());
        }
        styleDialog(alert.getDialogPane());

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void showInfo(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(header);
        alert.setContentText(content);
        if (ownerWindow() != null) {
            alert.initOwner(ownerWindow());
        }
        styleDialog(alert.getDialogPane());
        alert.showAndWait();
    }

    private void showWarning(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Attention");
        alert.setHeaderText(header);
        alert.setContentText(content);
        if (ownerWindow() != null) {
            alert.initOwner(ownerWindow());
        }
        styleDialog(alert.getDialogPane());
        alert.showAndWait();
    }

    private void showError(String header, String content, String details) {
        String message = content;
        if (details != null && !details.isBlank()) {
            message += "\n\nDétails: " + details;
        }

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(header);
        alert.setContentText(message);
        if (ownerWindow() != null) {
            alert.initOwner(ownerWindow());
        }
        styleDialog(alert.getDialogPane());
        alert.showAndWait();
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase().trim();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
