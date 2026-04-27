package com.example.studysprint.modules.quizz.controllers;

import com.example.studysprint.modules.quizz.models.Flashcard;
import com.example.studysprint.modules.quizz.models.FlashcardDeck;
import com.example.studysprint.modules.quizz.services.FlashcardService;
import javafx.animation.RotateTransition;
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

    private final FlashcardService service = new FlashcardService();

    private List<FlashcardDeck> allDecks = new ArrayList<>();
    private FlashcardDeck       selectedDeck = null;
    private List<Flashcard>     cards;
    private int                 cardIndex;
    private boolean             flipped;
    private boolean             animating;

    // Cards already viewed in this study session (for progress bar)
    private final Set<Long> viewed = new HashSet<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
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

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(10, lblTitle, spacer, lblCount, lblStudy);
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

        return card;
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
        for (VBox p : new VBox[]{panelList, panelStudy}) {
            boolean show = (p == panel);
            p.setVisible(show);
            p.setManaged(show);
        }
    }
}
