package com.example.studysprint.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Client local Ollama (http://localhost:11434). Modèle : mistral.
 * Toutes les méthodes sont bloquantes — appeler depuis un Thread daemon,
 * jamais sur le JavaFX Application Thread.
 * Aucune méthode ne propage d'exception : retourne "" ou false en cas d'erreur.
 */
public class OllamaService {

    private static final String BASE_URL = "http://localhost:11434";
    private static final String MODEL    = "mistral";

    static final ObjectMapper MAPPER = new ObjectMapper();

    static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    // ── Private helper ────────────────────────────────────────────────────

    private String callOllamaGenerate(String prompt, int timeoutSeconds) {
        return callOllamaGenerate(prompt, timeoutSeconds, false);
    }

    private String callOllamaGenerate(String prompt, int timeoutSeconds, boolean jsonMode) {
        try {
            ObjectNode body = MAPPER.createObjectNode();
            body.put("model",  MODEL);
            body.put("prompt", prompt);
            body.put("stream", false);
            if (jsonMode) body.put("format", "json");

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/generate"))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                    .build();

            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return "";

            JsonNode root = MAPPER.readTree(resp.body());
            String text = root.path("response").asText("").trim();
            return stripBackticks(text);
        } catch (Exception e) {
            return "";
        }
    }

    /** Strips leading ```json / ``` fences and trailing ```. */
    private String stripBackticks(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.startsWith("```")) {
            int nl = t.indexOf('\n');
            t = (nl >= 0) ? t.substring(nl + 1) : t.substring(3);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
            t = t.trim();
        }
        return t;
    }

    // ── Public method 1 : generateQuizQuestions ───────────────────────────

    public String generateQuizQuestions(String subject, String difficulty, int count) {
        String prompt = "Génère exactement " + count + " questions QCM en français sur le sujet '"
                + safe(subject) + "' niveau " + safe(difficulty) + ". "
                + "Réponds UNIQUEMENT avec un tableau JSON valide, rien d'autre. "
                + "Format exact : [{\"question\":\"texte\",\"options\":[\"A\",\"B\",\"C\",\"D\"],\"answer\":\"texte exact de la bonne option\"}]";
        String raw = callOllamaGenerate(prompt, 90, true);
        return extractJsonArray(raw);
    }

    // ── Public method 2 : generateFlashcardsFromText ──────────────────────

    public String generateFlashcardsFromText(String courseText, int count) {
        String prompt = "Extrait exactement " + count + " concepts clés du texte suivant et génère des flashcards. "
                + "Réponds UNIQUEMENT avec un tableau JSON valide, rien d'autre. "
                + "Format exact : [{\"front\":\"question courte\",\"back\":\"réponse concise\",\"hint\":\"indice mnémotechnique\"}] "
                + "Texte : " + safe(courseText);
        String raw = callOllamaGenerate(prompt, 90, true);
        return extractJsonArray(raw);
    }

    // ── Public method 3 : generateFlashcardHint ──────────────────────────

    public String generateFlashcardHint(String front, String back) {
        String prompt = "Tu es un assistant pédagogique. Flashcard — Recto: \""
                + safe(front) + "\", Verso: \"" + safe(back) + "\". "
                + "Génère UNE seule phrase courte en français qui aide à se souvenir "
                + "SANS révéler ni mentionner la réponse. "
                + "Réponds uniquement avec la phrase, sans préfixe ni guillemets.";
        return callOllamaGenerate(prompt, 30).replaceAll("^[\"']+|[\"']+$", "").trim();
    }

    // ── Public method 4 : generateScoreFeedback ───────────────────────────

    public String generateScoreFeedback(int score, int total, String quizTitle) {
        int pct = total > 0 ? (score * 100 / total) : 0;
        String revise = (pct < 50)
                ? "Le score est inférieur à 50%, suggère explicitement de réviser les sujets du quiz. "
                : "";
        String prompt = "Tu es un coach pédagogique bienveillant. Un élève vient de terminer le quiz \""
                + safe(quizTitle) + "\" avec " + score + " bonnes réponses sur " + total
                + " (" + pct + "%). " + revise
                + "Donne un feedback motivant en français en 2 à 3 phrases. "
                + "Pas de préfixe, pas de guillemets, juste les phrases.";
        return callOllamaGenerate(prompt, 30).replaceAll("^[\"']+|[\"']+$", "").trim();
    }

    // ── Public method 5 : generateAdaptiveRecommendation ─────────────────

    public String generateAdaptiveRecommendation(String quizTitle, String difficulty,
                                                  String failedQuestionsJSON) {
        String prompt = "Un étudiant a échoué à ces questions du quiz \"" + safe(quizTitle)
                + "\" (difficulté " + safe(difficulty) + "): " + failedQuestionsJSON + ". "
                + "Réponds UNIQUEMENT avec un objet JSON valide, rien d'autre. "
                + "Format exact : {\"concept\":\"concept à retravailler\",\"recommended_difficulty\":\"EASY\","
                + "\"explanation\":\"2 phrases d'explication\",\"suggested_quiz_title\":\"titre suggéré\"}";
        String raw = callOllamaGenerate(prompt, 60, true);
        return extractJsonObject(raw);
    }

    // ── Public method 6 : chat ────────────────────────────────────────────

    public String chat(String systemPrompt, List<Map<String, String>> history, String userMessage) {
        try {
            ArrayNode messages = MAPPER.createArrayNode();

            ObjectNode sys = MAPPER.createObjectNode();
            sys.put("role", "system");
            sys.put("content", systemPrompt != null ? systemPrompt : "");
            messages.add(sys);

            if (history != null) {
                for (Map<String, String> turn : history) {
                    ObjectNode msg = MAPPER.createObjectNode();
                    msg.put("role",    turn.getOrDefault("role", "user"));
                    msg.put("content", turn.getOrDefault("content", ""));
                    messages.add(msg);
                }
            }

            ObjectNode user = MAPPER.createObjectNode();
            user.put("role", "user");
            user.put("content", userMessage != null ? userMessage : "");
            messages.add(user);

            ObjectNode body = MAPPER.createObjectNode();
            body.put("model",  MODEL);
            body.set("messages", messages);
            body.put("stream", false);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/chat"))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                    .build();

            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return "";

            JsonNode root = MAPPER.readTree(resp.body());
            return root.path("message").path("content").asText("").trim();
        } catch (Exception e) {
            return "";
        }
    }

    // ── Public method 7 : isAvailable ────────────────────────────────────

    public boolean isAvailable() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/tags"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    // ── Private utilities ─────────────────────────────────────────────────

    private String extractJsonArray(String s) {
        if (s == null || s.isBlank()) return "[]";
        int start = s.indexOf('[');
        int end   = s.lastIndexOf(']');
        if (start < 0 || end <= start) return "[]";
        String candidate = s.substring(start, end + 1);
        try {
            MAPPER.readTree(candidate);
            return candidate.trim();
        } catch (Exception e) {
            return "[]";
        }
    }

    private String extractJsonObject(String s) {
        if (s == null || s.isBlank()) return "{}";
        int start = s.indexOf('{');
        int end   = s.lastIndexOf('}');
        if (start < 0 || end <= start) return "{}";
        String candidate = s.substring(start, end + 1);
        try {
            MAPPER.readTree(candidate);
            return candidate.trim();
        } catch (Exception e) {
            return "{}";
        }
    }

    private String safe(String s) {
        if (s == null) return "";
        return s.replace("\"", "'").replace("\n", " ").trim();
    }
}
