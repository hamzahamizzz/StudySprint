package com.example.studysprint.modules.quizz.controllers;

import com.example.studysprint.modules.quizz.models.Flashcard;
import com.example.studysprint.modules.quizz.models.FlashcardDeck;
import com.example.studysprint.modules.quizz.services.FlashcardService;
import com.example.studysprint.utils.OllamaService;
import com.example.studysprint.utils.PDFExportService;
import com.example.studysprint.utils.SM2Algorithm;
import com.example.studysprint.utils.SM2Algorithm.SM2Result;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

public class FlashcardFrontController implements Initializable {

    // ── List panel ─────────────────────────────────────────────────────
    @FXML private VBox      panelList;
    @FXML private TextField txtDeckSearch;
    @FXML private VBox      decksContainer;
    @FXML private Label     lblListStatus;

    // ── Study panel ────────────────────────────────────────────────────
    @FXML private VBox        panelStudy;
    @FXML private Label       lblDeckTitle;
    @FXML private Label       lblProgress;
    @FXML private ProgressBar progressBar;
    @FXML private VBox        flipCard;
    @FXML private Label       lblFront;
    @FXML private Label       lblBack;
    @FXML private Label       lblHint;
    @FXML private Label       lblStudyStatus;

    private final FlashcardService service    = new FlashcardService();
    private final OllamaService    ollama     = new OllamaService();
    private final PDFExportService pdfService = new PDFExportService();

    private long resolvedUserId    = -1L;
    private long resolvedSubjectId = -1L;

    private List<FlashcardDeck> allDecks = new ArrayList<>();
    private FlashcardDeck       selectedDeck = null;
    private List<Flashcard>     cards;
    private int                 cardIndex;
    private boolean             flipped;
    private boolean             animating;

    // Cards already viewed in this study session (for progress bar)
    private final Set<Long> viewed = new HashSet<>();

    // SM-2 review session state
    private final SM2Algorithm sm2 = new SM2Algorithm();
    private List<Flashcard> reviewCards   = new ArrayList<>();
    private int             reviewIndex   = 0;
    private boolean         reviewFlipped = false;
    private boolean         reviewAnimating = false;
    // Programmatic review panel — built in startFlipCardSession(), held as field so showPanel() can toggle it
    private VBox panelReview = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            resolvedUserId    = service.getFirstUserId();
            resolvedSubjectId = service.getFirstSubjectId();
        } catch (SQLException e) {
            lblListStatus.setText("Erreur DB au démarrage : " + e.getMessage());
        }
        txtDeckSearch.textProperty().addListener((obs, old, nw) -> renderDecks(nw));
        loadPublishedDecks();
        showPanel(panelList);
    }

    // ── Data ────────────────────────────────────────────────────────────

    private void loadPublishedDecks() {
        try {
            allDecks = service.getPublishedDecks();
            renderDecks(txtDeckSearch.getText());
        } catch (SQLException e) {
            lblListStatus.setText("Erreur : " + e.getMessage());
        }
    }

    private void renderDecks(String filter) {
        decksContainer.getChildren().clear();
        selectedDeck = null;

        String lower = (filter == null) ? "" : filter.toLowerCase();

        List<FlashcardDeck> visible = new ArrayList<>();
        for (FlashcardDeck d : allDecks) {
            if (lower.isBlank() || d.getTitle().toLowerCase().contains(lower)) {
                visible.add(d);
            }
        }

        if (visible.isEmpty()) {
            Label empty = new Label("Aucun deck disponible.");
            empty.setStyle("-fx-text-fill:#B0BBC8;-fx-font-size:13;");
            decksContainer.getChildren().add(empty);
            return;
        }

        for (FlashcardDeck d : visible) {
            decksContainer.getChildren().add(buildDeckCard(d));
        }
    }

    private VBox buildDeckCard(FlashcardDeck d) {
        int nbCards = 0;
        try { nbCards = service.countFlashcardsByDeck(d.getId()); } catch (Exception ignored) {}

        Label lblTitle = new Label(d.getTitle());
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblTitle.setStyle("-fx-text-fill:#4A5673;");
        lblTitle.setWrapText(true);
        HBox.setHgrow(lblTitle, Priority.ALWAYS);

        Label lblCount = new Label(nbCards + " carte(s)");
        lblCount.setStyle("-fx-text-fill:#6B7C94;-fx-font-size:12;");

        Label lblStudy = new Label("▶ Étudier");
        lblStudy.setStyle("-fx-text-fill:#5AAEEF;-fx-font-weight:bold;-fx-font-size:12;-fx-cursor:hand;");

        // Due count for this deck
        int dueCount = 0;
        try { dueCount = service.getDueCount(d.getId()); } catch (Exception ignored) {}
        final int finalDue = dueCount;

        String reviewStyle = dueCount > 0
                ? "-fx-text-fill:#388E3C;-fx-font-weight:bold;-fx-font-size:12;-fx-cursor:hand;"
                : "-fx-text-fill:#B0BBC8;-fx-font-size:12;-fx-cursor:default;";
        Label lblReview = new Label("🔁 Réviser (" + dueCount + ")");
        lblReview.setStyle(reviewStyle);
        if (dueCount == 0) lblReview.setDisable(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblExport = new Label("📄 Exporter");
        lblExport.setStyle("-fx-text-fill:#E65100;-fx-font-size:12;-fx-cursor:hand;");

        HBox row = new HBox(10, lblTitle, spacer, lblCount, lblStudy, lblReview, lblExport);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(row);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setStyle("-fx-background-color:#FFFFFF;-fx-border-color:#E5EAF2;"
                + "-fx-border-radius:10;-fx-background-radius:10;-fx-cursor:hand;");

        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color:#F0F4F8;-fx-border-color:#5AAEEF;"
                + "-fx-border-radius:10;-fx-background-radius:10;-fx-cursor:hand;"));
        card.setOnMouseExited(e -> {
            boolean sel = (selectedDeck != null && selectedDeck.getId() == d.getId());
            applyDeckStyle(card, sel);
        });
        card.setOnMouseClicked(e -> {
            selectedDeck = d;
            for (Node node : decksContainer.getChildren()) {
                if (node instanceof VBox) {
                    node.setStyle("-fx-background-color:#FFFFFF;-fx-border-color:#E5EAF2;"
                            + "-fx-border-radius:10;-fx-background-radius:10;-fx-cursor:hand;");
                }
            }
            applyDeckStyle(card, true);
        });

        lblStudy.setOnMouseClicked(e -> { selectedDeck = d; handleStudy(); e.consume(); });
        lblReview.setOnMouseClicked(e -> {
            if (finalDue > 0) { selectedDeck = d; handleStartReview(d.getId()); e.consume(); }
        });
        lblExport.setOnMouseClicked(e -> { handleExportDeckPdf(d); e.consume(); });

        return card;
    }

    private void handleExportDeckPdf(FlashcardDeck d) {
        lblListStatus.setText("Génération du PDF…");
        Thread t = new Thread(() -> {
            try {
                List<Flashcard> cards = service.getFlashcardsByDeck(d.getId());
                byte[] pdf = pdfService.exportFlashcardDeck(d, cards);
                String fileName = "deck_" + d.getTitle()
                        .replaceAll("[^\\w\\- ]", "_") + ".pdf";
                pdfService.saveToFile(pdf, fileName);
                Platform.runLater(() -> {
                    lblListStatus.setText("");
                    Alert ok = new Alert(Alert.AlertType.INFORMATION,
                            "PDF sauvegardé dans ~/Documents/" + fileName, ButtonType.OK);
                    ok.setHeaderText(null);
                    ok.showAndWait();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    lblListStatus.setText("Erreur PDF : " + ex.getMessage());
                    new Alert(Alert.AlertType.ERROR,
                            "Erreur export PDF : " + ex.getMessage(), ButtonType.OK).showAndWait();
                });
            }
        }, "pdf-export-deck");
        t.setDaemon(true);
        t.start();
    }

    private void applyDeckStyle(VBox card, boolean selected) {
        if (selected) {
            card.setStyle("-fx-background-color:#E0F2FE;-fx-border-color:#5AAEEF;-fx-border-width:2;"
                    + "-fx-border-radius:10;-fx-background-radius:10;-fx-cursor:hand;");
        } else {
            card.setStyle("-fx-background-color:#FFFFFF;-fx-border-color:#E5EAF2;"
                    + "-fx-border-radius:10;-fx-background-radius:10;-fx-cursor:hand;");
        }
    }

    // ── List panel ──────────────────────────────────────────────────────

    @FXML private void handleStudy() {
        if (selectedDeck == null) { lblListStatus.setText("Cliquer sur un deck pour le sélectionner."); return; }
        try {
            cards = service.getFlashcardsByDeck(selectedDeck.getId());
        } catch (SQLException e) {
            lblListStatus.setText("Erreur : " + e.getMessage()); return;
        }
        if (cards.isEmpty()) { lblListStatus.setText("Ce deck n'a pas de cartes."); return; }

        // Reset session state pour ce deck
        viewed.clear();
        cardIndex = 0;
        flipped   = false;
        lblDeckTitle.setText(selectedDeck.getTitle());
        showPanel(panelStudy);
        renderCard();
    }

    @FXML private void handleRefresh() { loadPublishedDecks(); lblListStatus.setText(""); }

    // ── Study panel ──────────────────────────────────────────────────────

    @FXML private void handleFlip() {
        if (cards == null || cards.isEmpty() || animating) return;
        animating = true;

        Rotate rotator = new Rotate(0, flipCard.getWidth() / 2.0, flipCard.getHeight() / 2.0, 0, Rotate.Y_AXIS);
        flipCard.getTransforms().setAll(rotator);

        RotateTransition first = new RotateTransition(Duration.millis(150), flipCard);
        first.setAxis(Rotate.Y_AXIS);
        first.setFromAngle(0);
        first.setToAngle(90);

        first.setOnFinished(ev -> {
            flipped = !flipped;
            renderCardFace();
            RotateTransition second = new RotateTransition(Duration.millis(150), flipCard);
            second.setAxis(Rotate.Y_AXIS);
            second.setFromAngle(-90);
            second.setToAngle(0);
            second.setOnFinished(e2 -> animating = false);
            second.play();
        });
        first.play();
    }

    @FXML private void handleNext() {
        if (cards == null || cards.isEmpty()) return;
        cardIndex = (cardIndex + 1) % cards.size();
        flipped   = false;
        renderCard();
    }

    @FXML private void handlePrev() {
        if (cards == null || cards.isEmpty()) return;
        cardIndex = (cardIndex - 1 + cards.size()) % cards.size();
        flipped   = false;
        renderCard();
    }

    @FXML private void handleBackToList() {
        showPanel(panelList);
        loadPublishedDecks();
    }

    /** Affiche la carte courante + reset l'angle + met à jour la progress bar. */
    private void renderCard() {
        if (cards == null || cards.isEmpty()) return;
        flipCard.getTransforms().clear();
        flipCard.setRotationAxis(Rotate.Y_AXIS);
        flipCard.setRotate(0);

        viewed.add(cards.get(cardIndex).getId());
        renderCardFace();

        int total = cards.size();
        lblProgress.setText("Carte " + (cardIndex + 1) + " / " + total + "  —  Vues : " + viewed.size() + "/" + total);
        if (progressBar != null) {
            progressBar.setProgress(total == 0 ? 0 : (viewed.size() * 1.0 / total));
        }
        lblStudyStatus.setText("");
    }

    /** Met à jour seulement le contenu textuel de la face (utilisé pendant l'anim). */
    private void renderCardFace() {
        if (cards == null || cards.isEmpty()) return;
        Flashcard c = cards.get(cardIndex);

        lblFront.setText(c.getFront());

        if (flipped) {
            lblBack.setText(c.getBack());
            lblBack.setStyle("-fx-font-size:14;-fx-text-fill:#4A5673;");
            String hint = c.getHint();
            lblHint.setText((hint != null && !hint.isBlank()) ? "Indice : " + hint : "");
        } else {
            lblBack.setText("[ cliquer sur Retourner ]");
            lblBack.setStyle("-fx-font-size:13;-fx-text-fill:#B0BBC8;");
            lblHint.setText("");
        }
    }

    // ── Panel switching ──────────────────────────────────────────────────

    private void showPanel(VBox panel) {
        List<VBox> panels = new ArrayList<>(List.of(panelList, panelStudy));
        if (panelReview != null) panels.add(panelReview);
        for (VBox p : panels) {
            boolean show = (p == panel);
            p.setVisible(show);
            p.setManaged(show);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SM-2 REVIEW SESSION
    // ═══════════════════════════════════════════════════════════════════

    private void handleStartReview(long deckId) {
        try {
            List<Flashcard> due = service.getDueFlashcards(deckId);
            if (due.isEmpty()) {
                LocalDate next = service.getNextSessionDate(deckId);
                String msg = next != null
                        ? "Toutes les cartes sont à jour ! Prochaine révision : " + next
                        : "Aucune carte à réviser pour l'instant.";
                new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
                return;
            }
            startFlipCardSession(due);
        } catch (SQLException e) {
            lblListStatus.setText("Erreur : " + e.getMessage());
        }
    }

    private void startFlipCardSession(List<Flashcard> cards) {
        reviewCards    = new ArrayList<>(cards);
        reviewIndex    = 0;
        reviewFlipped  = false;
        reviewAnimating = false;

        // ── Build the review panel programmatically
        // Progress row
        Label lblReviewTitle = new Label(selectedDeck != null ? selectedDeck.getTitle() : "Révision");
        lblReviewTitle.setStyle("-fx-font-size:16;-fx-font-weight:bold;-fx-text-fill:#4A5673;");
        Label lblReviewProg = new Label("");
        lblReviewProg.setStyle("-fx-font-size:12;-fx-text-fill:#6B7C94;"
                + "-fx-background-color:#F0F4F8;-fx-padding:4 12;-fx-background-radius:20;");
        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);
        HBox reviewHeader = new HBox(12, lblReviewTitle, hSpacer, lblReviewProg);
        reviewHeader.setAlignment(Pos.CENTER_LEFT);
        reviewHeader.setStyle("-fx-background-color:#FFFFFF;"
                + "-fx-border-color:transparent transparent #E5EAF2 transparent;"
                + "-fx-border-width:0 0 1 0;-fx-padding:14 24;");

        ProgressBar reviewPB = new ProgressBar(0);
        reviewPB.setMaxWidth(Double.MAX_VALUE);
        reviewPB.setStyle("-fx-pref-height:3;-fx-background-color:#E5EAF2;");

        // Mastery bar
        Label lblMasteryTxt = new Label("Maîtrise :");
        lblMasteryTxt.setStyle("-fx-font-size:11;-fx-text-fill:#6B7C94;");
        ProgressBar masteryBar = new ProgressBar(0);
        masteryBar.setPrefWidth(120);
        masteryBar.setPrefHeight(8);
        Label lblMasteryPct = new Label("0%");
        lblMasteryPct.setStyle("-fx-font-size:11;-fx-text-fill:#6B7C94;");
        HBox masteryRow = new HBox(8, lblMasteryTxt, masteryBar, lblMasteryPct);
        masteryRow.setAlignment(Pos.CENTER_LEFT);

        // Front face
        Label lblFrontTag = new Label("RECTO");
        lblFrontTag.setStyle("-fx-font-size:10;-fx-text-fill:#B0BBC8;-fx-font-weight:bold;-fx-letter-spacing:0.12em;");
        Label lblRFront = new Label("");
        lblRFront.setWrapText(true);
        lblRFront.setMaxWidth(580);
        lblRFront.setAlignment(Pos.CENTER);
        lblRFront.setStyle("-fx-font-size:20;-fx-font-weight:bold;-fx-text-fill:#4A5673;");
        VBox frontFace = new VBox(10, lblFrontTag, lblRFront);
        frontFace.setAlignment(Pos.CENTER);

        // Back face
        Label lblBackTag = new Label("VERSO");
        lblBackTag.setStyle("-fx-font-size:10;-fx-text-fill:#B0BBC8;-fx-font-weight:bold;-fx-letter-spacing:0.12em;");
        Label lblRBack = new Label("");
        lblRBack.setWrapText(true);
        lblRBack.setMaxWidth(580);
        lblRBack.setAlignment(Pos.CENTER);
        lblRBack.setStyle("-fx-font-size:16;-fx-text-fill:#4A5673;");
        Label lblRHint = new Label("");
        lblRHint.setStyle("-fx-font-style:italic;-fx-text-fill:#B0BBC8;-fx-font-size:11;");
        VBox backFace = new VBox(10, lblBackTag, lblRBack, lblRHint);
        backFace.setAlignment(Pos.CENTER);
        backFace.setVisible(false);
        backFace.setManaged(false);

        // Card StackPane
        StackPane cardPane = new StackPane(frontFace, backFace);
        cardPane.setMinHeight(250);
        cardPane.setMaxWidth(680);
        cardPane.setStyle("-fx-background-color:#FFFFFF;-fx-border-color:#E5EAF2;"
                + "-fx-border-radius:16;-fx-background-radius:16;"
                + "-fx-effect:dropshadow(gaussian,rgba(90,174,239,0.15),12,0,0,4);");
        cardPane.setPadding(new Insets(32));

        // Flip button
        Button btnFlip = new Button("Retourner");
        btnFlip.getStyleClass().add("btn-primary");
        btnFlip.setStyle("-fx-padding:10 28;");

        // Quality buttons (hidden until flipped)
        Button btnBlackout = new Button("Blackout (0)");
        btnBlackout.setStyle("-fx-background-color:#D32F2F;-fx-text-fill:white;-fx-background-radius:6;-fx-padding:8 12;");
        Button btnDiff = new Button("Difficile (2)");
        btnDiff.setStyle("-fx-background-color:#EF6C00;-fx-text-fill:white;-fx-background-radius:6;-fx-padding:8 12;");
        Button btnCorrect = new Button("Correct (4)");
        btnCorrect.setStyle("-fx-background-color:#689F38;-fx-text-fill:white;-fx-background-radius:6;-fx-padding:8 12;");
        Button btnPerfect = new Button("Parfait (5)");
        btnPerfect.setStyle("-fx-background-color:#2E7D32;-fx-text-fill:white;-fx-background-radius:6;-fx-padding:8 12;");
        HBox qualityBox = new HBox(10, btnBlackout, btnDiff, btnCorrect, btnPerfect);
        qualityBox.setAlignment(Pos.CENTER);
        qualityBox.setVisible(false);
        qualityBox.setManaged(false);

        Button btnBackFromReview = new Button("← Retour à la liste");
        btnBackFromReview.getStyleClass().add("btn-secondary");

        VBox centerArea = new VBox(24, masteryRow, cardPane, btnFlip, qualityBox, btnBackFromReview);
        centerArea.setAlignment(Pos.CENTER);
        centerArea.setPadding(new Insets(32, 60, 32, 60));
        VBox.setVgrow(centerArea, Priority.ALWAYS);

        panelReview = new VBox(reviewHeader, reviewPB, centerArea);
        panelReview.setStyle("-fx-background-color:#F7FFFC;");
        VBox.setVgrow(centerArea, Priority.ALWAYS);

        // Inject into the StackPane (same parent as panelList/panelStudy)
        StackPane root = (StackPane) panelList.getParent();
        if (!root.getChildren().contains(panelReview)) root.getChildren().add(panelReview);

        // Helpers to refresh the review card display
        Runnable renderReviewCard = () -> {
            if (reviewCards.isEmpty()) return;
            Flashcard c = reviewCards.get(reviewIndex);
            lblRFront.setText(c.getFront());
            lblRBack.setText(c.getBack());
            String hint = c.getHint();
            lblRHint.setText(hint != null && !hint.isBlank() ? "Indice : " + hint : "");

            // Reset faces
            frontFace.setVisible(true);  frontFace.setManaged(true);
            backFace.setVisible(false);  backFace.setManaged(false);
            qualityBox.setVisible(false); qualityBox.setManaged(false);
            btnFlip.setVisible(true);    btnFlip.setManaged(true);
            reviewFlipped = false;

            lblReviewProg.setText("Carte " + (reviewIndex + 1) + "/" + reviewCards.size());
            reviewPB.setProgress((double)(reviewIndex) / reviewCards.size());

            int mastery = sm2.getMasteryPercent(c.getRepetitions(), c.getEaseFactor());
            masteryBar.setProgress(mastery / 100.0);
            lblMasteryPct.setText(mastery + "%");
            String mbColor = mastery >= 70 ? "#2E7D32" : mastery >= 30 ? "#EF6C00" : "#D32F2F";
            masteryBar.setStyle("-fx-accent:" + mbColor + ";-fx-pref-height:8;");
        };
        renderReviewCard.run();

        // Flip animation
        btnFlip.setOnAction(ev -> {
            if (reviewAnimating) return;
            reviewAnimating = true;
            RotateTransition r1 = new RotateTransition(Duration.millis(150), cardPane);
            r1.setAxis(Rotate.Y_AXIS);
            r1.setFromAngle(0); r1.setToAngle(90);
            r1.setOnFinished(e -> {
                reviewFlipped = true;
                frontFace.setVisible(false); frontFace.setManaged(false);
                backFace.setVisible(true);   backFace.setManaged(true);
                qualityBox.setVisible(true); qualityBox.setManaged(true);
                btnFlip.setVisible(false);   btnFlip.setManaged(false);
                RotateTransition r2 = new RotateTransition(Duration.millis(150), cardPane);
                r2.setAxis(Rotate.Y_AXIS);
                r2.setFromAngle(-90); r2.setToAngle(0);
                r2.setOnFinished(e2 -> reviewAnimating = false);
                r2.play();
            });
            r1.play();
        });

        // Quality handlers
        btnBlackout.setOnAction(ev -> handleQuality(0, renderReviewCard));
        btnDiff.setOnAction(ev     -> handleQuality(2, renderReviewCard));
        btnCorrect.setOnAction(ev  -> handleQuality(4, renderReviewCard));
        btnPerfect.setOnAction(ev  -> handleQuality(5, renderReviewCard));

        btnBackFromReview.setOnAction(ev -> {
            showPanel(panelList);
            loadPublishedDecks();
        });

        showPanel(panelReview);
    }

    private void handleQuality(int quality, Runnable renderReviewCard) {
        if (reviewCards.isEmpty() || reviewIndex >= reviewCards.size()) return;
        Flashcard card = reviewCards.get(reviewIndex);

        SM2Result result = sm2.calculate(quality, card.getRepetitions(),
                card.getEaseFactor(), card.getIntervalDays());

        // Update in-memory immediately so mastery bar refreshes
        card.setRepetitions(result.newRepetitions());
        card.setEaseFactor(result.newEaseFactor());
        card.setIntervalDays(result.newIntervalDays());
        card.setNextReview(result.nextReview());

        // Persist asynchronously
        long cardId = card.getId();
        Thread t = new Thread(() -> {
            try { service.updateSM2(cardId, result); } catch (SQLException ignored) {}
        }, "sm2-persist");
        t.setDaemon(true);
        t.start();

        reviewIndex++;
        if (reviewIndex >= reviewCards.size()) {
            showSessionEnd();
        } else {
            renderReviewCard.run();
        }
    }

    private void showSessionEnd() {
        // Build end-of-session summary screen inline
        Label lblDone = new Label("✅ Session terminée !");
        lblDone.setStyle("-fx-font-size:24;-fx-font-weight:bold;-fx-text-fill:#2E7D32;");

        int total = reviewCards.size();
        long mastered = reviewCards.stream()
                .filter(c -> sm2.getMasteryPercent(c.getRepetitions(), c.getEaseFactor()) >= 70)
                .count();
        Label lblSummary = new Label(mastered + "/" + total + " cartes maîtrisées (≥70%)");
        lblSummary.setStyle("-fx-font-size:15;-fx-text-fill:#4A5673;");

        Label lblNextDate = new Label("Chargement de la prochaine date…");
        lblNextDate.setStyle("-fx-font-size:13;-fx-text-fill:#6B7C94;");

        if (selectedDeck != null) {
            Thread t = new Thread(() -> {
                try {
                    LocalDate next = service.getNextSessionDate(selectedDeck.getId());
                    String msg = next != null
                            ? "Prochaine session : " + next
                            : "Toutes les cartes sont maîtrisées !";
                    Platform.runLater(() -> lblNextDate.setText(msg));
                } catch (SQLException ignored) {}
            }, "sm2-next-date");
            t.setDaemon(true);
            t.start();
        }

        Button btnRetour = new Button("← Retour à la liste");
        btnRetour.getStyleClass().add("btn-secondary");
        btnRetour.setOnAction(e -> { showPanel(panelList); loadPublishedDecks(); });

        VBox summary = new VBox(20, lblDone, lblSummary, lblNextDate, btnRetour);
        summary.setAlignment(Pos.CENTER);
        summary.setPadding(new Insets(60));
        summary.setStyle("-fx-background-color:#F7FFFC;");
        VBox.setVgrow(summary, Priority.ALWAYS);

        if (panelReview != null) {
            panelReview.getChildren().setAll(summary);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  CRUD étudiant — Création / Modification / Suppression
    // ═══════════════════════════════════════════════════════════════════

    @FXML private void handleCreateMyDeck() {
        if (resolvedUserId <= 0 || resolvedSubjectId <= 0) {
            lblListStatus.setText("Aucun utilisateur/sujet en base.");
            return;
        }
        FlashcardDeck draft = new FlashcardDeck(resolvedUserId, resolvedSubjectId, "");
        draft.setPublished(true);
        if (showDeckEditor(draft, false, new ArrayList<>())) {
            // Création déjà persistée à l'intérieur de la dialog (deck créé avant les cartes)
            loadPublishedDecks();
            lblListStatus.setText("Deck créé.");
        }
    }

    @FXML private void handleEditMyDeck() {
        if (selectedDeck == null) { lblListStatus.setText("Sélectionner un deck à modifier."); return; }
        if (selectedDeck.getOwnerId() != resolvedUserId) {
            lblListStatus.setText("Vous ne pouvez modifier que vos propres decks.");
            return;
        }
        try {
            List<Flashcard> existing = service.getFlashcardsByDeck(selectedDeck.getId());
            if (showDeckEditor(selectedDeck, true, existing)) {
                loadPublishedDecks();
                lblListStatus.setText("Deck modifié.");
            }
        } catch (SQLException e) {
            lblListStatus.setText("Erreur : " + e.getMessage());
        }
    }

    @FXML private void handleDeleteMyDeck() {
        if (selectedDeck == null) { lblListStatus.setText("Sélectionner un deck à supprimer."); return; }
        if (selectedDeck.getOwnerId() != resolvedUserId) {
            lblListStatus.setText("Vous ne pouvez supprimer que vos propres decks.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer \"" + selectedDeck.getTitle() + "\" et toutes ses cartes ?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    service.deleteDeck(selectedDeck.getId());
                    lblListStatus.setText("Deck supprimé.");
                    loadPublishedDecks();
                } catch (SQLException e) {
                    lblListStatus.setText("Erreur : " + e.getMessage());
                }
            }
        });
    }

    /**
     * Ouvre la dialog d'édition de deck (create ou edit).
     * Persiste : crée le deck si nouveau, met à jour sinon, et synchronise les cartes (add/update/delete).
     * Le bouton IA génère un indice pour la carte en cours d'édition.
     */
    private boolean showDeckEditor(FlashcardDeck target, boolean editMode, List<Flashcard> existingCards) {
        // ── State local
        // Cartes "en cours" : nouvelles + existantes (référencées par id si elles viennent de la base)
        final List<EditableCard> editableCards = new ArrayList<>();
        for (Flashcard c : existingCards) editableCards.add(EditableCard.fromExisting(c));
        final ObservableList<String> previews = FXCollections.observableArrayList();
        for (int i = 0; i < editableCards.size(); i++) previews.add(formatPreview(i, editableCards.get(i)));

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(editMode ? "Modifier mon deck" : "Créer mon deck");
        dlg.setHeaderText(null);
        ButtonType saveBt   = new ButtonType(editMode ? "Enregistrer" : "Créer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBt = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dlg.getDialogPane().getButtonTypes().addAll(saveBt, cancelBt);
        try {
            dlg.getDialogPane().getScene().getStylesheets()
                    .add(getClass().getResource("/css/studysprint.css").toExternalForm());
        } catch (Exception ignored) {}

        // ── Form widgets — meta deck
        TextField txtDeckTitle = new TextField(target.getTitle() == null ? "" : target.getTitle());
        txtDeckTitle.setPromptText("Nom du deck");
        CheckBox chkPub = new CheckBox("Publié");
        chkPub.setSelected(target.isPublished());

        // ── Form widgets — carte courante
        TextField txtFront = new TextField();   txtFront.setPromptText("Recto / question");
        TextField txtBack  = new TextField();   txtBack.setPromptText("Verso / réponse");
        TextField txtHint  = new TextField();   txtHint.setPromptText("Indice (optionnel)");

        Button btnHintAi = new Button("💡  Hint IA");
        btnHintAi.getStyleClass().add("btn-secondary");
        Label lblAiStatus = new Label("");
        lblAiStatus.setStyle("-fx-text-fill:#6B7C94;-fx-font-size:11;");

        // Vérifie disponibilité Ollama
        Thread tCheck = new Thread(() -> {
            boolean ok = ollama.isAvailable();
            Platform.runLater(() -> {
                if (!ok) {
                    btnHintAi.setDisable(true);
                    btnHintAi.setTooltip(new Tooltip("Ollama non disponible"));
                    lblAiStatus.setText("IA indisponible");
                }
            });
        }, "ollama-check-fc-front");
        tCheck.setDaemon(true);
        tCheck.start();

        ListView<String> listView = new ListView<>(previews);
        listView.setPrefHeight(140);

        Label lblCount = new Label(editableCards.size() + " carte(s)");
        lblCount.setStyle("-fx-text-fill:#6B7C94;-fx-font-size:11;");

        Button btnAddCard = new Button("+ Ajouter");
        btnAddCard.getStyleClass().add("btn-primary");
        Button btnUpdateCard = new Button("✏ Mettre à jour");
        btnUpdateCard.getStyleClass().add("btn-warning");
        btnUpdateCard.setDisable(true);
        Button btnDelCard = new Button("🗑 Retirer");
        btnDelCard.getStyleClass().add("btn-danger");
        btnDelCard.setDisable(true);
        Button btnClearCard = new Button("Vider");
        btnClearCard.getStyleClass().add("btn-secondary");

        final int[] selectedIdx = { -1 };
        Runnable clearCardForm = () -> {
            txtFront.clear(); txtBack.clear(); txtHint.clear();
            selectedIdx[0] = -1;
            btnUpdateCard.setDisable(true);
            btnDelCard.setDisable(true);
            listView.getSelectionModel().clearSelection();
        };

        listView.getSelectionModel().selectedIndexProperty().addListener((obs, o, n) -> {
            int idx = n.intValue();
            if (idx < 0 || idx >= editableCards.size()) return;
            EditableCard ec = editableCards.get(idx);
            txtFront.setText(ec.front);
            txtBack.setText(ec.back);
            txtHint.setText(ec.hint == null ? "" : ec.hint);
            selectedIdx[0] = idx;
            btnUpdateCard.setDisable(false);
            btnDelCard.setDisable(false);
        });

        btnAddCard.setOnAction(e -> {
            String f = txtFront.getText().trim();
            String b = txtBack.getText().trim();
            if (f.isEmpty() || b.isEmpty()) { lblAiStatus.setText("Recto + Verso obligatoires."); return; }
            String h = txtHint.getText().trim();
            EditableCard ec = EditableCard.fresh(f, b, h.isBlank() ? null : h);
            editableCards.add(ec);
            previews.add(formatPreview(editableCards.size() - 1, ec));
            lblCount.setText(editableCards.size() + " carte(s)");
            clearCardForm.run();
            lblAiStatus.setText("");
        });

        btnUpdateCard.setOnAction(e -> {
            int idx = selectedIdx[0];
            if (idx < 0 || idx >= editableCards.size()) return;
            String f = txtFront.getText().trim();
            String b = txtBack.getText().trim();
            if (f.isEmpty() || b.isEmpty()) { lblAiStatus.setText("Recto + Verso obligatoires."); return; }
            String h = txtHint.getText().trim();
            EditableCard ec = editableCards.get(idx);
            ec.front = f;
            ec.back  = b;
            ec.hint  = h.isBlank() ? null : h;
            ec.dirty = true;
            previews.set(idx, formatPreview(idx, ec));
            clearCardForm.run();
            lblAiStatus.setText("Carte mise à jour (en attente d'enregistrement).");
        });

        btnDelCard.setOnAction(e -> {
            int idx = selectedIdx[0];
            if (idx < 0 || idx >= editableCards.size()) return;
            EditableCard ec = editableCards.remove(idx);
            previews.remove(idx);
            if (ec.existingId != null) deletedExistingIds.add(ec.existingId);
            // Re-render previews avec nouvelles positions
            previews.clear();
            for (int i = 0; i < editableCards.size(); i++) previews.add(formatPreview(i, editableCards.get(i)));
            lblCount.setText(editableCards.size() + " carte(s)");
            clearCardForm.run();
        });

        btnClearCard.setOnAction(e -> clearCardForm.run());

        btnHintAi.setOnAction(e -> {
            String f = txtFront.getText().trim();
            String b = txtBack.getText().trim();
            if (f.isBlank() || b.isBlank()) { lblAiStatus.setText("Renseigne recto + verso pour l'IA."); return; }
            btnHintAi.setDisable(true);
            String prev = btnHintAi.getText();
            btnHintAi.setText("⏳…");
            lblAiStatus.setText("Génération…");
            Thread t = new Thread(() -> {
                try {
                    String hint = ollama.generateFlashcardHint(f, b);
                    Platform.runLater(() -> {
                        txtHint.setText(hint);
                        lblAiStatus.setText("Indice généré.");
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> lblAiStatus.setText("Erreur IA."));
                } finally {
                    Platform.runLater(() -> {
                        btnHintAi.setDisable(false);
                        btnHintAi.setText(prev);
                    });
                }
            }, "ollama-front-hint-gen");
            t.setDaemon(true);
            t.start();
        });

        // ── Layout
        VBox metaBox = new VBox(8,
                buildLabelFront("DECK"),
                buildLabelFront("TITRE *"), txtDeckTitle,
                chkPub);
        metaBox.getStyleClass().add("card");

        HBox hintRow = new HBox(8, txtHint, btnHintAi);
        HBox.setHgrow(txtHint, Priority.ALWAYS);

        VBox cardBox = new VBox(8,
                buildLabelFront("CARTE EN COURS"),
                buildLabelFront("RECTO *"), txtFront,
                buildLabelFront("VERSO *"), txtBack,
                buildLabelFront("INDICE"), hintRow,
                new HBox(8, btnAddCard, btnUpdateCard, btnDelCard, btnClearCard),
                lblAiStatus,
                buildLabelFront("CARTES DU DECK"),
                lblCount, listView);
        cardBox.getStyleClass().add("card");

        VBox content = new VBox(14, metaBox, cardBox);
        content.setPadding(new Insets(8));
        content.setPrefWidth(560);

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setPrefHeight(560);
        sp.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;");

        dlg.getDialogPane().setContent(sp);

        // ── Validation + persistance
        var saveButton = (Button) dlg.getDialogPane().lookupButton(saveBt);
        saveButton.addEventFilter(ActionEvent.ACTION, ev -> {
            if (txtDeckTitle.getText().isBlank()) {
                lblAiStatus.setText("Le titre du deck est obligatoire.");
                ev.consume();
                return;
            }
            // Persistance
            try {
                target.setTitle(txtDeckTitle.getText().trim());
                target.setPublished(chkPub.isSelected());
                if (!editMode) {
                    service.addDeck(target);
                } else {
                    service.updateDeck(target);
                }
                // Sync cartes : delete -> update -> insert
                for (Long id : deletedExistingIds) service.deleteFlashcard(id);
                deletedExistingIds.clear();

                for (int i = 0; i < editableCards.size(); i++) {
                    EditableCard ec = editableCards.get(i);
                    int pos = i + 1;
                    if (ec.existingId == null) {
                        Flashcard fc = new Flashcard(target.getId(), ec.front, ec.back, pos);
                        fc.setHint(ec.hint);
                        service.addFlashcard(fc);
                    } else if (ec.dirty || ec.position != pos) {
                        Flashcard fc = new Flashcard();
                        fc.setId(ec.existingId);
                        fc.setDeckId(target.getId());
                        fc.setFront(ec.front);
                        fc.setBack(ec.back);
                        fc.setHint(ec.hint);
                        fc.setPosition(pos);
                        service.updateFlashcard(fc);
                    }
                }
            } catch (SQLException ex) {
                lblAiStatus.setText("Erreur DB : " + ex.getMessage());
                ev.consume();
            }
        });

        var result = dlg.showAndWait();
        return result.isPresent() && result.get() == saveBt;
    }

    private final List<Long> deletedExistingIds = new ArrayList<>();

    private String formatPreview(int idx, EditableCard ec) {
        return (idx + 1) + ". " + ec.front + "  →  " + ec.back +
               (ec.existingId == null ? "  [nouvelle]" : "");
    }

    private Label buildLabelFront(String txt) {
        Label l = new Label(txt);
        l.setStyle("-fx-font-size:11;-fx-font-weight:bold;-fx-text-fill:#6B7C94;-fx-letter-spacing:0.06em;");
        return l;
    }

    /** Représentation interne d'une carte en cours d'édition. */
    private static class EditableCard {
        Long    existingId;   // null si nouvelle
        String  front, back, hint;
        int     position;
        boolean dirty;

        static EditableCard fresh(String f, String b, String h) {
            EditableCard ec = new EditableCard();
            ec.front = f; ec.back = b; ec.hint = h;
            return ec;
        }
        static EditableCard fromExisting(Flashcard c) {
            EditableCard ec = new EditableCard();
            ec.existingId = c.getId();
            ec.front = c.getFront();
            ec.back  = c.getBack();
            ec.hint  = c.getHint();
            ec.position = c.getPosition();
            return ec;
        }
    }
}
