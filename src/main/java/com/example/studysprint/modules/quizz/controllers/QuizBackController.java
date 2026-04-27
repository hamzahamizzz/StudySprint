package com.example.studysprint.modules.quizz.controllers;

import com.example.studysprint.modules.quizz.models.Difficulty;
import com.example.studysprint.modules.quizz.models.Quiz;
import com.example.studysprint.modules.quizz.services.QuizService;
import com.example.studysprint.utils.OllamaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.application.Platform;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class QuizBackController implements Initializable {

    // ── Search + list ──────────────────────────────────────────────────
    @FXML private TextField txtSearch;
    @FXML private VBox      quizListContainer;

    // ── Quiz meta form ─────────────────────────────────────────────────
    @FXML private TextField        txtTitle;
    @FXML private ComboBox<String> cmbDifficulty;
    @FXML private CheckBox         chkPublished;

    // ── Question builder ───────────────────────────────────────────────
    @FXML private TextField        txtQuestion;
    @FXML private TextField        txtOpt0, txtOpt1, txtOpt2, txtOpt3;
    @FXML private ComboBox<String> cmbCorrect;
    @FXML private ListView<String> listQuestions;
    @FXML private Label            lblQCount;
    @FXML private Button           btnUpdateQuestion;
    @FXML private Label            lblEditingQuestion;

    private int selectedQuestionIndex = -1; // index dans questionNodes de la question en cours d'édition

    // ── IA ─────────────────────────────────────────────────────────────
    @FXML private Button btnGenerateAi;
    @FXML private Label  lblAiStatus;

    // ── Status ─────────────────────────────────────────────────────────
    @FXML private Label lblStatus;

    private final QuizService    service       = new QuizService();
    private final OllamaService  ollama        = new OllamaService();
    private static final ObjectMapper MAPPER   = new ObjectMapper();

    private long resolvedOwnerId   = 1L;
    private long resolvedSubjectId = 1L;

    private final List<ObjectNode>     questionNodes    = new ArrayList<>();
    private final ObservableList<String> questionPreviews = FXCollections.observableArrayList();

    private List<Quiz>          allQuizzes = new ArrayList<>();
    private Quiz                selected   = null;
    private Map<Long, int[]>    statsCache = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            long uid = service.getFirstUserId();
            long sid = service.getFirstSubjectId();
            if (uid <= 0 || sid <= 0) {
                showStatus("Aucun utilisateur ou sujet trouvé en base. Créez-en un d'abord.", true);
            } else {
                resolvedOwnerId   = uid;
                resolvedSubjectId = sid;
            }
        } catch (SQLException e) {
            showStatus("Erreur DB au démarrage : " + e.getMessage(), true);
        }

        cmbDifficulty.setItems(FXCollections.observableArrayList("EASY", "MEDIUM", "HARD"));
        cmbCorrect.setItems(FXCollections.observableArrayList("A", "B", "C", "D"));
        listQuestions.setItems(questionPreviews);

        // Clic sur une question dans la liste → charge dans le formulaire pour édition
        listQuestions.getSelectionModel().selectedIndexProperty().addListener((obs, old, nw) -> {
            int idx = nw.intValue();
            if (idx >= 0 && idx < questionNodes.size()) {
                loadQuestionIntoForm(idx);
            }
        });

        if (btnUpdateQuestion != null) btnUpdateQuestion.setDisable(true);
        if (lblEditingQuestion != null) { lblEditingQuestion.setText(""); lblEditingQuestion.setVisible(false); }

        for (TextField opt : new TextField[]{txtOpt0, txtOpt1, txtOpt2, txtOpt3}) {
            opt.textProperty().addListener((obs, o, n) -> refreshCorrectCombo());
        }

        txtSearch.textProperty().addListener((obs, old, nw) -> renderList(nw));

        // Vérifie disponibilité IA en arrière-plan
        checkOllamaAvailability();

        loadQuizzes();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  IA — Disponibilité + génération
    // ═══════════════════════════════════════════════════════════════════

    private void checkOllamaAvailability() {
        if (btnGenerateAi == null) return;
        Thread t = new Thread(() -> {
            boolean ok = ollama.isAvailable();
            Platform.runLater(() -> {
                if (!ok) {
                    btnGenerateAi.setDisable(true);
                    btnGenerateAi.setTooltip(new Tooltip("Ollama non disponible (http://localhost:11434)"));
                    if (lblAiStatus != null) lblAiStatus.setText("IA indisponible");
                } else {
                    btnGenerateAi.setDisable(false);
                    btnGenerateAi.setTooltip(new Tooltip("Génère des questions via Mistral local"));
                    if (lblAiStatus != null) lblAiStatus.setText("");
                }
            });
        }, "ollama-check");
        t.setDaemon(true);
        t.start();
    }

    @FXML private void handleGenerateAi() {
        String title = txtTitle.getText() == null ? "" : txtTitle.getText().trim();
        if (title.isBlank()) {
            showStatus("Renseigne le titre du quiz pour donner un sujet à l'IA.", true);
            return;
        }
        String diff = cmbDifficulty.getValue();
        if (diff == null) diff = "MEDIUM";

        final String fSubject    = title;
        final String fDifficulty = diff;

        btnGenerateAi.setDisable(true);
        String previousLabel = btnGenerateAi.getText();
        btnGenerateAi.setText("⏳ Génération…");
        if (lblAiStatus != null) lblAiStatus.setText("Génération IA en cours…");

        Thread t = new Thread(() -> {
            try {
                String json = ollama.generateQuizQuestions(fSubject, fDifficulty, 5);
                Platform.runLater(() -> {
                    try {
                        JsonNode arr = MAPPER.readTree(json);
                        if (!arr.isArray() || arr.size() == 0) {
                            showStatus("L'IA n'a pas renvoyé de questions valides.", true);
                            return;
                        }
                        int added = 0;
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

                            questionNodes.add(node);
                            String letter = String.valueOf("ABCD".charAt(correctIdx));
                            questionPreviews.add(questionNodes.size() + ". " + qText + "  ✓ " + letter + "  [IA]");
                            added++;
                        }
                        lblQCount.setText(questionNodes.size() + " question(s)");
                        showStatus(added + " question(s) générée(s) par l'IA.", false);
                    } catch (Exception ex) {
                        showStatus("JSON IA invalide : " + ex.getMessage(), true);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showStatus("Erreur IA : " + e.getMessage(), true));
            } finally {
                Platform.runLater(() -> {
                    btnGenerateAi.setDisable(false);
                    btnGenerateAi.setText(previousLabel);
                    if (lblAiStatus != null) lblAiStatus.setText("");
                });
            }
        }, "ollama-quiz-gen");
        t.setDaemon(true);
        t.start();
    }

    // ── Question builder ───────────────────────────────────────────────

    @FXML private void handleAddQuestion() {
        String q   = txtQuestion.getText().trim();
        String a   = txtOpt0.getText().trim();
        String b   = txtOpt1.getText().trim();
        String c   = txtOpt2.getText().trim();
        String d   = txtOpt3.getText().trim();
        String sel = cmbCorrect.getValue();

        if (q.isEmpty() || a.isEmpty() || b.isEmpty() || c.isEmpty() || d.isEmpty()) {
            showStatus("Remplir la question et les 4 options.", true); return;
        }
        if (sel == null) { showStatus("Sélectionner la bonne réponse.", true); return; }

        int correctIndex = "ABCD".indexOf(sel.charAt(0));

        ObjectNode node = MAPPER.createObjectNode();
        node.put("question", q);
        ArrayNode options = node.putArray("options");
        options.add(a); options.add(b); options.add(c); options.add(d);
        node.put("correct", correctIndex);

        questionNodes.add(node);
        questionPreviews.add(questionNodes.size() + ". " + q + "  ✓ " + sel.charAt(0));
        lblQCount.setText(questionNodes.size() + " question(s)");

        txtQuestion.clear();
        txtOpt0.clear(); txtOpt1.clear(); txtOpt2.clear(); txtOpt3.clear();
        cmbCorrect.setValue(null);
        lblStatus.setText("");
    }

    @FXML private void handleRemoveLast() {
        if (questionNodes.isEmpty()) return;
        questionNodes.remove(questionNodes.size() - 1);
        questionPreviews.remove(questionPreviews.size() - 1);
        lblQCount.setText(questionNodes.size() + " question(s)");
        cancelEditQuestion();
    }

    /** Charge la question à l'index donné dans le formulaire pour édition. */
    private void loadQuestionIntoForm(int idx) {
        ObjectNode node = questionNodes.get(idx);
        selectedQuestionIndex = idx;

        txtQuestion.setText(node.path("question").asText(""));
        JsonNode opts = node.path("options");
        txtOpt0.setText(opts.size() > 0 ? opts.get(0).asText("") : "");
        txtOpt1.setText(opts.size() > 1 ? opts.get(1).asText("") : "");
        txtOpt2.setText(opts.size() > 2 ? opts.get(2).asText("") : "");
        txtOpt3.setText(opts.size() > 3 ? opts.get(3).asText("") : "");

        int correct = node.path("correct").asInt(0);
        cmbCorrect.setValue(String.valueOf("ABCD".charAt(Math.min(correct, 3))));

        if (btnUpdateQuestion != null) btnUpdateQuestion.setDisable(false);
        if (lblEditingQuestion != null) {
            lblEditingQuestion.setText("Édition question " + (idx + 1));
            lblEditingQuestion.setVisible(true);
        }
        showStatus("Question " + (idx + 1) + " chargée — modifie puis clique 'Modifier question'.", false);
    }

    /** Applique les modifications sur la question sélectionnée. */
    @FXML private void handleUpdateQuestion() {
        if (selectedQuestionIndex < 0 || selectedQuestionIndex >= questionNodes.size()) {
            showStatus("Aucune question sélectionnée.", true); return;
        }
        String q   = txtQuestion.getText().trim();
        String a   = txtOpt0.getText().trim();
        String b   = txtOpt1.getText().trim();
        String c   = txtOpt2.getText().trim();
        String d   = txtOpt3.getText().trim();
        String sel = cmbCorrect.getValue();

        if (q.isEmpty() || a.isEmpty() || b.isEmpty() || c.isEmpty() || d.isEmpty()) {
            showStatus("Remplir la question et les 4 options.", true); return;
        }
        if (sel == null) { showStatus("Sélectionner la bonne réponse.", true); return; }

        int correctIndex = "ABCD".indexOf(sel.charAt(0));

        ObjectNode node = MAPPER.createObjectNode();
        node.put("question", q);
        ArrayNode options = node.putArray("options");
        options.add(a); options.add(b); options.add(c); options.add(d);
        node.put("correct", correctIndex);

        questionNodes.set(selectedQuestionIndex, node);
        String letter = String.valueOf("ABCD".charAt(correctIndex));
        questionPreviews.set(selectedQuestionIndex,
                (selectedQuestionIndex + 1) + ". " + q + "  ✓ " + letter);

        showStatus("Question " + (selectedQuestionIndex + 1) + " modifiée.", false);
        cancelEditQuestion();
    }

    /** Annule le mode édition de question. */
    @FXML private void handleCancelEditQuestion() { cancelEditQuestion(); }

    private void cancelEditQuestion() {
        selectedQuestionIndex = -1;
        txtQuestion.clear();
        txtOpt0.clear(); txtOpt1.clear(); txtOpt2.clear(); txtOpt3.clear();
        cmbCorrect.setValue(null);
        listQuestions.getSelectionModel().clearSelection();
        if (btnUpdateQuestion != null) btnUpdateQuestion.setDisable(true);
        if (lblEditingQuestion != null) { lblEditingQuestion.setText(""); lblEditingQuestion.setVisible(false); }
    }

    // ── Data loading ───────────────────────────────────────────────────

    private void loadQuizzes() {
        try {
            allQuizzes = service.getAllQuizzes();
            try { statsCache = service.getQuizStats(); } catch (Exception ignored) { statsCache = new HashMap<>(); }
            renderList(txtSearch.getText());
        } catch (SQLException e) {
            showStatus("Erreur chargement : " + e.getMessage(), true);
        }
    }

    private void renderList(String filter) {
        quizListContainer.getChildren().clear();
        selected = null;

        String lower = (filter == null) ? "" : filter.toLowerCase();

        List<Quiz> visible = new ArrayList<>();
        for (Quiz q : allQuizzes) {
            if (lower.isBlank()
                    || q.getTitle().toLowerCase().contains(lower)
                    || (q.getDifficulty() != null && q.getDifficulty().name().toLowerCase().contains(lower))) {
                visible.add(q);
            }
        }

        if (visible.isEmpty()) {
            Label empty = new Label("Aucun quiz.");
            empty.setStyle("-fx-text-fill:#B0BBC8;-fx-font-size:13;");
            quizListContainer.getChildren().add(empty);
            return;
        }

        for (Quiz q : visible) {
            quizListContainer.getChildren().add(buildQuizRow(q));
        }
    }

    private VBox buildQuizRow(Quiz q) {
        int nbQ = q.getQuestionCount();
        int[] stats = statsCache.get(q.getId());
        int nbAtt = (stats != null) ? stats[0] : 0;
        int avgSc = (stats != null) ? stats[1] : 0;

        Label lblTitle = new Label(q.getTitle());
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblTitle.setStyle("-fx-text-fill:#4A5673;");
        lblTitle.setWrapText(true);
        HBox.setHgrow(lblTitle, Priority.ALWAYS);

        Label lblDiff = new Label(q.getDifficulty() != null ? q.getDifficulty().name() : "");
        lblDiff.setStyle("-fx-font-size:11;-fx-text-fill:#6B7C94;");

        Label lblNbQ = new Label(nbQ + " Q");
        lblNbQ.setStyle("-fx-font-size:11;-fx-text-fill:#6B7C94;");

        Button btnTogglePub = new Button(q.isPublished() ? "Dépublier" : "Publier");
        btnTogglePub.getStyleClass().add(q.isPublished() ? "btn-warning" : "btn-primary");
        btnTogglePub.setStyle("-fx-font-size:10;-fx-padding:3 8;");
        btnTogglePub.setOnAction(e -> handleTogglePublish(q, btnTogglePub));

        HBox topRow = new HBox(10, lblTitle, lblDiff, lblNbQ, btnTogglePub);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label lblStats = new Label(nbAtt + " tentative(s)  •  Score moyen : " + (nbAtt > 0 ? (avgSc + "%") : "—"));
        lblStats.setStyle("-fx-font-size:11;-fx-text-fill:#8B97A8;");

        VBox row = new VBox(4, topRow, lblStats);
        row.setPadding(new Insets(8, 12, 8, 12));
        row.setStyle("-fx-background-color:#FFFFFF;-fx-border-color:#E5EAF2;"
                + "-fx-border-radius:8;-fx-background-radius:8;-fx-cursor:hand;");

        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color:#F0F4F8;-fx-border-color:#5AAEEF;"
                + "-fx-border-radius:8;-fx-background-radius:8;-fx-cursor:hand;"));
        row.setOnMouseExited(e -> {
            boolean sel = (selected != null && selected.getId() == q.getId());
            applyRowStyle(row, sel);
        });
        row.setOnMouseClicked(e -> {
            // ignore clicks coming from the toggle button itself
            if (e.getTarget() instanceof Button) return;
            selected = q;
            for (javafx.scene.Node node : quizListContainer.getChildren()) {
                if (node instanceof VBox) {
                    ((VBox) node).setStyle("-fx-background-color:#FFFFFF;-fx-border-color:#E5EAF2;"
                            + "-fx-border-radius:8;-fx-background-radius:8;-fx-cursor:hand;");
                }
            }
            applyRowStyle(row, true);
            populateForm(q);
        });

        return row;
    }

    private void applyRowStyle(VBox row, boolean selected) {
        if (selected) {
            row.setStyle("-fx-background-color:#E0F2FE;-fx-border-color:#5AAEEF;-fx-border-width:2;"
                    + "-fx-border-radius:8;-fx-background-radius:8;-fx-cursor:hand;");
        } else {
            row.setStyle("-fx-background-color:#FFFFFF;-fx-border-color:#E5EAF2;"
                    + "-fx-border-radius:8;-fx-background-radius:8;-fx-cursor:hand;");
        }
    }

    private void handleTogglePublish(Quiz q, Button btn) {
        boolean newState = !q.isPublished();
        try {
            service.togglePublished(q.getId(), newState);
            q.setPublished(newState);
            btn.setText(newState ? "Dépublier" : "Publier");
            btn.getStyleClass().removeAll("btn-warning", "btn-primary");
            btn.getStyleClass().add(newState ? "btn-warning" : "btn-primary");
            showStatus("Quiz \"" + q.getTitle() + "\" " + (newState ? "publié" : "dépublié") + ".", false);
        } catch (SQLException e) {
            showStatus("Erreur : " + e.getMessage(), true);
        }
    }

    // ── Form helpers ───────────────────────────────────────────────────

    private void populateForm(Quiz q) {
        txtTitle.setText(q.getTitle());
        cmbDifficulty.setValue(q.getDifficulty() != null ? q.getDifficulty().name() : null);
        chkPublished.setSelected(q.isPublished());
        lblStatus.setText("");

        questionNodes.clear();
        questionPreviews.clear();
        String json = q.getQuestions();
        if (json != null && !json.isBlank()) {
            try {
                JsonNode arr = MAPPER.readTree(json);
                if (arr.isArray()) {
                    for (JsonNode item : arr) {
                        questionNodes.add((ObjectNode) item.deepCopy());
                        int correct = item.path("correct").asInt(0);
                        String letter = String.valueOf("ABCD".charAt(correct));
                        questionPreviews.add(questionNodes.size() + ". "
                                + item.path("question").asText() + "  ✓ " + letter);
                    }
                }
            } catch (Exception ignored) {}
        }
        lblQCount.setText(questionNodes.size() + " question(s)");
    }

    private boolean validateMeta() {
        if (txtTitle.getText().isBlank()) {
            showStatus("Le titre est obligatoire.", true); return false;
        }
        if (cmbDifficulty.getValue() == null) {
            showStatus("Choisir une difficulté.", true); return false;
        }
        if (questionNodes.isEmpty()) {
            showStatus("Ajouter au moins 1 question.", true); return false;
        }
        return true;
    }

    private Quiz buildFromForm() throws Exception {
        Quiz q = new Quiz();
        q.setOwnerId(resolvedOwnerId);
        q.setSubjectId(resolvedSubjectId);
        q.setTitle(txtTitle.getText().trim());
        q.setDifficulty(Difficulty.fromString(cmbDifficulty.getValue()));
        q.setPublished(chkPublished.isSelected());
        ArrayNode arr = MAPPER.createArrayNode();
        for (ObjectNode node : questionNodes) arr.add(node);
        q.setQuestions(MAPPER.writeValueAsString(arr));
        return q;
    }

    private void clearForm() {
        selected = null;
        txtTitle.clear();
        cmbDifficulty.setValue(null);
        chkPublished.setSelected(false);
        txtQuestion.clear();
        txtOpt0.clear(); txtOpt1.clear(); txtOpt2.clear(); txtOpt3.clear();
        cmbCorrect.setValue(null);
        questionNodes.clear();
        questionPreviews.clear();
        lblQCount.setText("0 question(s)");
        lblStatus.setText("");
        for (javafx.scene.Node node : quizListContainer.getChildren()) {
            if (node instanceof VBox) {
                ((VBox) node).setStyle("-fx-background-color:#FFFFFF;-fx-border-color:#E5EAF2;"
                        + "-fx-border-radius:8;-fx-background-radius:8;-fx-cursor:hand;");
            }
        }
    }

    private void refreshCorrectCombo() {
        String prev = cmbCorrect.getValue();
        cmbCorrect.setItems(FXCollections.observableArrayList(
                label("A", txtOpt0), label("B", txtOpt1),
                label("C", txtOpt2), label("D", txtOpt3)
        ));
        if (prev != null) {
            cmbCorrect.getItems().stream()
                    .filter(s -> s.startsWith(prev.charAt(0) + ""))
                    .findFirst().ifPresent(cmbCorrect::setValue);
        }
    }

    private String label(String letter, TextField field) {
        String text = field.getText().trim();
        return text.isEmpty() ? letter : letter + " — " + text;
    }

    // ── Button handlers ────────────────────────────────────────────────

    @FXML private void handleAdd() {
        if (!validateMeta()) return;
        try {
            Quiz q = buildFromForm();
            service.addQuiz(q);
            showStatus("Quiz ajouté (id=" + q.getId() + ")", false);
            clearForm(); loadQuizzes();
        } catch (Exception e) {
            showStatus("Erreur : " + e.getMessage(), true);
        }
    }

    @FXML private void handleUpdate() {
        if (selected == null) { showStatus("Sélectionner un quiz.", true); return; }
        if (!validateMeta()) return;
        try {
            Quiz q = buildFromForm();
            q.setId(selected.getId());
            service.updateQuiz(q);
            showStatus("Quiz mis à jour.", false);
            clearForm(); loadQuizzes();
        } catch (Exception e) {
            showStatus("Erreur : " + e.getMessage(), true);
        }
    }

    @FXML private void handleDelete() {
        if (selected == null) { showStatus("Sélectionner un quiz.", true); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer \"" + selected.getTitle() + "\" ?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    service.deleteQuiz(selected.getId());
                    showStatus("Quiz supprimé.", false);
                    clearForm(); loadQuizzes();
                } catch (SQLException e) {
                    showStatus("Erreur : " + e.getMessage(), true);
                }
            }
        });
    }

    @FXML private void handleRefresh() { loadQuizzes(); }
    @FXML private void handleClear()   { clearForm(); }

    // ── Util ───────────────────────────────────────────────────────────

    private void showStatus(String msg, boolean error) {
        lblStatus.setText(msg);
        lblStatus.getStyleClass().removeAll("status-success", "status-error");
        lblStatus.getStyleClass().add(error ? "status-error" : "status-success");
    }
}
