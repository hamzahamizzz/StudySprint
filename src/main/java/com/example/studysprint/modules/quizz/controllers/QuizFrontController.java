package com.example.studysprint.modules.quizz.controllers;

import com.example.studysprint.modules.quizz.models.Difficulty;
import com.example.studysprint.modules.quizz.models.Quiz;
import com.example.studysprint.modules.quizz.models.QuizAttempt;
import com.example.studysprint.modules.quizz.models.QuizRating;
import com.example.studysprint.modules.quizz.services.QuizService;
import com.example.studysprint.utils.AdaptiveEngine;
import com.example.studysprint.utils.AdaptiveEngine.AdaptiveRecommendation;
import com.example.studysprint.utils.EmailService;
import com.example.studysprint.utils.OllamaService;
import com.example.studysprint.utils.PDFExportService;
import com.example.studysprint.utils.RecommendationEngine;
import com.example.studysprint.utils.ScorePredictionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.util.Duration;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

public class QuizFrontController implements Initializable {

    // ── List panel ─────────────────────────────────────────────────────
    @FXML private VBox            panelList;
    @FXML private TextField       txtSearch;
    @FXML private ComboBox<String> cmbFilterDifficulty;
    @FXML private ComboBox<String> cmbSort;
    @FXML private VBox            cardsContainer;
    @FXML private Slider          sliderRating;
    @FXML private Label           lblRatingVal;
    @FXML private Label           lblListStatus;

    // ── Quiz panel ─────────────────────────────────────────────────────
    @FXML private VBox   panelQuiz;
    @FXML private Label  lblQuizTitle;
    @FXML private Label  lblProgress;
    @FXML private Label  lblDifficulty;
    @FXML private Label  lblQuestion;
    @FXML private VBox   vboxOptions;
    @FXML private Label  lblFeedback;
    @FXML private Button btnValidate;
    @FXML private Button btnNext;
    @FXML private Label  lblQuizStatus;

    // ── Score panel ────────────────────────────────────────────────────
    @FXML private VBox     panelScore;
    @FXML private Label    lblScore;
    @FXML private Label    lblScoreDetail;
    @FXML private TextArea txtAiFeedback;
    @FXML private Button   btnEmailResult;
    @FXML private Button   btnExportPdf;

    private final QuizService     service     = new QuizService();
    private final OllamaService   ollama      = new OllamaService();
    private final AdaptiveEngine  adaptive    = new AdaptiveEngine();
    private final EmailService    email       = new EmailService();
    private final PDFExportService    pdfService = new PDFExportService();
    private final RecommendationEngine recEngine = new RecommendationEngine();
    private static final ObjectMapper MAPPER     = new ObjectMapper();

    // Recommended quiz ids — populated by background thread, consumed in renderCards
    private final Set<Long> recommendedIds = new java.util.concurrent.CopyOnWriteArraySet<>();

    // Prediction labels — one per quiz card, filled by background OLS thread
    private final Map<Long, Label> predictionLabels = new HashMap<>();

    // Last AI feedback text — captured so the email handler can include it
    private String lastAiFeedback = "";
    // Per-question result log for PDF export
    private final List<Map<String, Object>> questionResults = new ArrayList<>();

    private long resolvedUserId    = -1L;
    private long resolvedSubjectId = -1L;

    // All published quizzes + selected
    private List<Quiz>   allPublished = new ArrayList<>();
    private Quiz         selectedQuiz = null;

    // Caches (avoids N+1)
    private Map<Long, Double> ratingsCache = new HashMap<>();
    private Map<Long, int[]>  statsCache   = new HashMap<>();

    // Quiz session state
    private Quiz           currentQuiz;
    private List<JsonNode> questions = new ArrayList<>();
    private int            qIndex;
    private int            correctCount;
    private ToggleGroup    answerGroup;
    private QuizAttempt    attempt;
    private LocalDateTime  startTime;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            resolvedUserId    = service.getFirstUserId();
            resolvedSubjectId = service.getFirstSubjectId();
        } catch (SQLException e) {
            lblListStatus.setText("Erreur DB au démarrage : " + e.getMessage());
        }

        sliderRating.valueProperty().addListener((obs, old, nw) ->
                lblRatingVal.setText(String.valueOf(nw.intValue())));

        if (cmbFilterDifficulty != null) {
            cmbFilterDifficulty.setItems(FXCollections.observableArrayList("Toutes", "EASY", "MEDIUM", "HARD"));
            cmbFilterDifficulty.setValue("Toutes");
            cmbFilterDifficulty.valueProperty().addListener((o, a, b) -> renderCards());
        }
        if (cmbSort != null) {
            cmbSort.setItems(FXCollections.observableArrayList("Plus récent", "Mieux noté", "Plus tenté"));
            cmbSort.setValue("Plus récent");
            cmbSort.valueProperty().addListener((o, a, b) -> renderCards());
        }

        txtSearch.textProperty().addListener((obs, old, nw) -> renderCards());

        loadPublishedQuizzes();
        showPanel(panelList);
    }

    // ── Data ────────────────────────────────────────────────────────────

    private void loadPublishedQuizzes() {
        try {
            allPublished = service.getPublishedQuizzes();
            try { ratingsCache = service.getAllAverageRatings(); } catch (Exception ignored) {}
            try { statsCache   = service.getQuizStats();         } catch (Exception ignored) {}
            renderCards();
        } catch (SQLException e) {
            lblListStatus.setText("Erreur : " + e.getMessage());
        }
        // Recommendation engine — runs once after the card list is built
        if (resolvedUserId > 0) {
            long capturedUserId = resolvedUserId;
            Thread rec = new Thread(() -> {
                List<Quiz> recommended = recEngine.getRecommendedQuizzes(capturedUserId, 3);
                Platform.runLater(() -> applyRecommendationBadges(recommended));
            }, "rec-engine");
            rec.setDaemon(true);
            rec.start();
        }
    }

    private void renderCards() {
        cardsContainer.getChildren().clear();
        predictionLabels.clear();
        selectedQuiz = null;

        String text   = txtSearch.getText() == null ? "" : txtSearch.getText().toLowerCase().trim();
        String diffF  = (cmbFilterDifficulty != null) ? cmbFilterDifficulty.getValue() : "Toutes";
        String sort   = (cmbSort != null) ? cmbSort.getValue() : "Plus récent";

        // Filtre combiné texte + difficulté (AND)
        List<Quiz> visible = new ArrayList<>();
        for (Quiz q : allPublished) {
            boolean okText = text.isBlank()
                    || q.getTitle().toLowerCase().contains(text)
                    || (q.getDifficulty() != null && q.getDifficulty().name().toLowerCase().contains(text));
            boolean okDiff = diffF == null || "Toutes".equals(diffF)
                    || (q.getDifficulty() != null && q.getDifficulty().name().equalsIgnoreCase(diffF));
            if (okText && okDiff) visible.add(q);
        }

        // Tri en mémoire
        Comparator<Quiz> cmp;
        switch (sort == null ? "" : sort) {
            case "Mieux noté":
                cmp = Comparator.comparingDouble((Quiz q) -> {
                    Double r = ratingsCache.get(q.getId());
                    return r == null ? -1.0 : r;
                }).reversed();
                break;
            case "Plus tenté":
                cmp = Comparator.comparingInt((Quiz q) -> {
                    int[] s = statsCache.get(q.getId());
                    return s == null ? 0 : s[0];
                }).reversed();
                break;
            case "Plus récent":
            default:
                cmp = Comparator.comparingLong(Quiz::getId).reversed();
                break;
        }
        visible.sort(cmp);

        if (visible.isEmpty()) {
            Label empty = new Label("Aucun quiz disponible.");
            empty.setStyle("-fx-text-fill:#B0BBC8;-fx-font-size:13;");
            cardsContainer.getChildren().add(empty);
            return;
        }

        for (Quiz q : visible) {
            cardsContainer.getChildren().add(buildQuizCard(q));
        }

        // Score prediction — one thread for all visible quizzes
        if (resolvedUserId > 0 && !visible.isEmpty()) {
            long capturedUserId = resolvedUserId;
            List<Quiz> capturedVisible = new ArrayList<>(visible);
            Thread predThread = new Thread(() -> {
                ScorePredictionService svc = new ScorePredictionService();
                Map<Long, Integer> predictions = new HashMap<>();
                for (Quiz q : capturedVisible) {
                    try {
                        svc.predictScore(capturedUserId, q)
                           .ifPresent(pred -> predictions.put(q.getId(), pred));
                    } catch (Exception ignored) {}
                }
                if (!predictions.isEmpty()) {
                    Platform.runLater(() -> {
                        for (Map.Entry<Long, Integer> e : predictions.entrySet()) {
                            Label lbl = predictionLabels.get(e.getKey());
                            if (lbl != null) {
                                lbl.setText("📊 ~" + e.getValue() + "%");
                                lbl.setVisible(true);
                                lbl.setManaged(true);
                            }
                        }
                    });
                }
            }, "score-prediction");
            predThread.setDaemon(true);
            predThread.start();
        }
    }

    private VBox buildQuizCard(Quiz q) {
        int nbQ = q.getQuestionCount();

        String ratingText = "-";
        Double avg = ratingsCache.get(q.getId());
        if (avg != null) ratingText = String.format("%.1f/5", avg);

        int[] stats = statsCache.get(q.getId());
        int nbAtt = (stats != null) ? stats[0] : 0;

        String diffColor  = difficultyColor(q.getDifficulty());
        String diffBg     = difficultyBg(q.getDifficulty());

        Label lblTitle = new Label(q.getTitle());
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblTitle.setStyle("-fx-text-fill:#4A5673;");
        lblTitle.setWrapText(true);
        HBox.setHgrow(lblTitle, Priority.ALWAYS);

        Label badge = new Label(q.getDifficulty() != null ? q.getDifficulty().name() : "");
        badge.setStyle("-fx-background-color:" + diffBg + ";-fx-text-fill:" + diffColor
                + ";-fx-font-weight:bold;-fx-font-size:11;-fx-padding:3 10;"
                + "-fx-background-radius:9999;-fx-border-radius:9999;");

        HBox titleRow = new HBox(8, lblTitle, badge);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label lblNbQ    = new Label(nbQ + " question(s)");
        lblNbQ.setStyle("-fx-text-fill:#6B7C94;-fx-font-size:12;");
        Label lblRating = new Label("Note : " + ratingText);
        lblRating.setStyle("-fx-text-fill:#6B7C94;-fx-font-size:12;");
        Label lblAtt    = new Label(nbAtt + " tentative(s)");
        lblAtt.setStyle("-fx-text-fill:#8B97A8;-fx-font-size:12;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox metaRow = new HBox(16, lblNbQ, lblAtt, spacer, lblRating);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label lblPrediction = new Label("");
        lblPrediction.setStyle("-fx-font-size:11;-fx-font-style:italic;-fx-text-fill:#9E9E9E;");
        lblPrediction.setVisible(false);
        lblPrediction.setManaged(false);
        predictionLabels.put(q.getId(), lblPrediction);

        VBox card = new VBox(6, titleRow, metaRow, lblPrediction);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setUserData(q.getId());   // used by recommendation badge injection
        card.setStyle("-fx-background-color:#FFFFFF;-fx-border-color:#E5EAF2;"
                + "-fx-border-radius:10;-fx-background-radius:10;-fx-cursor:hand;");

        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color:#F0F4F8;-fx-border-color:#5AAEEF;"
                + "-fx-border-radius:10;-fx-background-radius:10;-fx-cursor:hand;"));
        card.setOnMouseExited(e -> {
            boolean sel = (selectedQuiz != null && selectedQuiz.getId() == q.getId());
            applyCardStyle(card, sel);
        });

        card.setOnMouseClicked(e -> {
            selectedQuiz = q;
            for (javafx.scene.Node node : cardsContainer.getChildren()) {
                if (node instanceof VBox) {
                    node.setStyle("-fx-background-color:#FFFFFF;-fx-border-color:#E5EAF2;"
                            + "-fx-border-radius:10;-fx-background-radius:10;-fx-cursor:hand;");
                }
            }
            applyCardStyle(card, true);
        });

        return card;
    }

    private void applyRecommendationBadges(List<Quiz> recommended) {
        if (recommended.isEmpty()) return;

        Set<Long> recIds = new java.util.HashSet<>();
        for (Quiz q : recommended) recIds.add(q.getId());

        // Insert section header at top of cardsContainer
        Label sectionLbl = new Label("📌 Recommandés pour toi");
        sectionLbl.setStyle("-fx-font-size:11;-fx-font-weight:bold;"
                + "-fx-text-fill:#6A1B9A;-fx-letter-spacing:0.08em;"
                + "-fx-padding:4 0 2 0;");
        sectionLbl.setId("rec-section-header");
        cardsContainer.getChildren().add(0, sectionLbl);

        // Walk cards and inject badge into matching ones
        for (javafx.scene.Node node : cardsContainer.getChildren()) {
            if (!(node instanceof VBox card)) continue;
            Object data = card.getUserData();
            if (!(data instanceof Long quizId)) continue;
            if (!recIds.contains(quizId)) continue;

            // Build purple "✨ Recommandé" badge
            Label badge = new Label("✨ Recommandé");
            badge.setStyle("-fx-background-color:#F3E5F5;-fx-text-fill:#6A1B9A;"
                    + "-fx-font-weight:bold;-fx-font-size:11;-fx-padding:3 10;"
                    + "-fx-background-radius:9999;-fx-border-radius:9999;");

            // Inject after titleRow (index 0 = titleRow, 1 = metaRow)
            if (!card.getChildren().isEmpty()) {
                // titleRow is the first child; add badge into it
                javafx.scene.Node firstRow = card.getChildren().get(0);
                if (firstRow instanceof HBox hbox) {
                    hbox.getChildren().add(badge);
                }
            }

            // Highlight the card border with a subtle purple tint
            card.setStyle(card.getStyle()
                    .replace("-fx-border-color:#E5EAF2;", "-fx-border-color:#CE93D8;")
                    + "-fx-border-width:1.5;");
        }
    }

    private void applyCardStyle(VBox card, boolean selected) {
        if (selected) {
            card.setStyle("-fx-background-color:#E0F2FE;-fx-border-color:#5AAEEF;-fx-border-width:2;"
                    + "-fx-border-radius:10;-fx-background-radius:10;-fx-cursor:hand;");
        } else {
            card.setStyle("-fx-background-color:#FFFFFF;-fx-border-color:#E5EAF2;"
                    + "-fx-border-radius:10;-fx-background-radius:10;-fx-cursor:hand;");
        }
    }

    private String difficultyColor(Difficulty d) {
        if (d == null) return "#6B7C94";
        return switch (d) {
            case EASY   -> "#4BB543";
            case HARD   -> "#E03E3E";
            case MEDIUM -> "#E09010";
        };
    }

    private String difficultyBg(Difficulty d) {
        if (d == null) return "#F0F4F8";
        return switch (d) {
            case EASY   -> "#E8F8E6";
            case HARD   -> "#FDEAEA";
            case MEDIUM -> "#FFF4E0";
        };
    }

    // ── List panel actions ──────────────────────────────────────────────

    @FXML private void handleStartQuiz() {
        if (selectedQuiz == null) { lblListStatus.setText("Cliquer sur un quiz pour le sélectionner."); return; }
        startQuiz(selectedQuiz);
    }

    private void startQuiz(Quiz q) {
        String json = q.getQuestions();
        if (json == null || json.isBlank()) {
            lblListStatus.setText("Ce quiz n'a pas de questions."); return;
        }
        try {
            JsonNode arr = MAPPER.readTree(json);
            if (!arr.isArray() || arr.size() == 0) {
                lblListStatus.setText("Ce quiz n'a pas de questions."); return;
            }
            questions.clear();
            arr.forEach(questions::add);
        } catch (Exception e) {
            lblListStatus.setText("Questions JSON invalides."); return;
        }

        currentQuiz  = q;
        qIndex       = 0;
        correctCount = 0;
        startTime    = LocalDateTime.now();
        questionResults.clear();

        if (resolvedUserId > 0) {
            attempt = new QuizAttempt(resolvedUserId, q.getId(), startTime, questions.size());
            try { service.addAttempt(attempt); } catch (SQLException ignored) {}
        }

        showPanel(panelQuiz);
        displayQuestion();
    }

    @FXML private void handleRate() {
        if (selectedQuiz == null) { lblListStatus.setText("Cliquer sur un quiz pour le sélectionner."); return; }
        if (resolvedUserId <= 0) {
            lblListStatus.setText("Aucun utilisateur trouvé en base."); return;
        }
        int stars = (int) sliderRating.getValue();
        try {
            service.addOrUpdateRating(new QuizRating(resolvedUserId, selectedQuiz.getId(), stars));
            lblListStatus.setText("Note " + stars + "/5 enregistrée.");
            loadPublishedQuizzes();
        } catch (SQLException e) {
            lblListStatus.setText("Erreur : " + e.getMessage());
        }
    }

    @FXML private void handleRefresh() {
        loadPublishedQuizzes();
        lblListStatus.setText("");
    }

    // ── Quiz panel actions ──────────────────────────────────────────────

    private void displayQuestion() {
        JsonNode q    = questions.get(qIndex);
        String text   = q.path("question").asText("(pas de texte)");
        JsonNode opts = q.path("options");

        lblQuizTitle.setText(currentQuiz.getTitle());
        lblProgress.setText("Question " + (qIndex + 1) + " / " + questions.size());
        lblDifficulty.setText("[" + (currentQuiz.getDifficulty() != null ? currentQuiz.getDifficulty().name() : "") + "]");
        lblQuestion.setText(text);

        vboxOptions.getChildren().clear();
        answerGroup = new ToggleGroup();
        if (opts.isArray()) {
            for (int i = 0; i < opts.size(); i++) {
                RadioButton rb = new RadioButton(opts.get(i).asText());
                rb.setToggleGroup(answerGroup);
                rb.setUserData(i);
                vboxOptions.getChildren().add(rb);
            }
        }

        setVisible(lblFeedback, false);
        setVisible(btnNext, false);
        btnValidate.setDisable(false);
        lblQuizStatus.setText("");
    }

    @FXML private void handleValidate() {
        Toggle selected = answerGroup.getSelectedToggle();
        if (selected == null) { lblQuizStatus.setText("Choisir une réponse."); return; }

        int chosen  = (int) selected.getUserData();
        JsonNode qNode  = questions.get(qIndex);
        int correct     = qNode.path("correct").asInt(-1);
        boolean ok      = chosen == correct;
        if (ok) correctCount++;

        JsonNode opts       = qNode.path("options");
        String userAnsText  = (opts.isArray() && chosen  >= 0 && chosen  < opts.size())
                ? opts.get(chosen).asText()  : String.valueOf(chosen);
        String corrAnsText  = (opts.isArray() && correct >= 0 && correct < opts.size())
                ? opts.get(correct).asText() : String.valueOf(correct);

        // Record for PDF export
        Map<String, Object> result = new HashMap<>();
        result.put("question",      qNode.path("question").asText(""));
        result.put("userAnswer",    userAnsText);
        result.put("correctAnswer", corrAnsText);
        result.put("isCorrect",     ok);
        questionResults.add(result);

        String feedbackText = ok ? "Correct !" : "Incorrect. Bonne réponse : " + corrAnsText;
        lblFeedback.setText(feedbackText);
        lblFeedback.getStyleClass().removeAll("status-success", "status-error");
        lblFeedback.getStyleClass().add(ok ? "status-success" : "status-error");
        setVisible(lblFeedback, true);

        vboxOptions.getChildren().forEach(n -> n.setDisable(true));
        btnValidate.setDisable(true);

        boolean last = (qIndex == questions.size() - 1);
        btnNext.setText(last ? "Voir le score" : "Question suivante");
        setVisible(btnNext, true);
    }

    @FXML private void handleNext() {
        qIndex++;
        if (qIndex >= questions.size()) finishQuiz();
        else displayQuestion();
    }

    @FXML private void handleAbort() {
        showPanel(panelList);
        loadPublishedQuizzes();
    }

    @FXML private void handleBackToList() {
        showPanel(panelList);
        loadPublishedQuizzes();
    }

    private void finishQuiz() {
        // Remove any leftover adaptive banner from a previous session
        panelScore.getChildren().removeIf(n -> n instanceof HBox hb
                && "-fx-background-color:#FFFDE7;-fx-border-color:#FFF176;-fx-border-width:1;-fx-border-radius:8;-fx-background-radius:8;"
                    .equals(hb.getStyle()));

        double pct     = questions.isEmpty() ? 0 : (correctCount * 100.0 / questions.size());
        long   seconds = java.time.Duration.between(startTime, LocalDateTime.now()).getSeconds();

        if (attempt != null) {
            attempt.setCompletedAt(LocalDateTime.now());
            attempt.setCorrectCount(correctCount);
            attempt.setTotalQuestions(questions.size());
            attempt.setScore(pct);
            attempt.setDurationSeconds((int) seconds);
            try { service.updateAttempt(attempt); } catch (SQLException ignored) {}
        }

        lblScore.setText(correctCount + " / " + questions.size());
        lblScoreDetail.setText(String.format("Score : %.0f%%   —   Durée : %ds", pct, seconds));

        // Reset email button state + last AI feedback
        lastAiFeedback = "";
        if (btnEmailResult != null) {
            btnEmailResult.setText("📧 Envoyer par email");
            btnEmailResult.setDisable(!email.isConfigured());
            if (!email.isConfigured()) {
                btnEmailResult.setTooltip(new Tooltip("Configurer EmailService.java avec un App Password Gmail"));
            } else {
                btnEmailResult.setTooltip(null);
            }
        }

        // Feedback IA en arrière-plan
        if (txtAiFeedback != null) {
            txtAiFeedback.setText("");
            requestAiFeedback(correctCount, questions.size(), currentQuiz.getTitle());
        }

        showPanel(panelScore);

        // Adaptive recommendation — async, does not block result display
        if (attempt != null) {
            checkAndShowAdaptive(resolvedUserId, currentQuiz.getId());
        }
    }

    private void requestAiFeedback(int score, int total, String title) {
        if (txtAiFeedback == null) return;
        Thread check = new Thread(() -> {
            boolean ok = ollama.isAvailable();
            if (!ok) {
                Platform.runLater(() -> {
                    txtAiFeedback.setVisible(false);
                    txtAiFeedback.setManaged(false);
                });
                return;
            }
            Platform.runLater(() -> {
                txtAiFeedback.setVisible(true);
                txtAiFeedback.setManaged(true);
                txtAiFeedback.setText("🤖 Analyse en cours…");
            });
            try {
                String fb = ollama.generateScoreFeedback(score, total, title);
                Platform.runLater(() -> {
                    txtAiFeedback.setText(fb);
                    lastAiFeedback = fb;
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    txtAiFeedback.setVisible(false);
                    txtAiFeedback.setManaged(false);
                });
            }
        }, "ollama-feedback");
        check.setDaemon(true);
        check.start();
    }

    // ── Adaptive recommendation ──────────────────────────────────────────

    private void checkAndShowAdaptive(long userId, long quizId) {
        Thread t = new Thread(() -> {
            java.util.Optional<AdaptiveRecommendation> opt = adaptive.analyze(userId, quizId);
            if (opt.isEmpty()) return;
            AdaptiveRecommendation rec = opt.get();
            Platform.runLater(() -> showAdaptiveBanner(rec));
        }, "adaptive-engine");
        t.setDaemon(true);
        t.start();
    }

    private void showAdaptiveBanner(AdaptiveRecommendation rec) {
        // Title
        Label lblTitle = new Label("💡 Recommandation IA");
        lblTitle.setStyle("-fx-font-weight:bold;-fx-text-fill:#F57F17;-fx-font-size:13;");

        // Concept + difficulty
        Label lblConcept = new Label(rec.concept() + "  —  " + rec.recommendedDifficulty().name());
        lblConcept.setStyle("-fx-text-fill:#4A5673;-fx-font-size:12;");

        // Explanation
        Label lblExpl = new Label(rec.explanation());
        lblExpl.setStyle("-fx-font-style:italic;-fx-text-fill:#6B7C94;-fx-font-size:11;");
        lblExpl.setWrapText(true);

        VBox textBox = new VBox(4, lblTitle, lblConcept, lblExpl);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        HBox banner = new HBox(12, textBox);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setPadding(new Insets(12, 16, 12, 16));
        banner.setStyle("-fx-background-color:#FFFDE7;"
                + "-fx-border-color:#FFF176;"
                + "-fx-border-width:1;"
                + "-fx-border-radius:8;"
                + "-fx-background-radius:8;");
        banner.setMaxWidth(Double.MAX_VALUE);

        // Quiz button — only if a matching quiz was found
        if (rec.matchingQuizId() != 0) {
            Button btnGo = new Button("→ Quiz recommandé");
            btnGo.setStyle("-fx-background-color:#FFA000;-fx-text-fill:white;"
                    + "-fx-font-weight:bold;-fx-background-radius:6;-fx-padding:6 12;");
            long targetId = rec.matchingQuizId();
            btnGo.setOnAction(e -> {
                try {
                    Quiz target = service.getQuizById(targetId);
                    if (target != null) {
                        selectedQuiz = target;
                        startQuiz(target);
                    }
                } catch (Exception ignored) {}
            });
            banner.getChildren().add(btnGo);
        }

        // Dismiss button
        Button btnClose = new Button("✕");
        btnClose.setStyle("-fx-background-color:transparent;-fx-text-fill:#9E9E9E;"
                + "-fx-font-size:13;-fx-cursor:hand;-fx-padding:2 6;");
        btnClose.setOnAction(e -> {
            if (banner.getParent() instanceof VBox parent) {
                parent.getChildren().remove(banner);
            }
        });
        banner.getChildren().add(btnClose);

        // Fade in
        banner.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(500), banner);
        fade.setFromValue(0);
        fade.setToValue(1);

        // Add at bottom of panelScore
        panelScore.getChildren().add(banner);
        fade.play();
    }

    // ── Email handler ────────────────────────────────────────────────────

    @FXML
    private void handleEmailResult() {
        if (currentQuiz == null) return;

        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Envoyer le résultat par email");
        dlg.setHeaderText(null);
        dlg.setContentText("Adresse email :");
        dlg.getEditor().setPromptText("vous@exemple.com");

        dlg.showAndWait().ifPresent(toEmail -> {
            if (toEmail.isBlank()) return;

            btnEmailResult.setDisable(true);
            btnEmailResult.setText("Envoi…");

            String studentName = "Étudiant";
            email.sendQuizResult(
                    toEmail.trim(),
                    studentName,
                    currentQuiz.getTitle(),
                    correctCount,
                    questions.size(),
                    lastAiFeedback,
                    () -> {
                        btnEmailResult.setText("✅ Envoyé !");
                        btnEmailResult.setDisable(false);
                    },
                    err -> {
                        btnEmailResult.setText("❌ " + err);
                        btnEmailResult.setDisable(false);
                    }
            );
        });
    }

    @FXML
    private void handleExportPdf() {
        if (currentQuiz == null || attempt == null) return;
        btnExportPdf.setDisable(true);
        btnExportPdf.setText("Export…");
        Thread t = new Thread(() -> {
            try {
                byte[] pdf = pdfService.exportQuizResult(
                        currentQuiz, attempt,
                        new ArrayList<>(questionResults),
                        lastAiFeedback);
                String fileName = "quiz_" + currentQuiz.getTitle()
                        .replaceAll("[^\\w\\- ]", "_") + "_" + attempt.getId() + ".pdf";
                pdfService.saveToFile(pdf, fileName);
                Platform.runLater(() -> {
                    btnExportPdf.setText("📄 Exporter PDF");
                    btnExportPdf.setDisable(false);
                    Alert ok = new Alert(Alert.AlertType.INFORMATION,
                            "PDF sauvegardé dans ~/Documents/" + fileName, ButtonType.OK);
                    ok.setHeaderText(null);
                    ok.showAndWait();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnExportPdf.setText("📄 Exporter PDF");
                    btnExportPdf.setDisable(false);
                    new Alert(Alert.AlertType.ERROR,
                            "Erreur export PDF : " + e.getMessage(), ButtonType.OK).showAndWait();
                });
            }
        }, "pdf-export-quiz");
        t.setDaemon(true);
        t.start();
    }

    // ── Panel switching ─────────────────────────────────────────────────

    private void showPanel(VBox panel) {
        for (VBox p : new VBox[]{panelList, panelQuiz, panelScore}) {
            boolean show = (p == panel);
            p.setVisible(show);
            p.setManaged(show);
        }
    }

    private void setVisible(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  CRUD étudiant — Création / Modification / Suppression
    // ═══════════════════════════════════════════════════════════════════

    @FXML private void handleCreateMyQuiz() {
        if (resolvedUserId <= 0 || resolvedSubjectId <= 0) {
            lblListStatus.setText("Aucun utilisateur/sujet en base.");
            return;
        }
        Quiz draft = new Quiz();
        draft.setOwnerId(resolvedUserId);
        draft.setSubjectId(resolvedSubjectId);
        draft.setDifficulty(Difficulty.MEDIUM);
        draft.setPublished(true);
        if (showQuizEditor(draft, false)) {
            try {
                service.addQuiz(draft);
                lblListStatus.setText("Quiz créé.");
                loadPublishedQuizzes();
            } catch (SQLException e) {
                lblListStatus.setText("Erreur : " + e.getMessage());
            }
        }
    }

    @FXML private void handleEditMyQuiz() {
        if (selectedQuiz == null) { lblListStatus.setText("Sélectionner un quiz à modifier."); return; }
        if (selectedQuiz.getOwnerId() != resolvedUserId) {
            lblListStatus.setText("Vous ne pouvez modifier que vos propres quiz.");
            return;
        }
        Quiz copy = new Quiz();
        copy.setId(selectedQuiz.getId());
        copy.setOwnerId(selectedQuiz.getOwnerId());
        copy.setSubjectId(selectedQuiz.getSubjectId());
        copy.setTitle(selectedQuiz.getTitle());
        copy.setDifficulty(selectedQuiz.getDifficulty());
        copy.setPublished(selectedQuiz.isPublished());
        copy.setQuestions(selectedQuiz.getQuestions());
        if (showQuizEditor(copy, true)) {
            try {
                service.updateQuiz(copy);
                lblListStatus.setText("Quiz modifié.");
                loadPublishedQuizzes();
            } catch (SQLException e) {
                lblListStatus.setText("Erreur : " + e.getMessage());
            }
        }
    }

    @FXML private void handleDeleteMyQuiz() {
        if (selectedQuiz == null) { lblListStatus.setText("Sélectionner un quiz à supprimer."); return; }
        if (selectedQuiz.getOwnerId() != resolvedUserId) {
            lblListStatus.setText("Vous ne pouvez supprimer que vos propres quiz.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer \"" + selectedQuiz.getTitle() + "\" ?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    service.deleteQuiz(selectedQuiz.getId());
                    lblListStatus.setText("Quiz supprimé.");
                    loadPublishedQuizzes();
                } catch (SQLException e) {
                    lblListStatus.setText("Erreur : " + e.getMessage());
                }
            }
        });
    }

    /**
     * Ouvre la fenêtre d'édition de quiz (create OU edit).
     * Retourne true si l'utilisateur a validé, false si annulé.
     * Modifie directement {@code target} (titre, difficulté, published, questions JSON).
     */
    private boolean showQuizEditor(Quiz target, boolean editMode) {
        // ── State pour les questions
        final List<ObjectNode> qNodes = new ArrayList<>();
        final ObservableList<String> qPreviews = FXCollections.observableArrayList();
        if (editMode && target.getQuestions() != null && !target.getQuestions().isBlank()) {
            try {
                JsonNode arr = MAPPER.readTree(target.getQuestions());
                if (arr.isArray()) {
                    for (JsonNode it : arr) {
                        qNodes.add((ObjectNode) it.deepCopy());
                        int correct = it.path("correct").asInt(0);
                        qPreviews.add(qNodes.size() + ". " + it.path("question").asText("") +
                                "  ✓ " + "ABCD".charAt(Math.min(correct, 3)));
                    }
                }
            } catch (Exception ignored) {}
        }

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(editMode ? "Modifier mon quiz" : "Créer mon quiz");
        dlg.setHeaderText(null);
        ButtonType saveBt   = new ButtonType(editMode ? "Enregistrer" : "Créer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBt = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dlg.getDialogPane().getButtonTypes().addAll(saveBt, cancelBt);
        try {
            dlg.getDialogPane().getScene().getStylesheets()
                    .add(getClass().getResource("/css/studysprint.css").toExternalForm());
        } catch (Exception ignored) {}

        // ── Form widgets
        TextField txtTitle = new TextField(target.getTitle() == null ? "" : target.getTitle());
        txtTitle.setPromptText("Titre du quiz");

        ComboBox<String> cmbDiff = new ComboBox<>(FXCollections.observableArrayList("EASY", "MEDIUM", "HARD"));
        cmbDiff.setValue(target.getDifficulty() != null ? target.getDifficulty().name() : "MEDIUM");
        cmbDiff.setMaxWidth(Double.MAX_VALUE);

        CheckBox chkPub = new CheckBox("Publié");
        chkPub.setSelected(target.isPublished());

        Button btnAi = new Button("✨  Générer avec IA");
        btnAi.getStyleClass().add("btn-primary");
        Label lblAiStatus = new Label("");
        lblAiStatus.setStyle("-fx-text-fill:#6B7C94;-fx-font-size:11;");

        // Vérifie disponibilité Ollama en arrière-plan
        Thread tCheck = new Thread(() -> {
            boolean ok = ollama.isAvailable();
            Platform.runLater(() -> {
                if (!ok) {
                    btnAi.setDisable(true);
                    btnAi.setTooltip(new Tooltip("Ollama non disponible"));
                    lblAiStatus.setText("IA indisponible");
                }
            });
        }, "ollama-check-front");
        tCheck.setDaemon(true);
        tCheck.start();

        TextField txtQ  = new TextField();   txtQ.setPromptText("Texte de la question");
        TextField txtA  = new TextField();   txtA.setPromptText("Option A");
        TextField txtB  = new TextField();   txtB.setPromptText("Option B");
        TextField txtC  = new TextField();   txtC.setPromptText("Option C");
        TextField txtD  = new TextField();   txtD.setPromptText("Option D");
        ComboBox<String> cmbCorrect = new ComboBox<>(FXCollections.observableArrayList("A", "B", "C", "D"));
        cmbCorrect.setPromptText("Bonne réponse");
        cmbCorrect.setMaxWidth(Double.MAX_VALUE);

        ListView<String> listView = new ListView<>(qPreviews);
        listView.setPrefHeight(120);

        Label lblCount = new Label(qNodes.size() + " question(s)");
        lblCount.setStyle("-fx-text-fill:#6B7C94;-fx-font-size:11;");

        Button btnAddQ = new Button("+ Ajouter");
        btnAddQ.getStyleClass().add("btn-primary");
        Button btnDelQ = new Button("Retirer dernière");
        btnDelQ.getStyleClass().add("btn-danger");

        btnAddQ.setOnAction(e -> {
            String q = txtQ.getText().trim();
            String a = txtA.getText().trim(), b = txtB.getText().trim();
            String c = txtC.getText().trim(), d = txtD.getText().trim();
            String sel = cmbCorrect.getValue();
            if (q.isEmpty() || a.isEmpty() || b.isEmpty() || c.isEmpty() || d.isEmpty() || sel == null) return;
            int correctIdx = "ABCD".indexOf(sel.charAt(0));
            ObjectNode node = MAPPER.createObjectNode();
            node.put("question", q);
            ArrayNode opts = node.putArray("options");
            opts.add(a); opts.add(b); opts.add(c); opts.add(d);
            node.put("correct", correctIdx);
            qNodes.add(node);
            qPreviews.add(qNodes.size() + ". " + q + "  ✓ " + sel.charAt(0));
            lblCount.setText(qNodes.size() + " question(s)");
            txtQ.clear(); txtA.clear(); txtB.clear(); txtC.clear(); txtD.clear();
            cmbCorrect.setValue(null);
        });
        btnDelQ.setOnAction(e -> {
            if (qNodes.isEmpty()) return;
            qNodes.remove(qNodes.size() - 1);
            qPreviews.remove(qPreviews.size() - 1);
            lblCount.setText(qNodes.size() + " question(s)");
        });

        btnAi.setOnAction(e -> {
            String subject = txtTitle.getText().trim();
            if (subject.isBlank()) {
                lblAiStatus.setText("Renseigne le titre comme sujet.");
                return;
            }
            String diff = cmbDiff.getValue() == null ? "MEDIUM" : cmbDiff.getValue();
            btnAi.setDisable(true);
            String prevTxt = btnAi.getText();
            btnAi.setText("⏳ Génération…");
            lblAiStatus.setText("Génération IA en cours…");

            Thread t = new Thread(() -> {
                try {
                    String json = ollama.generateQuizQuestions(subject, diff, 5);
                    Platform.runLater(() -> {
                        try {
                            JsonNode arr = MAPPER.readTree(json);
                            int added = 0;
                            if (arr.isArray()) {
                                for (JsonNode item : arr) {
                                    String qText  = item.path("question").asText("").trim();
                                    JsonNode opts = item.path("options");
                                    String answer = item.path("answer").asText("").trim();
                                    if (qText.isBlank() || !opts.isArray() || opts.size() < 4) continue;
                                    int correctIdx = -1;
                                    for (int i = 0; i < 4; i++) {
                                        if (opts.get(i).asText("").equalsIgnoreCase(answer)) { correctIdx = i; break; }
                                    }
                                    if (correctIdx < 0) correctIdx = 0;
                                    ObjectNode node = MAPPER.createObjectNode();
                                    node.put("question", qText);
                                    ArrayNode oArr = node.putArray("options");
                                    for (int i = 0; i < 4; i++) oArr.add(opts.get(i).asText(""));
                                    node.put("correct", correctIdx);
                                    qNodes.add(node);
                                    String letter = String.valueOf("ABCD".charAt(correctIdx));
                                    qPreviews.add(qNodes.size() + ". " + qText + "  ✓ " + letter + "  [IA]");
                                    added++;
                                }
                            }
                            lblCount.setText(qNodes.size() + " question(s)");
                            lblAiStatus.setText(added + " question(s) générée(s).");
                        } catch (Exception ex) {
                            lblAiStatus.setText("JSON IA invalide.");
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> lblAiStatus.setText("Erreur IA : " + ex.getMessage()));
                } finally {
                    Platform.runLater(() -> {
                        btnAi.setDisable(false);
                        btnAi.setText(prevTxt);
                    });
                }
            }, "ollama-front-quiz-gen");
            t.setDaemon(true);
            t.start();
        });

        // ── Layout
        VBox metaBox = new VBox(8,
                buildLabel("TITRE *"), txtTitle,
                buildLabel("DIFFICULTÉ"), cmbDiff,
                chkPub,
                new HBox(8, btnAi, lblAiStatus));
        metaBox.getStyleClass().add("card");

        HBox ab = new HBox(6, txtA, txtB);
        HBox cd = new HBox(6, txtC, txtD);
        HBox.setHgrow(txtA, Priority.ALWAYS); HBox.setHgrow(txtB, Priority.ALWAYS);
        HBox.setHgrow(txtC, Priority.ALWAYS); HBox.setHgrow(txtD, Priority.ALWAYS);

        VBox qBuilder = new VBox(8,
                buildLabel("AJOUTER UNE QUESTION"),
                txtQ, ab, cd, cmbCorrect,
                new HBox(8, btnAddQ, btnDelQ),
                lblCount, listView);
        qBuilder.getStyleClass().add("card");

        VBox content = new VBox(14, metaBox, qBuilder);
        content.setPadding(new Insets(8));
        content.setPrefWidth(560);

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setPrefHeight(540);
        sp.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;");

        dlg.getDialogPane().setContent(sp);

        // ── Validation
        var saveButton = (Button) dlg.getDialogPane().lookupButton(saveBt);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            if (txtTitle.getText().isBlank()) {
                lblAiStatus.setText("Le titre est obligatoire.");
                ev.consume();
                return;
            }
            if (qNodes.isEmpty()) {
                lblAiStatus.setText("Ajouter au moins 1 question.");
                ev.consume();
            }
        });

        var result = dlg.showAndWait();
        if (result.isPresent() && result.get() == saveBt) {
            target.setTitle(txtTitle.getText().trim());
            target.setDifficulty(Difficulty.fromString(cmbDiff.getValue()));
            target.setPublished(chkPub.isSelected());
            try {
                ArrayNode arr = MAPPER.createArrayNode();
                for (ObjectNode n : qNodes) arr.add(n);
                target.setQuestions(MAPPER.writeValueAsString(arr));
            } catch (Exception ex) {
                target.setQuestions("[]");
            }
            return true;
        }
        return false;
    }

    private Label buildLabel(String txt) {
        Label l = new Label(txt);
        l.setStyle("-fx-font-size:11;-fx-font-weight:bold;-fx-text-fill:#6B7C94;-fx-letter-spacing:0.06em;");
        return l;
    }
}
