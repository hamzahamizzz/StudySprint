package com.example.studysprint.modules.quizz.controllers;

import com.example.studysprint.modules.quizz.models.Flashcard;
import com.example.studysprint.modules.quizz.models.FlashcardDeck;
import com.example.studysprint.modules.quizz.services.FlashcardService;
import com.example.studysprint.utils.OllamaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class FlashcardBackController implements Initializable {

    private long resolvedOwnerId   = 1L;
    private long resolvedSubjectId = 1L;

    // ── Search + lists ──────────────────────────────────────────────────
    @FXML private TextField txtDeckSearch;
    @FXML private VBox      deckListContainer;
    @FXML private VBox      cardListContainer;
    @FXML private Label     lblCardSectionTitle;

    // ── Deck form ───────────────────────────────────────────────────────
    @FXML private TextField txtDeckTitle;
    @FXML private CheckBox  chkDeckPublished;
    @FXML private Label     lblDeckStatus;

    // ── Card form ───────────────────────────────────────────────────────
    @FXML private TextField txtCardFront;
    @FXML private TextField txtCardBack;
    @FXML private TextField txtCardHint;
    @FXML private Button    btnGenerateHint;
    @FXML private Label     lblCardStatus;

    // ── AI text-to-flashcards ───────────────────────────────────────────
    @FXML private TextArea  courseTextArea;
    @FXML private Spinner<Integer> countSpinner;
    @FXML private Button    generateFromTextBtn;
    @FXML private Label     generateStatusLabel;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final FlashcardService service = new FlashcardService();
    private final OllamaService    ollama  = new OllamaService();

    private FlashcardDeck selectedDeck = null;
    private Flashcard     selectedCard = null;
    private List<FlashcardDeck> allDecks = new ArrayList<>();

    // Live count cache : deck id → nb of cards
    private Map<Long, Integer> deckCardCounts = new HashMap<>();

    // Drag & drop reorder state
    private Flashcard draggedCard = null;
    private final List<Flashcard> currentDeckCards = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        txtDeckSearch.textProperty().addListener((obs, old, nw) -> renderDecks(nw));

        try {
            long uid = service.getFirstUserId();
            long sid = service.getFirstSubjectId();
            if (uid <= 0 || sid <= 0) {
                showDeckStatus("Aucun utilisateur ou sujet trouvé en base. Créez-en un d'abord.", true);
            } else {
                resolvedOwnerId   = uid;
                resolvedSubjectId = sid;
            }
        } catch (SQLException e) {
            showDeckStatus("Erreur DB au démarrage : " + e.getMessage(), true);
        }

        if (btnGenerateHint != null) {
            btnGenerateHint.setDisable(true);
            txtCardFront.textProperty().addListener((o, a, b) -> updateHintBtnState());
            txtCardBack.textProperty().addListener((o, a, b) -> updateHintBtnState());
        }

        if (countSpinner != null) {
            SpinnerValueFactory<Integer> svf =
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(3, 15, 5);
            countSpinner.setValueFactory(svf);
        }

        checkOllamaAvailability();
        loadDecks();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  IA — hint
    // ═══════════════════════════════════════════════════════════════════

    private void checkOllamaAvailability() {
        Thread t = new Thread(() -> {
            boolean ok = ollama.isAvailable();
            Platform.runLater(() -> {
                if (!ok) {
                    if (btnGenerateHint != null) {
                        btnGenerateHint.setDisable(true);
                        btnGenerateHint.setTooltip(new Tooltip("Ollama non disponible (http://localhost:11434)"));
                    }
                    if (generateFromTextBtn != null) {
                        generateFromTextBtn.setDisable(true);
                        generateFromTextBtn.setTooltip(new Tooltip("Ollama non disponible"));
                    }
                } else {
                    if (btnGenerateHint != null) {
                        btnGenerateHint.setTooltip(new Tooltip("Génère un indice via Mistral local"));
                        updateHintBtnState();
                    }
                }
            });
        }, "ollama-check");
        t.setDaemon(true);
        t.start();
    }

    private void updateHintBtnState() {
        boolean canGen = !txtCardFront.getText().isBlank() && !txtCardBack.getText().isBlank();
        btnGenerateHint.setDisable(!canGen);
    }

    @FXML private void handleGenerateHint() {
        final String front = txtCardFront.getText().trim();
        final String back  = txtCardBack.getText().trim();
        if (front.isBlank() || back.isBlank()) return;

        btnGenerateHint.setDisable(true);
        String prevLabel = btnGenerateHint.getText();
        btnGenerateHint.setText("⏳ …");

        Thread t = new Thread(() -> {
            try {
                String hint = ollama.generateFlashcardHint(front, back);
                Platform.runLater(() -> {
                    txtCardHint.setText(hint);
                    showCardStatus("Indice IA généré.", false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> showCardStatus("Erreur IA : " + e.getMessage(), true));
            } finally {
                Platform.runLater(() -> {
                    btnGenerateHint.setText(prevLabel);
                    updateHintBtnState();
                });
            }
        }, "ollama-hint-gen");
        t.setDaemon(true);
        t.start();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  DECKS
    // ═══════════════════════════════════════════════════════════════════

    private void loadDecks() {
        try {
            allDecks = service.getAllDecks();
            // Pré-charge les counts pour affichage live
            deckCardCounts.clear();
            for (FlashcardDeck d : allDecks) {
                try { deckCardCounts.put(d.getId(), service.countFlashcardsByDeck(d.getId())); }
                catch (Exception ignored) {}
            }
            renderDecks(txtDeckSearch.getText());
        } catch (SQLException e) {
            showDeckStatus("Erreur : " + e.getMessage(), true);
        }
    }

    private void renderDecks(String filter) {
        deckListContainer.getChildren().clear();
        selectedDeck = null;
        cardListContainer.getChildren().clear();
        selectedCard = null;
        currentDeckCards.clear();

        String lower = (filter == null) ? "" : filter.toLowerCase();

        List<FlashcardDeck> visible = new ArrayList<>();
        for (FlashcardDeck d : allDecks) {
            if (lower.isBlank() || d.getTitle().toLowerCase().contains(lower)) {
                visible.add(d);
            }
        }

        if (visible.isEmpty()) {
            Label empty = new Label("Aucun deck.");
            empty.setStyle("-fx-text-fill:#B0BBC8;-fx-font-size:13;");
            deckListContainer.getChildren().add(empty);
            return;
        }

        for (FlashcardDeck d : visible) {
            deckListContainer.getChildren().add(buildDeckRow(d));
        }
    }

    private HBox buildDeckRow(FlashcardDeck d) {
        Integer count = deckCardCounts.get(d.getId());
        int nbCards = count != null ? count : 0;

        Label lblTitle = new Label(d.getTitle());
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblTitle.setStyle("-fx-text-fill:#4A5673;");
        lblTitle.setWrapText(true);
        HBox.setHgrow(lblTitle, Priority.ALWAYS);

        Label lblCount = new Label(nbCards + " carte(s)");
        lblCount.setStyle("-fx-font-size:11;-fx-text-fill:#6B7C94;");
        lblCount.getProperties().put("deckId", d.getId());

        Label lblPub = new Label(d.isPublished() ? "✓ publié" : "— brouillon");
        lblPub.setStyle("-fx-font-size:11;-fx-text-fill:" + (d.isPublished() ? "#4BB543" : "#B0BBC8") + ";");

        HBox row = new HBox(10, lblTitle, lblCount, lblPub);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 12, 8, 12));
        applyDeckRowStyle(row, false);

        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color:#F0F4F8;-fx-border-color:#5AAEEF;"
                + "-fx-border-radius:8;-fx-background-radius:8;-fx-cursor:hand;"));
        row.setOnMouseExited(e -> {
            boolean sel = (selectedDeck != null && selectedDeck.getId() == d.getId());
            applyDeckRowStyle(row, sel);
        });
        row.setOnMouseClicked(e -> {
            selectedDeck = d;
            txtDeckTitle.setText(d.getTitle());
            chkDeckPublished.setSelected(d.isPublished());
            lblCardSectionTitle.setText("Cartes du deck : " + d.getTitle());
            lblDeckStatus.setText("");
            for (Node node : deckListContainer.getChildren()) {
                if (node instanceof HBox) applyDeckRowStyle((HBox) node, false);
            }
            applyDeckRowStyle(row, true);
            loadCards(d.getId());
        });

        return row;
    }

    private void applyDeckRowStyle(HBox row, boolean selected) {
        if (selected) {
            row.setStyle("-fx-background-color:#E0F2FE;-fx-border-color:#5AAEEF;-fx-border-width:2;"
                    + "-fx-border-radius:8;-fx-background-radius:8;-fx-cursor:hand;");
        } else {
            row.setStyle("-fx-background-color:#FFFFFF;-fx-border-color:#E5EAF2;"
                    + "-fx-border-radius:8;-fx-background-radius:8;-fx-cursor:hand;");
        }
    }

    /** Met à jour visuellement le compteur de cartes d'un deck sans recharger. */
    private void refreshDeckCountLabel(long deckId, int newCount) {
        deckCardCounts.put(deckId, newCount);
        for (Node node : deckListContainer.getChildren()) {
            if (node instanceof HBox row) {
                for (Node child : row.getChildren()) {
                    if (child instanceof Label l && Long.valueOf(deckId).equals(l.getProperties().get("deckId"))) {
                        l.setText(newCount + " carte(s)");
                        return;
                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  CARDS
    // ═══════════════════════════════════════════════════════════════════

    private void loadCards(long deckId) {
        cardListContainer.getChildren().clear();
        selectedCard = null;
        currentDeckCards.clear();
        try {
            List<Flashcard> cards = service.getFlashcardsByDeck(deckId);
            currentDeckCards.addAll(cards);
            if (cards.isEmpty()) {
                Label empty = new Label("Aucune carte dans ce deck.");
                empty.setStyle("-fx-text-fill:#B0BBC8;-fx-font-size:13;");
                cardListContainer.getChildren().add(empty);
                return;
            }
            for (Flashcard c : cards) {
                cardListContainer.getChildren().add(buildCardRow(c));
            }
        } catch (SQLException e) {
            showCardStatus("Erreur : " + e.getMessage(), true);
        }
    }

    private HBox buildCardRow(Flashcard c) {
        Label lblHandle = new Label("⋮⋮");
        lblHandle.setStyle("-fx-text-fill:#5AAEEF;-fx-font-size:16;-fx-font-weight:bold;-fx-cursor:open-hand;-fx-padding:0 4 0 4;");
        lblHandle.setTooltip(new Tooltip("Glisser pour réordonner"));

        Label lblPos = new Label(String.valueOf(c.getPosition()));
        lblPos.setStyle("-fx-text-fill:#B0BBC8;-fx-font-size:10;-fx-min-width:18;");

        Label lblFront = new Label(c.getFront());
        lblFront.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblFront.setStyle("-fx-text-fill:#4A5673;");
        HBox.setHgrow(lblFront, Priority.ALWAYS);
        lblFront.setMaxWidth(160);

        Label sep = new Label("→");
        sep.setStyle("-fx-text-fill:#B0BBC8;");

        Label lblBack = new Label(c.getBack());
        lblBack.setStyle("-fx-text-fill:#6B7C94;-fx-font-size:12;");
        HBox.setHgrow(lblBack, Priority.ALWAYS);
        lblBack.setMaxWidth(160);

        HBox row = new HBox(8, lblHandle, lblPos, lblFront, sep, lblBack);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(7, 12, 7, 12));
        row.getProperties().put("cardId", c.getId());
        applyCardRowStyle(row, false);

        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color:#F0F4F8;-fx-border-color:#5AAEEF;"
                + "-fx-border-radius:8;-fx-background-radius:8;-fx-cursor:hand;"));
        row.setOnMouseExited(e -> {
            boolean sel = (selectedCard != null && selectedCard.getId() == c.getId());
            applyCardRowStyle(row, sel);
        });
        row.setOnMouseClicked(e -> {
            selectedCard = c;
            txtCardFront.setText(c.getFront());
            txtCardBack.setText(c.getBack());
            txtCardHint.setText(c.getHint() != null ? c.getHint() : "");
            lblCardStatus.setText("");
            for (Node node : cardListContainer.getChildren()) {
                if (node instanceof HBox) applyCardRowStyle((HBox) node, false);
            }
            applyCardRowStyle(row, true);
        });

        // Drag & drop reorder — déclenché uniquement depuis le handle
        installDragAndDrop(row, lblHandle, c);

        return row;
    }

    private void installDragAndDrop(HBox row, Label handle, Flashcard c) {
        // Le drag est initié uniquement quand on commence depuis le handle
        handle.setOnDragDetected((MouseEvent ev) -> {
            draggedCard = c;
            Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(c.getId()));
            db.setContent(content);
            row.setOpacity(0.4);
            ev.consume();
        });

        // La row entière accepte les drops
        row.setOnDragOver((DragEvent ev) -> {
            if (ev.getGestureSource() != row && ev.getDragboard().hasString() && draggedCard != null) {
                ev.acceptTransferModes(TransferMode.MOVE);
            }
            ev.consume();
        });
        row.setOnDragEntered(ev -> {
            if (draggedCard != null && draggedCard.getId() != c.getId()) {
                row.setStyle("-fx-background-color:#DCEEFC;-fx-border-color:#5AAEEF;-fx-border-width:2;"
                        + "-fx-border-radius:8;-fx-background-radius:8;");
            }
            ev.consume();
        });
        row.setOnDragExited(ev -> {
            boolean sel = (selectedCard != null && selectedCard.getId() == c.getId());
            applyCardRowStyle(row, sel);
            ev.consume();
        });
        row.setOnDragDropped((DragEvent ev) -> {
            boolean ok = false;
            if (draggedCard != null && draggedCard.getId() != c.getId()) {
                int from = indexOfCard(draggedCard.getId());
                int to   = indexOfCard(c.getId());
                if (from >= 0 && to >= 0) {
                    Flashcard moved = currentDeckCards.remove(from);
                    currentDeckCards.add(to, moved);
                    persistNewPositions();
                    // Re-render local sans recharger depuis la base (évite la course)
                    rerenderCardList();
                    ok = true;
                }
            }
            ev.setDropCompleted(ok);
            ev.consume();
        });
        row.setOnDragDone(ev -> {
            row.setOpacity(1.0);
            draggedCard = null;
            ev.consume();
        });
    }

    /** Re-render la liste des cartes depuis currentDeckCards sans appel DB. */
    private void rerenderCardList() {
        cardListContainer.getChildren().clear();
        if (currentDeckCards.isEmpty()) {
            Label empty = new Label("Aucune carte dans ce deck.");
            empty.setStyle("-fx-text-fill:#B0BBC8;-fx-font-size:13;");
            cardListContainer.getChildren().add(empty);
            return;
        }
        for (Flashcard c : currentDeckCards) {
            cardListContainer.getChildren().add(buildCardRow(c));
        }
    }

    private int indexOfCard(long id) {
        for (int i = 0; i < currentDeckCards.size(); i++) {
            if (currentDeckCards.get(i).getId() == id) return i;
        }
        return -1;
    }

    private void persistNewPositions() {
        // Étape 1 : positions temporaires (offset 10000) pour éviter tout conflit UNIQUE
        try {
            for (int i = 0; i < currentDeckCards.size(); i++) {
                Flashcard fc = currentDeckCards.get(i);
                service.updatePosition(fc.getId(), 10000 + i + 1);
            }
            // Étape 2 : positions finales 1..N
            for (int i = 0; i < currentDeckCards.size(); i++) {
                Flashcard fc = currentDeckCards.get(i);
                int newPos = i + 1;
                service.updatePosition(fc.getId(), newPos);
                fc.setPosition(newPos);
            }
            showCardStatus("Ordre mis à jour.", false);
        } catch (SQLException e) {
            showCardStatus("Erreur reorder : " + e.getMessage(), true);
        }
    }

    private void applyCardRowStyle(HBox row, boolean selected) {
        if (selected) {
            row.setStyle("-fx-background-color:#E0F2FE;-fx-border-color:#5AAEEF;-fx-border-width:2;"
                    + "-fx-border-radius:8;-fx-background-radius:8;-fx-cursor:hand;");
        } else {
            row.setStyle("-fx-background-color:#FFFFFF;-fx-border-color:#E5EAF2;"
                    + "-fx-border-radius:8;-fx-background-radius:8;-fx-cursor:hand;");
        }
    }

    // ── DECK handlers ────────────────────────────────────────────────────

    @FXML private void handleAddDeck() {
        String title = txtDeckTitle.getText().trim();
        if (title.isBlank()) { showDeckStatus("Titre obligatoire.", true); return; }
        FlashcardDeck d = new FlashcardDeck(resolvedOwnerId, resolvedSubjectId, title);
        d.setPublished(chkDeckPublished.isSelected());
        try {
            service.addDeck(d);
            showDeckStatus("Deck ajouté (id=" + d.getId() + ")", false);
            clearDeckForm();
            loadDecks();
        } catch (SQLException e) {
            showDeckStatus("Erreur : " + e.getMessage(), true);
        }
    }

    @FXML private void handleUpdateDeck() {
        if (selectedDeck == null) { showDeckStatus("Sélectionner un deck.", true); return; }
        String title = txtDeckTitle.getText().trim();
        if (title.isBlank()) { showDeckStatus("Titre obligatoire.", true); return; }
        selectedDeck.setTitle(title);
        selectedDeck.setPublished(chkDeckPublished.isSelected());
        try {
            service.updateDeck(selectedDeck);
            showDeckStatus("Deck mis à jour.", false);
            loadDecks();
        } catch (SQLException e) {
            showDeckStatus("Erreur : " + e.getMessage(), true);
        }
    }

    @FXML private void handleDeleteDeck() {
        if (selectedDeck == null) { showDeckStatus("Sélectionner un deck.", true); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer le deck \"" + selectedDeck.getTitle() + "\" et toutes ses cartes ?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    service.deleteDeck(selectedDeck.getId());
                    showDeckStatus("Deck supprimé.", false);
                    cardListContainer.getChildren().clear();
                    clearDeckForm();
                    loadDecks();
                } catch (SQLException e) {
                    showDeckStatus("Erreur : " + e.getMessage(), true);
                }
            }
        });
    }

    @FXML private void handleRefreshDecks() { loadDecks(); }

    @FXML private void handleClearDeck() { clearDeckForm(); }

    private void clearDeckForm() {
        selectedDeck = null;
        txtDeckTitle.clear();
        chkDeckPublished.setSelected(false);
        lblDeckStatus.setText("");
        for (Node node : deckListContainer.getChildren()) {
            if (node instanceof HBox) applyDeckRowStyle((HBox) node, false);
        }
    }

    // ── CARD handlers ────────────────────────────────────────────────────

    @FXML private void handleAddCard() {
        if (selectedDeck == null) { showCardStatus("Sélectionner un deck d'abord.", true); return; }
        String front = txtCardFront.getText().trim();
        String back  = txtCardBack.getText().trim();
        if (front.isBlank() || back.isBlank()) {
            showCardStatus("Recto et Verso obligatoires.", true); return;
        }
        int nextPos = currentDeckCards.size() + 1;
        Flashcard c = new Flashcard(selectedDeck.getId(), front, back, nextPos);
        String hint = txtCardHint.getText().trim();
        c.setHint(hint.isBlank() ? null : hint);
        try {
            service.addFlashcard(c);
            showCardStatus("Carte ajoutée.", false);
            clearCardForm();
            loadCards(selectedDeck.getId());
            // MAJ live du compteur sans recharger toute la liste de decks
            int newCount = service.countFlashcardsByDeck(selectedDeck.getId());
            refreshDeckCountLabel(selectedDeck.getId(), newCount);
        } catch (SQLException e) {
            showCardStatus("Erreur : " + e.getMessage(), true);
        }
    }

    @FXML private void handleUpdateCard() {
        if (selectedCard == null) { showCardStatus("Sélectionner une carte.", true); return; }
        String front = txtCardFront.getText().trim();
        String back  = txtCardBack.getText().trim();
        if (front.isBlank() || back.isBlank()) {
            showCardStatus("Recto et Verso obligatoires.", true); return;
        }
        selectedCard.setFront(front);
        selectedCard.setBack(back);
        String hint = txtCardHint.getText().trim();
        selectedCard.setHint(hint.isBlank() ? null : hint);
        try {
            service.updateFlashcard(selectedCard);
            showCardStatus("Carte mise à jour.", false);
            loadCards(selectedDeck.getId());
        } catch (SQLException e) {
            showCardStatus("Erreur : " + e.getMessage(), true);
        }
    }

    @FXML private void handleDeleteCard() {
        if (selectedCard == null) { showCardStatus("Sélectionner une carte.", true); return; }
        try {
            long deckId = selectedDeck != null ? selectedDeck.getId() : -1L;
            service.deleteFlashcard(selectedCard.getId());
            showCardStatus("Carte supprimée.", false);
            clearCardForm();
            if (deckId > 0) {
                loadCards(deckId);
                int newCount = service.countFlashcardsByDeck(deckId);
                refreshDeckCountLabel(deckId, newCount);
            }
        } catch (SQLException e) {
            showCardStatus("Erreur : " + e.getMessage(), true);
        }
    }

    @FXML private void handleClearCard() { clearCardForm(); }

    private void clearCardForm() {
        selectedCard = null;
        txtCardFront.clear();
        txtCardBack.clear();
        txtCardHint.clear();
        lblCardStatus.setText("");
        for (Node node : cardListContainer.getChildren()) {
            if (node instanceof HBox) applyCardRowStyle((HBox) node, false);
        }
    }

    // ── Util ─────────────────────────────────────────────────────────────

    private void showDeckStatus(String msg, boolean error) {
        lblDeckStatus.setText(msg);
        lblDeckStatus.getStyleClass().removeAll("status-success", "status-error");
        lblDeckStatus.getStyleClass().add(error ? "status-error" : "status-success");
    }

    private void showCardStatus(String msg, boolean error) {
        lblCardStatus.setText(msg);
        lblCardStatus.getStyleClass().removeAll("status-success", "status-error");
        lblCardStatus.getStyleClass().add(error ? "status-error" : "status-success");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  IA — text-to-flashcards
    // ═══════════════════════════════════════════════════════════════════

    @FXML
    private void handleGenerateFromText() {
        // 1. Validate
        if (!ollama.isAvailable()) {
            showGenerateStatus("❌ Ollama non disponible. Vérifiez que le serveur tourne sur localhost:11434.", true);
            return;
        }
        String text = courseTextArea.getText().trim();
        if (text.isBlank()) {
            showGenerateStatus("❌ Le champ cours est vide.", true);
            return;
        }
        long deckId = selectedDeck != null ? selectedDeck.getId() : 0L;
        if (deckId == 0) {
            showGenerateStatus("❌ Sélectionnez un deck avant de générer.", true);
            return;
        }

        int count = countSpinner.getValue();

        // 2. Disable + feedback
        generateFromTextBtn.setDisable(true);
        generateFromTextBtn.setText("Mistral génère…");
        generateStatusLabel.setVisible(false);
        generateStatusLabel.setManaged(false);

        Thread t = new Thread(() -> {
            try {
                // 3. Call AI
                String raw = ollama.generateFlashcardsFromText(text, count);
                String json = raw.replaceAll("```json", "").replaceAll("```", "").trim();

                JsonNode arr = MAPPER.readTree(json);
                if (!arr.isArray()) throw new RuntimeException("Réponse IA invalide (pas un tableau JSON).");

                int nextPos = getNextPosition(deckId);
                int added = 0;
                for (JsonNode node : arr) {
                    String front = node.path("front").asText("").trim();
                    String back  = node.path("back").asText("").trim();
                    String hint  = node.path("hint").asText("").trim();
                    if (front.isBlank() || back.isBlank()) continue;
                    Flashcard c = new Flashcard(deckId, front, back, nextPos++);
                    c.setHint(hint.isBlank() ? null : hint);
                    service.addFlashcard(c);
                    added++;
                }

                final int finalAdded = added;
                // 4. Success
                Platform.runLater(() -> {
                    generateFromTextBtn.setText("📚 Générer depuis le cours");
                    generateFromTextBtn.setDisable(false);
                    courseTextArea.clear();
                    showGenerateStatus("✅ " + finalAdded + " flashcard(s) générée(s) !", false);
                    if (selectedDeck != null) {
                        loadCards(selectedDeck.getId());
                        try {
                            int newCount = service.countFlashcardsByDeck(selectedDeck.getId());
                            refreshDeckCountLabel(selectedDeck.getId(), newCount);
                        } catch (SQLException ignored) {}
                    }
                });
            } catch (Exception ex) {
                // 5. Error
                Platform.runLater(() -> {
                    generateFromTextBtn.setText("📚 Générer depuis le cours");
                    generateFromTextBtn.setDisable(false);
                    showGenerateStatus("❌ " + ex.getMessage(), true);
                });
            }
        }, "ollama-text-gen");
        t.setDaemon(true);
        t.start();
    }

    private int getNextPosition(long deckId) throws SQLException {
        String sql = "SELECT COALESCE(MAX(position)+1,1) FROM flashcards WHERE deck_id=?";
        try (Connection conn = com.example.studysprint.utils.MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, deckId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 1;
            }
        }
    }

    private void showGenerateStatus(String msg, boolean error) {
        generateStatusLabel.setText(msg);
        generateStatusLabel.setStyle(error
                ? "-fx-text-fill:#D32F2F;-fx-font-size:12;"
                : "-fx-text-fill:#388E3C;-fx-font-size:12;");
        generateStatusLabel.setVisible(true);
        generateStatusLabel.setManaged(true);
    }
}
