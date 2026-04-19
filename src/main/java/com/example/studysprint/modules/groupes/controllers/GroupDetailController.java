package com.example.studysprint.modules.groupes.controllers;

import com.example.studysprint.modules.groupes.models.GroupMember;
import com.example.studysprint.modules.groupes.models.GroupPost;
import com.example.studysprint.modules.groupes.models.PostComment;
import com.example.studysprint.modules.groupes.models.PostLike;
import com.example.studysprint.modules.groupes.models.PostRating;
import com.example.studysprint.modules.groupes.models.StudyGroup;
import com.example.studysprint.modules.groupes.services.GroupMemberService;
import com.example.studysprint.modules.groupes.services.GroupPostService;
import com.example.studysprint.modules.groupes.services.GroupService;
import com.example.studysprint.modules.groupes.services.PostCommentService;
import com.example.studysprint.modules.groupes.services.PostLikeService;
import com.example.studysprint.modules.groupes.services.PostRatingService;
import com.example.studysprint.modules.groupes.utils.GroupUiUtils;
import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.modules.utilisateurs.services.UtilisateurService;
import com.example.studysprint.utils.AppNavigator;
import com.example.studysprint.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class GroupDetailController {
    private static final UtilisateurService.UserDisplay UNKNOWN_USER = new UtilisateurService.UserDisplay("Utilisateur", "", "U");

    @FXML
    private HBox rootPane;
    @FXML
    private Label sidebarFullNameLabel;
    @FXML
    private Label sidebarRoleLabel;
    @FXML
    private Label groupAvatarLabel;
    @FXML
    private Label groupNameLabel;
    @FXML
    private Label groupSubjectLabel;
    @FXML
    private Label groupSubjectMetaLabel;
    @FXML
    private Label groupCreatedLabel;
    @FXML
    private Label memberCountLabel;
    @FXML
    private Label infoSubjectValueLabel;
    @FXML
    private Label infoTypeValueLabel;
    @FXML
    private Label infoCreatedValueLabel;
    @FXML
    private Label infoPostsValueLabel;
    @FXML
    private Button backToGroupsButton;
    @FXML
    private Button groupSettingsButton;
    @FXML
    private Button groupDeleteButton;
    @FXML
    private VBox membersListBox;
    @FXML
    private VBox postsListBox;
    @FXML
    private Label composeAvatarLabel;
    @FXML
    private Label composeEditorAvatarLabel;
    @FXML
    private HBox composeCompactBox;
    @FXML
    private VBox composeEditorBox;
    @FXML
    private Button tabTextButton;
    @FXML
    private Button tabFileButton;
    @FXML
    private Button tabLinkButton;
    @FXML
    private VBox composeTextPane;
    @FXML
    private VBox composeFilePane;
    @FXML
    private VBox composeLinkPane;
    @FXML
    private TextField composeTextTitleField;
    @FXML
    private TextArea composeTextBodyArea;
    @FXML
    private TextField composeFileTitleField;
    @FXML
    private Label composeFilePathLabel;
    @FXML
    private TextArea composeFileDescArea;
    @FXML
    private TextField composeLinkTitleField;
    @FXML
    private TextField composeLinkUrlField;
    @FXML
    private TextArea composeLinkCommentArea;

    private final GroupMemberService memberService = new GroupMemberService();
    private final GroupPostService postService = new GroupPostService();
    private final GroupService groupService = new GroupService();
    private final PostCommentService commentService = new PostCommentService();
    private final PostLikeService likeService = new PostLikeService();
    private final PostRatingService ratingService = new PostRatingService();
    private final UtilisateurService userService = new UtilisateurService();

    private StudyGroup currentGroup;
    private String composeMode = "text";
    private String selectedComposeFilePath;
    private final Set<Integer> expandedCommentPostIds = new HashSet<>();

    // Initialize static UI state.
    @FXML
    public void initialize() {
        populateSidebarUser();
        String initials = resolveUserDisplay(currentUserId()).initials();
        composeAvatarLabel.setText(initials);
        composeEditorAvatarLabel.setText(initials);
        if (backToGroupsButton != null) {
            backToGroupsButton.setGraphic(GroupUiUtils.icon("fas-arrow-left", "detail-dialog-icon"));
        }
        if (groupSettingsButton != null) {
            groupSettingsButton.setGraphic(GroupUiUtils.icon("fas-cog", "detail-dialog-icon"));
        }
        if (groupDeleteButton != null) {
            groupDeleteButton.setGraphic(GroupUiUtils.icon("fas-trash-alt", "detail-dialog-danger-icon"));
        }
        setComposeMode("text");
        setComposeEditorVisible(false);
    }

    private void populateSidebarUser() {
        Utilisateur currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }
        sidebarFullNameLabel.setText(currentUser.getFullName());
        sidebarRoleLabel.setText(formatRole(currentUser));
    }

    // Set selected group and refresh UI.
    public void setGroup(StudyGroup group) {
        this.currentGroup = group;
        renderGroupDetails();
    }

    // Return to groups list.
    @FXML
    private void onBackToGroups() {
        navigateToList();
    }

    // Leave current group and return to list.
    @FXML
    private void onLeaveGroup() {
        navigateToList();
    }

    @FXML
    private void onGoHome() {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        AppNavigator.switchTo(stage, AppNavigator.HOME_FXML, AppNavigator.HOME_TITLE, getClass());
    }

    @FXML
    private void onGoGroups() {
        navigateToList();
    }

    @FXML
    private void onOpenProfile() {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        AppNavigator.switchTo(stage, "/fxml/auth/profile.fxml", "Mon Profil - StudySprint", getClass());
    }

    @FXML
    private void onOpenChangePassword() {
        onOpenProfile();
    }

    @FXML
    private void onLogout() {
        SessionManager.getInstance().logout();
        Stage stage = (Stage) rootPane.getScene().getWindow();
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

    // Expand composer panel.
    @FXML
    private void onOpenComposer() {
        setComposeEditorVisible(true);
        setComposeMode(composeMode);
    }

    // Switch composer to text mode.
    @FXML
    private void onComposeTabText() {
        setComposeMode("text");
    }

    // Switch composer to file mode.
    @FXML
    private void onComposeTabFile() {
        setComposeMode("file");
    }

    // Switch composer to link mode.
    @FXML
    private void onComposeTabLink() {
        setComposeMode("link");
    }

    // Pick attachment file for file mode.
    @FXML
    private void onChooseComposeFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir un fichier");
        Stage stage = (Stage) rootPane.getScene().getWindow();
        File selected = chooser.showOpenDialog(stage);
        if (selected == null) {
            return;
        }

        selectedComposeFilePath = selected.getAbsolutePath();
        composeFilePathLabel.setText(selected.getName());
    }

    // Cancel compose and collapse editor.
    @FXML
    private void onCancelCompose() {
        resetComposeForm();
        setComposeEditorVisible(false);
    }

    // Validate input then publish a new post.
    @FXML
    private void onPublishCompose() {
        if (currentGroup == null || currentGroup.getId() == null) {
            return;
        }

        GroupPost post = new GroupPost();
        post.setGroupId(currentGroup.getId());
        post.setAuthorId(currentUserId());
        post.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        post.setParentPostId(null);

        try {
            if ("file".equals(composeMode)) {
                publishFilePost(post);
            } else if ("link".equals(composeMode)) {
                publishLinkPost(post);
            } else {
                publishTextPost(post);
            }

            postService.add(post);
            resetComposeForm();
            setComposeEditorVisible(false);
            renderGroupDetails();
            GroupUiUtils.showSuccess(rootPane.getScene().getWindow(), GroupDetailController.class,
                    "Publication creee",
                    "Votre publication a bien ete publiee.");
        } catch (IllegalArgumentException ex) {
            GroupUiUtils.showError(rootPane.getScene().getWindow(), GroupDetailController.class,
                    "Validation",
                    ex.getMessage(),
                    null);
        } catch (Exception ex) {
            GroupUiUtils.showError(rootPane.getScene().getWindow(), GroupDetailController.class,
                    "Erreur",
                    "Impossible de publier pour le moment.",
                    ex.getMessage());
        }
    }

    // Populate text-post payload.
    private void publishTextPost(GroupPost post) {
        String title = GroupUiUtils.nullSafe(composeTextTitleField.getText()).trim();
        String body = GroupUiUtils.nullSafe(composeTextBodyArea.getText()).trim();

        if (body.isBlank()) {
            throw new IllegalArgumentException("Le contenu du post est obligatoire.");
        }

        post.setPostType("text");
        post.setTitle(title.isBlank() ? null : title);
        post.setBody(body);
        post.setAttachmentUrl(null);
    }

    // Populate file-post payload.
    private void publishFilePost(GroupPost post) {
        String title = GroupUiUtils.nullSafe(composeFileTitleField.getText()).trim();
        String desc = GroupUiUtils.nullSafe(composeFileDescArea.getText()).trim();

        if (selectedComposeFilePath == null || selectedComposeFilePath.isBlank()) {
            throw new IllegalArgumentException("Veuillez choisir un fichier avant de publier.");
        }

        post.setPostType("file");
        post.setTitle(title.isBlank() ? null : title);
        post.setBody(desc.isBlank() ? "Fichier partage" : desc);
        post.setAttachmentUrl(selectedComposeFilePath);
    }

    // Populate link-post payload.
    private void publishLinkPost(GroupPost post) {
        String title = GroupUiUtils.nullSafe(composeLinkTitleField.getText()).trim();
        String url = GroupUiUtils.nullSafe(composeLinkUrlField.getText()).trim();
        String comment = GroupUiUtils.nullSafe(composeLinkCommentArea.getText()).trim();

        if (url.isBlank()) {
            throw new IllegalArgumentException("L'URL du lien est obligatoire.");
        }

        post.setPostType("link");
        post.setTitle(title.isBlank() ? null : title);
        post.setBody(comment.isBlank() ? "Lien partage" : comment);
        post.setAttachmentUrl(url);
    }

    // Show selected compose panel and active tab style.
    private void setComposeMode(String mode) {
        this.composeMode = mode;

        boolean textMode = "text".equals(mode);
        boolean fileMode = "file".equals(mode);
        boolean linkMode = "link".equals(mode);

        composeTextPane.setVisible(textMode);
        composeTextPane.setManaged(textMode);
        composeFilePane.setVisible(fileMode);
        composeFilePane.setManaged(fileMode);
        composeLinkPane.setVisible(linkMode);
        composeLinkPane.setManaged(linkMode);

        setTabActive(tabTextButton, textMode);
        setTabActive(tabFileButton, fileMode);
        setTabActive(tabLinkButton, linkMode);
    }

    // Toggle active class on compose tabs.
    private void setTabActive(Button tab, boolean active) {
        tab.getStyleClass().remove("compose-tab-active");
        if (active) {
            tab.getStyleClass().add("compose-tab-active");
        }
    }

    // Expand or collapse extended composer.
    private void setComposeEditorVisible(boolean visible) {
        composeCompactBox.setVisible(!visible);
        composeCompactBox.setManaged(!visible);
        composeEditorBox.setVisible(visible);
        composeEditorBox.setManaged(visible);
    }

    // Reset compose form fields.
    private void resetComposeForm() {
        composeTextTitleField.clear();
        composeTextBodyArea.clear();
        composeFileTitleField.clear();
        composeFileDescArea.clear();
        composeLinkTitleField.clear();
        composeLinkUrlField.clear();
        composeLinkCommentArea.clear();
        selectedComposeFilePath = null;
        composeFilePathLabel.setText("Aucun fichier selectionne");
        setComposeMode("text");
    }

    // Placeholder action for group settings.
    @FXML
    private void onGroupSettings() {
        if (currentGroup == null) {
            return;
        }

        try {
            StudyGroup updated = GroupFormController.showDialog(currentGroup, rootPane.getScene().getWindow());
            if (updated == null) {
                return;
            }

            groupService.update(updated);
            currentGroup = updated;
            renderGroupDetails();
            GroupUiUtils.showSuccess(rootPane.getScene().getWindow(), GroupDetailController.class,
                    "Groupe modifie",
                    "Parfait, vos modifications ont bien ete enregistrees.");
        } catch (Exception e) {
            GroupUiUtils.showError(rootPane.getScene().getWindow(), GroupDetailController.class,
                    "Erreur",
                    "Impossible de modifier le groupe.",
                    e.getMessage());
        }
    }

    // Delete current group and return to list.
    @FXML
    private void onDeleteGroup() {
        if (currentGroup == null || currentGroup.getId() == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer le groupe");
        confirm.setHeaderText("Confirmer la suppression");
        confirm.setContentText("Voulez-vous supprimer le groupe \"" + GroupUiUtils.nullSafe(currentGroup.getName()) + "\" ?\nCette action est irreversible.");
        ButtonType deleteType = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(deleteType, cancelType);
        GroupUiUtils.applyDialogStyle(confirm.getDialogPane(), GroupDetailController.class);

        Button deleteButton = (Button) confirm.getDialogPane().lookupButton(deleteType);
        if (deleteButton != null) {
            deleteButton.getStyleClass().add("danger-btn");
            deleteButton.setGraphic(GroupUiUtils.icon("fas-trash-alt", "detail-dialog-danger-icon"));
        }

        Button cancelButton = (Button) confirm.getDialogPane().lookupButton(cancelType);
        if (cancelButton != null) {
            cancelButton.setGraphic(GroupUiUtils.icon("fas-times", "detail-dialog-icon"));
        }

        confirm.showAndWait().ifPresent(result -> {
            if (result == deleteType) {
                groupService.delete(currentGroup.getId());
                GroupUiUtils.showSuccess(rootPane.getScene().getWindow(), GroupDetailController.class,
                        "Groupe supprime",
                        "C'est fait. Le groupe a bien ete supprime.");
                navigateToList();
            }
        });
    }

    // Reload and render group details.
    private void renderGroupDetails() {
        if (currentGroup == null || currentGroup.getId() == null) {
            return;
        }

        List<GroupMember> members = memberService.getByGroup(currentGroup.getId());
        List<GroupPost> posts = postService.getByGroup(currentGroup.getId());

        groupAvatarLabel.setText(GroupUiUtils.initial(currentGroup.getName()));
        groupNameLabel.setText(GroupUiUtils.nullSafe(currentGroup.getName()));
        groupNameLabel.setGraphic(GroupUiUtils.icon(GroupUiUtils.privacyIconLiteral(currentGroup.getPrivacy()), "group-privacy-icon"));
        groupSubjectLabel.setText(GroupUiUtils.nullSafe(currentGroup.getDescription()).isBlank()
            ? GroupUiUtils.nullSafe(currentGroup.getSubject())
            : GroupUiUtils.nullSafe(currentGroup.getDescription()));
        groupSubjectMetaLabel.setText("Matiere: " + GroupUiUtils.nullSafe(currentGroup.getSubject()));
        groupCreatedLabel.setText("Cree " + GroupUiUtils.formatRelativeTime(currentGroup.getCreatedAt()));

        memberCountLabel.setText("Membres (" + members.size() + ")");
        infoSubjectValueLabel.setText(GroupUiUtils.nullSafe(currentGroup.getSubject()));
        infoTypeValueLabel.setText("private".equalsIgnoreCase(currentGroup.getPrivacy()) ? "Prive" : "Public");
        infoCreatedValueLabel.setText(GroupUiUtils.formatRelativeTime(currentGroup.getCreatedAt()));
        infoPostsValueLabel.setText(String.valueOf(posts.size()));
        applyComposeAvatarRoleTheme();

        renderMembers(members);
        renderPosts(posts);
    }

    // Apply role-based theme to composer avatars.
    private void applyComposeAvatarRoleTheme() {
        String role = "member";
        int userId = currentUserId();
        if (currentGroup != null) {
            if (currentGroup.getCreatedById() != null && currentGroup.getCreatedById() == userId) {
                role = "admin";
            } else if (currentGroup.getId() != null) {
                role = memberService.getMemberRoleForUser(currentGroup.getId(), userId).orElse("member");
            }
        }

        String roleClass = detailAiAvatarClass(role);
        composeAvatarLabel.getStyleClass().removeAll(
                "detail-ai-avatar-admin",
                "detail-ai-avatar-moderator",
                "detail-ai-avatar-member"
        );
        composeEditorAvatarLabel.getStyleClass().removeAll(
                "detail-ai-avatar-admin",
                "detail-ai-avatar-moderator",
                "detail-ai-avatar-member"
        );
        composeAvatarLabel.getStyleClass().add(roleClass);
        composeEditorAvatarLabel.getStyleClass().add(roleClass);
    }

    // Render members list in sidebar.
    private void renderMembers(List<GroupMember> members) {
        membersListBox.getChildren().clear();

        if (members.isEmpty()) {
            Label empty = new Label("Aucun membre pour le moment");
            empty.getStyleClass().add("detail-muted-text");
            membersListBox.getChildren().add(empty);
            return;
        }

        List<GroupMember> admins = members.stream()
                .filter(m -> "admin".equalsIgnoreCase(m.getMemberRole()))
                .toList();
        List<GroupMember> moderators = members.stream()
                .filter(m -> "moderator".equalsIgnoreCase(m.getMemberRole()))
                .toList();
        List<GroupMember> basicMembers = members.stream()
                .filter(m -> !"admin".equalsIgnoreCase(m.getMemberRole()) && !"moderator".equalsIgnoreCase(m.getMemberRole()))
                .toList();

        boolean hasPreviousSection = false;

        if (!admins.isEmpty()) {
            addMemberSection("ADMINISTRATEURS", admins, false, hasPreviousSection);
            hasPreviousSection = true;
        }

        if (!moderators.isEmpty()) {
            addMemberSection("MODERATEURS", moderators, true, hasPreviousSection);
            hasPreviousSection = true;
        }

        if (!basicMembers.isEmpty()) {
            addMemberSection("MEMBRES", basicMembers, true, hasPreviousSection);
        }
    }

    // Render one role section.
    private void addMemberSection(String title, List<GroupMember> sectionMembers, boolean showMenu, boolean withDivider) {
        if (withDivider) {
            Separator divider = new Separator();
            divider.getStyleClass().add("detail-member-divider");
            membersListBox.getChildren().add(divider);
        }

        Label sectionTitle = new Label(title + " (" + sectionMembers.size() + ")");
        sectionTitle.getStyleClass().add("detail-member-section-title");
        membersListBox.getChildren().add(sectionTitle);

        for (GroupMember member : sectionMembers) {
            HBox row = new HBox(10);
            row.getStyleClass().add("detail-member-row");

            UtilisateurService.UserDisplay user = resolveUserDisplay(member.getUserId());

            Label avatar = new Label(user.initials());
            avatar.getStyleClass().add("detail-small-avatar");
            avatar.getStyleClass().add(detailAvatarClass(member.getMemberRole()));

            VBox identity = new VBox(2);
            Label name = new Label(user.fullName());
            name.getStyleClass().add("detail-member-name");
            name.setMaxWidth(145);
            name.setTextOverrun(OverrunStyle.ELLIPSIS);
            Label email = new Label(user.email());
            email.getStyleClass().add("detail-member-email");
            email.setMaxWidth(145);
            email.setTextOverrun(OverrunStyle.ELLIPSIS);
            identity.getChildren().addAll(name, email);
            identity.setMinWidth(0);

            Label role = new Label(roleLabel(member.getMemberRole()));
            role.getStyleClass().add("detail-role-pill");
            role.getStyleClass().add(detailRoleBadgeClass(member.getMemberRole()));
            role.setAlignment(Pos.CENTER);
            role.setMinWidth(Region.USE_PREF_SIZE);
            role.setMaxWidth(Region.USE_PREF_SIZE);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            if (showMenu) {
                MenuButton menuButton = buildMemberActionsMenu(member);
                row.getChildren().addAll(avatar, identity, spacer, role, menuButton);
            } else {
                row.getChildren().addAll(avatar, identity, spacer, role);
            }
            membersListBox.getChildren().add(row);
        }
    }

    // Build member actions menu.
    private MenuButton buildMemberActionsMenu(GroupMember member) {
        String currentRole = member.getMemberRole() == null ? "member" : member.getMemberRole().trim().toLowerCase();

        MenuItem promoteAdmin = new MenuItem("Promouvoir Admin");
        promoteAdmin.setGraphic(GroupUiUtils.icon("fas-arrow-up", "detail-menu-icon"));
        promoteAdmin.setOnAction(e -> updateMemberRole(member, "admin", "Role mis a jour", "Le membre a ete promu Admin."));

        MenuItem promoteModerator = new MenuItem("Promouvoir Moderateur");
        promoteModerator.setGraphic(GroupUiUtils.icon("fas-arrow-up", "detail-menu-icon"));
        promoteModerator.setOnAction(e -> updateMemberRole(member, "moderator", "Role mis a jour", "Le membre a ete promu Moderateur."));

        MenuItem demoteMember = new MenuItem("Retrograder Membre");
        demoteMember.setGraphic(GroupUiUtils.icon("fas-arrow-down", "detail-menu-icon"));
        demoteMember.setOnAction(e -> updateMemberRole(member, "member", "Role mis a jour", "Le membre est maintenant Membre."));

        MenuItem removeMember = new MenuItem("Retirer du groupe");
        removeMember.setGraphic(GroupUiUtils.icon("fas-trash-alt", "detail-menu-danger-icon"));
        removeMember.getStyleClass().add("danger-item");
        removeMember.setOnAction(e -> removeMember(member));

        SeparatorMenuItem divider = new SeparatorMenuItem();

        Circle dotTop = new Circle(1.3);
        Circle dotMiddle = new Circle(1.3);
        Circle dotBottom = new Circle(1.3);
        VBox dotsIcon = new VBox(1.8, dotTop, dotMiddle, dotBottom);
        dotsIcon.setAlignment(Pos.CENTER);
        dotsIcon.getStyleClass().add("detail-member-menu-icon");

        MenuButton menu = new MenuButton();
        menu.setGraphic(dotsIcon);
        menu.getStyleClass().add("detail-member-menu-btn");

        if ("moderator".equals(currentRole) || "moderateur".equals(currentRole)) {
            menu.getItems().addAll(promoteAdmin, demoteMember, divider, removeMember);
        } else {
            menu.getItems().addAll(promoteAdmin, promoteModerator, divider, removeMember);
        }

        return menu;
    }

    // Update member role and refresh UI.
    private void updateMemberRole(GroupMember member, String role, String header, String content) {
        try {
            memberService.updateRole(member.getId(), role);
            renderGroupDetails();
            GroupUiUtils.showSuccess(rootPane.getScene().getWindow(), GroupDetailController.class, header, content);
        } catch (Exception ex) {
            GroupUiUtils.showError(rootPane.getScene().getWindow(), GroupDetailController.class,
                    "Erreur",
                    "Impossible de modifier le role du membre.",
                    ex.getMessage());
        }
    }

    // Remove member after confirmation.
    private void removeMember(GroupMember member) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Retirer le membre");
        confirm.setHeaderText("Confirmer le retrait");
        confirm.setContentText("Voulez-vous retirer ce membre du groupe ?");

        ButtonType removeType = new ButtonType("Retirer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(removeType, cancelType);
        GroupUiUtils.applyDialogStyle(confirm.getDialogPane(), GroupDetailController.class);

        Button removeButton = (Button) confirm.getDialogPane().lookupButton(removeType);
        if (removeButton != null) {
            removeButton.getStyleClass().add("danger-btn");
        }

        confirm.showAndWait().ifPresent(result -> {
            if (result == removeType) {
                try {
                    memberService.delete(member.getId());
                    renderGroupDetails();
                    GroupUiUtils.showSuccess(rootPane.getScene().getWindow(), GroupDetailController.class,
                            "Membre retire",
                            "Le membre a ete retire du groupe.");
                } catch (Exception ex) {
                    GroupUiUtils.showError(rootPane.getScene().getWindow(), GroupDetailController.class,
                            "Erreur",
                            "Impossible de retirer ce membre.",
                            ex.getMessage());
                }
            }
        });
    }

    // Resolve user display identity from database.
    private UtilisateurService.UserDisplay resolveUserDisplay(Integer userId) {
        try {
            return userService.getDisplay(userId);
        } catch (Exception ignored) {
            return UNKNOWN_USER;
        }
    }

    // Render posts feed.
    private void renderPosts(List<GroupPost> posts) {
        postsListBox.getChildren().clear();
        int currentUserId = currentUserId();

        if (posts.isEmpty()) {
            Label empty = new Label("Aucun post dans ce groupe pour l'instant.");
            empty.getStyleClass().add("detail-empty-post");
            postsListBox.getChildren().add(empty);
            return;
        }

            List<GroupPost> visiblePosts = posts.stream().limit(6).toList();
            Set<Integer> visiblePostIds = visiblePosts.stream()
                .map(GroupPost::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            List<PostLike> visibleLikes = likeService.getAll().stream()
                .filter(like -> visiblePostIds.contains(like.getPostId()))
                .toList();
            Map<Integer, Long> likesCountByPostId = visibleLikes.stream()
                .collect(Collectors.groupingBy(PostLike::getPostId, Collectors.counting()));
            Set<Integer> likedPostIdsByCurrentUser = visibleLikes.stream()
                .filter(like -> like.getUserId() == currentUserId)
                .map(PostLike::getPostId)
                .collect(Collectors.toSet());

            List<PostComment> visibleComments = commentService.getAll().stream()
                .filter(comment -> visiblePostIds.contains(comment.getPostId()))
                .toList();
            Map<Integer, List<PostComment>> commentsByPostId = visibleComments.stream()
                .collect(Collectors.groupingBy(PostComment::getPostId));

            List<PostRating> visibleRatings = ratingService.getAll().stream()
                .filter(rating -> visiblePostIds.contains(rating.getPostId()))
                .toList();
            Map<Integer, Double> averageRatingByPostId = visibleRatings.stream()
                .collect(Collectors.groupingBy(PostRating::getPostId,
                    Collectors.averagingInt(r -> r.getRating() == null ? 0 : r.getRating())));
            Map<Integer, Integer> userRatingByPostId = visibleRatings.stream()
                .filter(r -> r.getUserId() == currentUserId)
                .collect(Collectors.toMap(
                    PostRating::getPostId,
                    r -> r.getRating() == null ? 0 : (int) r.getRating(),
                    (a, b) -> b
                ));

            for (GroupPost post : visiblePosts) {
            VBox card = new VBox(10);
            card.getStyleClass().add("detail-post-card");

            UtilisateurService.UserDisplay authorData = resolveUserDisplay(post.getAuthorId());
            String role = currentGroup == null || currentGroup.getId() == null
                    ? "member"
                    : memberService.getMemberRoleForUser(currentGroup.getId(), post.getAuthorId()).orElse("member");

            HBox head = new HBox(10);
            head.getStyleClass().add("detail-post-header-row");

            Label avatar = new Label(authorData.initials());
            avatar.getStyleClass().add("detail-small-avatar");
            avatar.getStyleClass().add(detailAvatarClass(role));

            VBox authorBox = new VBox(2);
            HBox authorRow = new HBox(8);
            Label author = new Label(authorData.fullName());
            author.getStyleClass().add("detail-post-author");
            Label rolePill = new Label(roleLabel(role));
            rolePill.getStyleClass().add("detail-post-role-pill");
            rolePill.getStyleClass().add(detailRoleBadgeClass(role));
            authorRow.getChildren().addAll(author, rolePill);

            Label when = new Label(GroupUiUtils.formatRelativeTime(post.getCreatedAt()));
            when.getStyleClass().add("detail-post-time");
            authorBox.getChildren().addAll(authorRow, when);

            Region headSpacer = new Region();
            HBox.setHgrow(headSpacer, Priority.ALWAYS);
            Button deleteButton = new Button();
            deleteButton.setGraphic(GroupUiUtils.icon("fas-trash-alt", "detail-delete-icon"));
            deleteButton.getStyleClass().addAll("detail-post-icon-btn", "detail-post-delete-btn");
            deleteButton.setOnAction(event -> onDeletePost(post));

            head.getChildren().addAll(avatar, authorBox, headSpacer, deleteButton);

            String titleText = GroupUiUtils.nullSafe(post.getTitle());
            Label title = new Label(titleText.isBlank() ? "Publication" : titleText);
            title.getStyleClass().add("detail-post-title");

            Label body = new Label(GroupUiUtils.nullSafe(post.getBody()));
            body.getStyleClass().add("detail-post-body");
            body.setWrapText(true);

            HBox attachmentChip = null;
            if (post.getAttachmentUrl() != null && !post.getAttachmentUrl().isBlank()) {
                attachmentChip = new HBox(8);
                attachmentChip.getStyleClass().add("detail-post-attachment-chip");
                attachmentChip.getStyleClass().add("detail-post-attachment-clickable");

                String icon = "link".equalsIgnoreCase(post.getPostType()) || post.getAttachmentUrl().startsWith("http")
                    ? "fas-link"
                    : "fas-file-alt";
                FontIcon attachmentIcon = GroupUiUtils.icon(icon, "detail-attachment-icon");
                attachmentIcon.getStyleClass().add("detail-post-attachment-icon");

                Label attachmentText = new Label(attachmentLabel(post));
                attachmentText.getStyleClass().add("detail-post-attachment-text");
                attachmentText.setTextOverrun(OverrunStyle.ELLIPSIS);
                attachmentText.setMaxWidth(340);

                attachmentChip.getChildren().addAll(attachmentIcon, attachmentText);
                attachmentChip.setOnMouseClicked(event -> openAttachment(post.getAttachmentUrl()));
            }

            Separator divider = new Separator();
            divider.getStyleClass().add("detail-post-divider");

                int postId = post.getId() == null ? -1 : post.getId();
                int likes = likesCountByPostId.getOrDefault(postId, 0L).intValue();
                boolean likedByCurrentUser = likedPostIdsByCurrentUser.contains(postId);

                List<PostComment> allComments = commentsByPostId.getOrDefault(postId, List.of());
                int comments = allComments.size();
                List<PostComment> postComments = allComments.stream().limit(6).toList();

                double rating = averageRatingByPostId.getOrDefault(postId, 0.0);
                int userRating = userRatingByPostId.getOrDefault(postId, 0);
            boolean commentsExpanded = post.getId() != null && expandedCommentPostIds.contains(post.getId());

            VBox inlineCommentBox = new VBox(8);
            inlineCommentBox.getStyleClass().add("detail-inline-box");
            inlineCommentBox.setVisible(commentsExpanded);
            inlineCommentBox.setManaged(commentsExpanded);

            TextField inlineCommentField = new TextField();
            inlineCommentField.setPromptText("Ajouter un commentaire...");
            inlineCommentField.getStyleClass().add("compose-field");

            HBox inlineCommentActions = new HBox(8);
            inlineCommentActions.getStyleClass().add("detail-inline-actions");
            Button inlineCommentSend = new Button("Envoyer");
            inlineCommentSend.getStyleClass().add("compose-publish-btn");
            inlineCommentSend.setOnAction(event -> submitInlineComment(post, inlineCommentField));
            Button inlineCommentCancel = new Button("Annuler");
            inlineCommentCancel.getStyleClass().add("compose-cancel-btn");
            inlineCommentCancel.setOnAction(event -> {
                inlineCommentBox.setVisible(false);
                inlineCommentBox.setManaged(false);
                inlineCommentField.clear();
            });
            inlineCommentActions.getChildren().addAll(inlineCommentCancel, inlineCommentSend);
            inlineCommentBox.getChildren().addAll(inlineCommentField, inlineCommentActions);

            VBox commentsSection = new VBox(10);
            commentsSection.getStyleClass().add("detail-comments-section");
            commentsSection.setVisible(commentsExpanded);
            commentsSection.setManaged(commentsExpanded);
            commentsSection.getChildren().add(inlineCommentBox);

            HBox footer = new HBox(20);
            footer.getStyleClass().add("detail-post-footer-row");
            HBox likesAction = new HBox(6);
            likesAction.getStyleClass().add("detail-post-action-group");
            Button likesButton = new Button();
            likesButton.setGraphic(GroupUiUtils.icon("fas-heart", "detail-like-icon"));
            likesButton.getStyleClass().addAll("detail-post-icon-btn", "detail-post-like-btn");
            likesButton.getStyleClass().add(likedByCurrentUser ? "detail-post-like-btn-liked" : "detail-post-like-btn-unliked");
            likesButton.setOnAction(event -> onTogglePostLike(post));
            Label likesCount = new Label(String.valueOf(likes));
            likesCount.getStyleClass().add("detail-post-action-count");
            likesAction.getChildren().addAll(likesButton, likesCount);

            HBox commentsAction = new HBox(6);
            commentsAction.getStyleClass().add("detail-post-action-group");
            Button commentsButton = new Button();
            commentsButton.setGraphic(GroupUiUtils.icon("fas-comment", "detail-comment-icon"));
            commentsButton.getStyleClass().add("detail-post-icon-btn");
            commentsButton.setOnAction(event -> {
                boolean show = !commentsSection.isVisible();
                commentsSection.setVisible(show);
                commentsSection.setManaged(show);
                if (post.getId() != null) {
                    if (show) {
                        expandedCommentPostIds.add(post.getId());
                    } else {
                        expandedCommentPostIds.remove(post.getId());
                    }
                }
                if (!show) {
                    inlineCommentBox.setVisible(false);
                    inlineCommentBox.setManaged(false);
                } else {
                    inlineCommentBox.setVisible(true);
                    inlineCommentBox.setManaged(true);
                    inlineCommentField.requestFocus();
                }
            });
            Label commentsCount = new Label(String.valueOf(comments));
            commentsCount.getStyleClass().add("detail-post-action-count");
            commentsAction.getChildren().addAll(commentsButton, commentsCount);

            HBox ratingAction = new HBox(6);
            ratingAction.getStyleClass().add("detail-post-action-group");
            HBox ratingStarsRow = new HBox(3);
            ratingStarsRow.getStyleClass().add("detail-rating-stars-row");
            List<Button> ratingStarButtons = new ArrayList<>();
            for (int value = 1; value <= 5; value++) {
                Button starBtn = new Button();
                starBtn.setGraphic(GroupUiUtils.icon("fas-star", "detail-rating-icon"));
                starBtn.getStyleClass().add("detail-post-star-btn");
                if (value <= userRating) {
                    starBtn.getStyleClass().add("detail-post-star-btn-active");
                } else {
                    starBtn.getStyleClass().add("detail-post-star-btn-inactive");
                }
                final int selected = value;
                starBtn.setOnAction(event -> applyInlineRating(post, selected));
                starBtn.setOnMouseEntered(event -> applyStarVisualState(ratingStarButtons, selected));
                starBtn.setOnMouseExited(event -> applyStarVisualState(ratingStarButtons, userRating));
                ratingStarButtons.add(starBtn);
                ratingStarsRow.getChildren().add(starBtn);
            }
            Label ratingLabel = new Label(String.format("%.1f", rating));
            ratingLabel.getStyleClass().add("detail-post-footer-item");
            ratingAction.getChildren().addAll(ratingStarsRow, ratingLabel);

            Region footerSpacer = new Region();
            HBox.setHgrow(footerSpacer, Priority.ALWAYS);
            Button translate = new Button();
            translate.setGraphic(GroupUiUtils.icon("fas-language", "detail-translate-icon"));
            translate.getStyleClass().add("detail-post-icon-btn");
            translate.setOnAction(event -> {
                // Placeholder: translation is not implemented yet.
            });

            footer.getChildren().addAll(likesAction, commentsAction, ratingAction, footerSpacer, translate);

            card.getChildren().addAll(head, title, body);
            if (attachmentChip != null) {
                card.getChildren().add(attachmentChip);
            }
            card.getChildren().addAll(divider, footer);
            card.getChildren().add(commentsSection);

            if (!postComments.isEmpty()) {
                Map<Integer, PostComment> commentsById = postComments.stream()
                        .collect(Collectors.toMap(PostComment::getId, c -> c, (a, b) -> a));

                VBox commentsThread = new VBox(10);
                commentsThread.getStyleClass().add("detail-comments-thread");

                for (PostComment comment : postComments) {
                    HBox commentRow = new HBox(10);
                    commentRow.getStyleClass().add("detail-comment-row");
                    if (comment.getParentCommentId() != null) {
                        commentRow.getStyleClass().add("detail-comment-reply");
                    }

                    UtilisateurService.UserDisplay commentAuthorData = resolveUserDisplay(comment.getAuthorId());
                        String commentRole = currentGroup == null || currentGroup.getId() == null
                            ? "member"
                            : memberService.getMemberRoleForUser(currentGroup.getId(), comment.getAuthorId()).orElse("member");
                    Label commentAvatar = new Label(commentAuthorData.initials());
                    commentAvatar.getStyleClass().add("detail-comment-avatar");
                        commentAvatar.getStyleClass().add(detailAvatarClass(commentRole));

                    VBox commentContent = new VBox(4);
                    commentContent.getStyleClass().add("detail-comment-content");
                    HBox.setHgrow(commentContent, Priority.ALWAYS);

                    HBox commentHead = new HBox(8);
                    commentHead.setFillHeight(true);
                    Label commentAuthor = new Label(commentAuthorData.fullName());
                    commentAuthor.getStyleClass().add("detail-comment-author");
                    Region commentHeadSpacer = new Region();
                    HBox.setHgrow(commentHeadSpacer, Priority.ALWAYS);
                    Label commentWhen = new Label(GroupUiUtils.formatRelativeTime(comment.getCreatedAt()));
                    commentWhen.getStyleClass().add("detail-comment-time");
                    commentWhen.setMaxWidth(Double.MAX_VALUE);
                    commentHead.getChildren().addAll(commentAuthor, commentHeadSpacer, commentWhen);

                    Label commentBody = new Label(GroupUiUtils.nullSafe(comment.getBody()));
                    commentBody.setWrapText(true);
                    commentBody.getStyleClass().add("detail-comment-body");

                    if (comment.getParentCommentId() != null && commentsById.containsKey(comment.getParentCommentId())) {
                        PostComment parent = commentsById.get(comment.getParentCommentId());
                        UtilisateurService.UserDisplay parentAuthor = resolveUserDisplay(parent.getAuthorId());
                        Label replyTo = new Label("↩ En reponse a " + parentAuthor.fullName());
                        replyTo.getStyleClass().add("detail-comment-reply-to");
                        commentContent.getChildren().add(replyTo);
                    }

                    HBox commentActions = new HBox(8);
                    commentActions.getStyleClass().add("detail-comment-actions");
                    Button replyBtn = new Button("Repondre");
                    replyBtn.setGraphic(GroupUiUtils.icon("fas-reply", "detail-comment-action-icon"));
                    replyBtn.getStyleClass().add("detail-comment-action-btn");
                    Button deleteBtn = new Button("Supprimer");
                    deleteBtn.setGraphic(GroupUiUtils.icon("fas-trash-alt", "detail-comment-action-danger-icon"));
                    deleteBtn.getStyleClass().addAll("detail-comment-action-btn", "detail-comment-action-danger");
                    if (comment.getAuthorId() == currentUserId) {
                        deleteBtn.setOnAction(event -> {
                            try {
                                commentService.delete(comment.getId());
                                renderGroupDetails();
                            } catch (Exception ex) {
                                GroupUiUtils.showError(rootPane.getScene().getWindow(), GroupDetailController.class,
                                        "Erreur",
                                        "Impossible de supprimer le commentaire.",
                                        ex.getMessage());
                            }
                        });
                    } else {
                        deleteBtn.setDisable(true);
                    }
                    commentActions.getChildren().addAll(replyBtn, deleteBtn);

                    VBox inlineReplyBox = new VBox(8);
                    inlineReplyBox.getStyleClass().add("detail-inline-box");
                    inlineReplyBox.setVisible(false);
                    inlineReplyBox.setManaged(false);

                    TextField inlineReplyField = new TextField();
                    inlineReplyField.setPromptText("Ecrire une reponse...");
                    inlineReplyField.getStyleClass().add("compose-field");

                    HBox inlineReplyActions = new HBox(8);
                    inlineReplyActions.getStyleClass().add("detail-inline-actions");
                    Button inlineReplySend = new Button("Envoyer");
                    inlineReplySend.getStyleClass().add("compose-publish-btn");
                    inlineReplySend.setOnAction(event -> submitInlineReply(post, comment, inlineReplyField));
                    Button inlineReplyCancel = new Button("Annuler");
                    inlineReplyCancel.getStyleClass().add("compose-cancel-btn");
                    inlineReplyCancel.setOnAction(event -> {
                        inlineReplyBox.setVisible(false);
                        inlineReplyBox.setManaged(false);
                        inlineReplyField.clear();
                    });
                    inlineReplyActions.getChildren().addAll(inlineReplyCancel, inlineReplySend);
                    inlineReplyBox.getChildren().addAll(inlineReplyField, inlineReplyActions);

                    replyBtn.setOnAction(event -> {
                        boolean show = !inlineReplyBox.isVisible();
                        inlineReplyBox.setVisible(show);
                        inlineReplyBox.setManaged(show);
                        if (show) {
                            inlineReplyField.requestFocus();
                        }
                    });

                    commentContent.getChildren().addAll(commentHead, commentBody, commentActions, inlineReplyBox);
                    commentRow.getChildren().addAll(commentAvatar, commentContent);
                    commentsThread.getChildren().add(commentRow);
                }

                commentsSection.getChildren().add(commentsThread);
            }

            postsListBox.getChildren().add(card);
        }
    }

    // Submit inline comment.
    private void submitInlineComment(GroupPost post, TextField field) {
        String body = GroupUiUtils.nullSafe(field.getText()).trim();
        if (body.isBlank()) {
            return;
        }

        try {
            PostComment comment = new PostComment();
            comment.setDepth(0);
            comment.setBody(body);
            comment.setIsBot(false);
            comment.setBotName(null);
            comment.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            comment.setPostId(post.getId());
            comment.setAuthorId(currentUserId());
            comment.setParentCommentId(null);
            commentService.add(comment);
            if (post.getId() != null) {
                expandedCommentPostIds.add(post.getId());
            }
            renderGroupDetails();
        } catch (Exception ex) {
            GroupUiUtils.showError(rootPane.getScene().getWindow(), GroupDetailController.class,
                    "Erreur",
                    "Impossible d'ajouter le commentaire.",
                    ex.getMessage());
        }
    }

    // Save rating and refresh feed.
    private void applyInlineRating(GroupPost post, int rating) {
        try {
            ratingService.rate(post.getId(), currentUserId(), rating);
            renderGroupDetails();
        } catch (Exception ex) {
            GroupUiUtils.showError(rootPane.getScene().getWindow(), GroupDetailController.class,
                    "Erreur",
                    "Impossible d'enregistrer la note.",
                    ex.getMessage());
        }
    }

    // Update temporary star hover/selection state.
    private void applyStarVisualState(List<Button> stars, int selectedValue) {
        for (int i = 0; i < stars.size(); i++) {
            Button star = stars.get(i);
            star.getStyleClass().removeAll("detail-post-star-btn-active", "detail-post-star-btn-inactive");
            if (i < selectedValue) {
                star.getStyleClass().add("detail-post-star-btn-active");
            } else {
                star.getStyleClass().add("detail-post-star-btn-inactive");
            }
        }
    }

    // Submit inline reply linked to parent comment.
    private void submitInlineReply(GroupPost post, PostComment parentComment, TextField field) {
        String body = GroupUiUtils.nullSafe(field.getText()).trim();
        if (body.isBlank()) {
            return;
        }

        try {
            PostComment reply = new PostComment();
            reply.setDepth(parentComment.getDepth() == null ? 1 : parentComment.getDepth() + 1);
            reply.setBody(body);
            reply.setIsBot(false);
            reply.setBotName(null);
            reply.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            reply.setPostId(post.getId());
            reply.setAuthorId(currentUserId());
            reply.setParentCommentId(parentComment.getId());
            commentService.add(reply);
            if (post.getId() != null) {
                expandedCommentPostIds.add(post.getId());
            }
            renderGroupDetails();
        } catch (Exception ex) {
            GroupUiUtils.showError(rootPane.getScene().getWindow(), GroupDetailController.class,
                    "Erreur",
                    "Impossible d'ajouter la reponse.",
                    ex.getMessage());
        }
    }

    // Toggle current user like on post.
    private void onTogglePostLike(GroupPost post) {
        try {
            likeService.toggleLike(post.getId(), currentUserId());
            renderGroupDetails();
        } catch (Exception ex) {
            GroupUiUtils.showError(rootPane.getScene().getWindow(), GroupDetailController.class,
                    "Erreur",
                    "Impossible de mettre a jour le like.",
                    ex.getMessage());
        }
    }


    // Delete post after confirmation.
    private void onDeletePost(GroupPost post) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer la publication");
        confirm.setHeaderText("Confirmer la suppression");
        confirm.setContentText("Voulez-vous supprimer cette publication ?");

        ButtonType deleteType = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(deleteType, cancelType);
        GroupUiUtils.applyDialogStyle(confirm.getDialogPane(), GroupDetailController.class);

        Button deleteButton = (Button) confirm.getDialogPane().lookupButton(deleteType);
        if (deleteButton != null) {
            deleteButton.getStyleClass().add("danger-btn");
            deleteButton.setGraphic(GroupUiUtils.icon("fas-trash-alt", "detail-dialog-danger-icon"));
        }

        Button cancelButton = (Button) confirm.getDialogPane().lookupButton(cancelType);
        if (cancelButton != null) {
            cancelButton.setGraphic(GroupUiUtils.icon("fas-times", "detail-dialog-icon"));
        }

        confirm.showAndWait().ifPresent(result -> {
            if (result == deleteType) {
                try {
                    postService.delete(post.getId());
                    renderGroupDetails();
                    GroupUiUtils.showSuccess(rootPane.getScene().getWindow(), GroupDetailController.class,
                            "Publication supprimee",
                            "La publication a ete supprimee.");
                } catch (Exception ex) {
                    GroupUiUtils.showError(rootPane.getScene().getWindow(), GroupDetailController.class,
                            "Erreur",
                            "Impossible de supprimer la publication.",
                            ex.getMessage());
                }
            }
        });
    }

    // Build readable attachment label.
    private String attachmentLabel(GroupPost post) {
        String attachment = GroupUiUtils.nullSafe(post.getAttachmentUrl()).trim();
        if (attachment.isBlank()) {
            return "Piece jointe";
        }

        boolean isLink = "link".equalsIgnoreCase(post.getPostType())
                || attachment.startsWith("http://")
                || attachment.startsWith("https://");

        if (isLink) {
            return attachment;
        }

        return new File(attachment).getName();
    }

    // Open attachment in browser or file viewer.
    private void openAttachment(String attachmentUrl) {
        String attachment = GroupUiUtils.nullSafe(attachmentUrl).trim();
        if (attachment.isBlank()) {
            return;
        }

        if (!Desktop.isDesktopSupported()) {
            GroupUiUtils.showError(rootPane.getScene().getWindow(), GroupDetailController.class,
                    "Ouverture impossible",
                    "Votre systeme ne permet pas d'ouvrir la piece jointe.",
                    null);
            return;
        }

        try {
            Desktop desktop = Desktop.getDesktop();
            if (attachment.startsWith("http://") || attachment.startsWith("https://")) {
                desktop.browse(URI.create(attachment));
                return;
            }

            File file = new File(attachment);
            if (file.exists()) {
                desktop.open(file);
                return;
            }

            GroupUiUtils.showError(rootPane.getScene().getWindow(), GroupDetailController.class,
                    "Fichier introuvable",
                    "La piece jointe n'existe plus sur ce chemin.",
                    attachment);
        } catch (Exception ex) {
            GroupUiUtils.showError(rootPane.getScene().getWindow(), GroupDetailController.class,
                    "Ouverture impossible",
                    "Impossible d'ouvrir la piece jointe.",
                    ex.getMessage());
        }
    }

    // Navigate back to list page.
    private void navigateToList() {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        AppNavigator.switchTo(stage, "/fxml/groupes/GroupListView.fxml", "Groupes - StudySprint", getClass());
    }

    // Convert role code to display label.
    private String roleLabel(String role) {
        if (role == null || role.isBlank()) {
            return "Membre";
        }
        String lower = role.trim().toLowerCase();
        if ("admin".equals(lower)) {
            return "Admin";
        }
        if ("moderator".equals(lower)) {
            return "Moderateur";
        }
        return "Membre";
    }

    // Map role to badge style class.
    private String detailRoleBadgeClass(String role) {
        if (role == null) {
            return "detail-role-pill-member";
        }

        String lower = role.trim().toLowerCase();
        if ("admin".equals(lower)) {
            return "detail-role-pill-admin";
        }
        if ("moderator".equals(lower) || "moderateur".equals(lower)) {
            return "detail-role-pill-moderator";
        }
        return "detail-role-pill-member";
    }

    // Map role to avatar style class.
    private String detailAvatarClass(String role) {
        if (role == null) {
            return "detail-small-avatar-member";
        }

        String lower = role.trim().toLowerCase();
        if ("admin".equals(lower)) {
            return "detail-small-avatar-admin";
        }
        if ("moderator".equals(lower) || "moderateur".equals(lower)) {
            return "detail-small-avatar-moderator";
        }
        return "detail-small-avatar-member";
    }

    // Map role to composer avatar style class.
    private String detailAiAvatarClass(String role) {
        if (role == null) {
            return "detail-ai-avatar-member";
        }

        String lower = role.trim().toLowerCase();
        if ("admin".equals(lower)) {
            return "detail-ai-avatar-admin";
        }
        if ("moderator".equals(lower) || "moderateur".equals(lower)) {
            return "detail-ai-avatar-moderator";
        }
        return "detail-ai-avatar-member";
    }

    private int currentUserId() {
        Integer userId = SessionManager.getInstance().getCurrentUserId();
        return userId == null ? 1 : userId;
    }

}
