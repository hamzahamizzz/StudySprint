package com.example.studysprint.modules.quizz.controllers;

import com.example.studysprint.modules.quizz.models.Difficulty;
import com.example.studysprint.modules.quizz.models.Quiz;
import com.example.studysprint.modules.quizz.models.QuizAttempt;
import com.example.studysprint.modules.quizz.models.QuizRating;
import com.example.studysprint.modules.quizz.services.QuizService;
import com.example.studysprint.utils.OllamaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
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

    private final QuizService    service     = new QuizService();
    private final OllamaService  ollama      = new OllamaService();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private long resolvedUserId = -1L;

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
            resolvedUserId = service.getFirstUserId();
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
    }

    private void renderCards() {
        cardsContainer.getChildren().clear();
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

        VBox card = new VBox(6, titleRow, metaRow);
        card.setPadding(new Insets(12, 16, 12, 16));
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
        Quiz q = selectedQuiz;
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
        int correct = questions.get(qIndex).path("correct").asInt(-1);
        boolean ok  = chosen == correct;
        if (ok) correctCount++;

        String feedbackText;
        if (ok) {
            feedbackText = "Correct !";
        } else {
            JsonNode opts = questions.get(qIndex).path("options");
            String correctText = (opts.isArray() && correct >= 0 && correct < opts.size())
                    ? opts.get(correct).asText() : String.valueOf(correct);
            feedbackText = "Incorrect. Bonne réponse : " + correctText;
        }
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

        // Feedback IA en arrière-plan
        if (txtAiFeedback != null) {
            txtAiFeedback.setText("");
            requestAiFeedback(correctCount, questions.size(), currentQuiz.getTitle());
        }

        showPanel(panelScore);
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
                Platform.runLater(() -> txtAiFeedback.setText(fb));
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
}
