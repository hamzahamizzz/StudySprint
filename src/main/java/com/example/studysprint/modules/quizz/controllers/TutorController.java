package com.example.studysprint.modules.quizz.controllers;

import com.example.studysprint.utils.OllamaService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextFlow;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class TutorController implements Initializable {

    @FXML private Label       lblHeaderTitle;
    @FXML private Label       lblHeaderScore;
    @FXML private ScrollPane  chatScroll;
    @FXML private VBox        chatBox;
    @FXML private Label       loadingLabel;
    @FXML private TextField   userInput;
    @FXML private Button      sendBtn;

    private final OllamaService ollama = new OllamaService();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final List<Map<String, String>> conversationHistory = new ArrayList<>();

    private String systemPrompt = "";
    private int exchangeCount   = 0;

    // ── Public entry point ────────────────────────────────────────────────

    /**
     * Call this after loading the FXML to inject quiz context and start the tutor.
     * Each map in failedQuestions must contain: "question", "userAnswer", "correctAnswer".
     */
    public void initTutor(String quizTitle, String subject, int score, int total,
                          List<Map<String, Object>> failedQuestions) {

        lblHeaderTitle.setText("Tuteur IA — " + quizTitle);
        lblHeaderScore.setText(score + "/" + total);

        String failedJson = serializeFailedQuestions(failedQuestions);

        systemPrompt = "Tu es un tuteur socratique pour StudySprint.\n"
                + "RÈGLES: 1.Ne donne JAMAIS la réponse directement. "
                + "2.Commence par 'Que signifie X selon toi?'. "
                + "3.Erreur→question creusement. "
                + "4.Proche→question confirmation. "
                + "5.Après 3 échanges→explication claire. "
                + "6.Propose 1 ressource réelle (Baeldung/JavaDoc/OpenClassrooms). "
                + "7.Refuse le hors-sujet poliment. "
                + "8.Réponds en français uniquement.\n"
                + "CONTEXTE: Matière:" + subject
                + " Quiz:" + quizTitle
                + " Score:" + score + "/" + total
                + " Erreurs:" + failedJson;

        sendBtn.setOnAction(e -> handleSend());
        userInput.setOnAction(e -> handleSend());

        triggerInitialAnalysis();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Wired up in initTutor to have access to dynamic values.
    }

    // ── Initial auto-trigger ──────────────────────────────────────────────

    private void triggerInitialAnalysis() {
        setLoading(true);
        Thread t = new Thread(() -> {
            if (!ollama.isAvailable()) {
                Platform.runLater(() -> {
                    setLoading(false);
                    appendBubble("Ollama n'est pas disponible. Vérifiez que le serveur tourne sur localhost:11434.", false);
                });
                return;
            }
            // Empty userMessage — Ollama uses system prompt context to start the session
            String response = ollama.chat(systemPrompt, new ArrayList<>(), "");
            if (response == null || response.isBlank()) {
                response = "Bonjour ! Je suis votre tuteur pour ce quiz. Commençons par examiner vos erreurs.";
            }
            final String finalResponse = response;
            conversationHistory.add(message("assistant", finalResponse));
            Platform.runLater(() -> {
                setLoading(false);
                appendBubble(finalResponse, false);
                scrollToBottom();
            });
        });
        t.setDaemon(true);
        t.start();
    }

    // ── Send handler ──────────────────────────────────────────────────────

    private void handleSend() {
        String text = userInput.getText().trim();
        if (text.isEmpty()) return;

        userInput.clear();
        appendBubble(text, true);
        conversationHistory.add(message("user", text));
        exchangeCount++;

        sendBtn.setDisable(true);
        setLoading(true);

        List<Map<String, String>> historyCopy = new ArrayList<>(conversationHistory);

        Thread t = new Thread(() -> {
            if (!ollama.isAvailable()) {
                Platform.runLater(() -> {
                    setLoading(false);
                    sendBtn.setDisable(false);
                    appendBubble("Ollama n'est pas disponible. Vérifiez que le serveur tourne sur localhost:11434.", false);
                });
                return;
            }

            // After 3 exchanges, append a reminder to give a clear explanation
            String effectiveSystem = systemPrompt;
            if (exchangeCount >= 3) {
                effectiveSystem += "\nREMINDER: Tu as eu " + exchangeCount
                        + " échanges. Donne maintenant une explication claire et propose une ressource.";
            }

            String response = ollama.chat(effectiveSystem, historyCopy, text);
            if (response == null || response.isBlank()) {
                response = "Je n'ai pas pu générer une réponse. Reformulez votre question.";
            }
            final String finalResponse = response;
            conversationHistory.add(message("assistant", finalResponse));

            Platform.runLater(() -> {
                setLoading(false);
                sendBtn.setDisable(false);
                appendBubble(finalResponse, false);
                scrollToBottom();
            });
        });
        t.setDaemon(true);
        t.start();
    }

    // ── Bubble builder ────────────────────────────────────────────────────

    private void appendBubble(String text, boolean isUser) {
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setMaxWidth(Double.MAX_VALUE);

        if (isUser) {
            lbl.setStyle("-fx-text-fill: white; -fx-font-size:13;");
        } else {
            lbl.setStyle("-fx-text-fill: #212121; -fx-font-size:13;");
        }

        HBox bubble = new HBox(lbl);
        bubble.setPadding(new Insets(10, 14, 10, 14));
        bubble.setMaxWidth(Double.MAX_VALUE);

        if (isUser) {
            bubble.setAlignment(Pos.CENTER_RIGHT);
            bubble.setStyle(
                    "-fx-background-color: #1976D2;"
                    + "-fx-background-radius: 12 12 0 12;"
                    + "-fx-max-width: 70%;"
            );
        } else {
            bubble.setAlignment(Pos.CENTER_LEFT);
            bubble.setStyle(
                    "-fx-background-color: #E3F2FD;"
                    + "-fx-background-radius: 12 12 12 0;"
                    + "-fx-max-width: 70%;"
            );
        }

        HBox.setHgrow(lbl, Priority.ALWAYS);

        HBox row = new HBox(bubble);
        row.setPadding(new Insets(2, 0, 2, 0));
        if (isUser) {
            row.setAlignment(Pos.CENTER_RIGHT);
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
        }

        chatBox.getChildren().add(row);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void setLoading(boolean visible) {
        loadingLabel.setVisible(visible);
        loadingLabel.setManaged(visible);
    }

    private void scrollToBottom() {
        chatScroll.layout();
        chatScroll.setVvalue(1.0);
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> m = new HashMap<>();
        m.put("role",    role);
        m.put("content", content);
        return m;
    }

    private String serializeFailedQuestions(List<Map<String, Object>> failedQuestions) {
        if (failedQuestions == null || failedQuestions.isEmpty()) return "[]";
        try {
            return MAPPER.writeValueAsString(failedQuestions);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
