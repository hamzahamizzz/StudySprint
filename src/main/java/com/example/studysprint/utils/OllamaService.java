package com.example.studysprint.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Client local Ollama (http://localhost:11434).
 * Modèle : "mistral".
 * Toutes les méthodes sont bloquantes — appeler depuis un Thread daemon
 * (jamais sur le JavaFX Application Thread).
 */
public class OllamaService {

    private static final String BASE_URL = "http://localhost:11434";
    private static final String MODEL    = "mistral";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    // ═══════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════

    /**
     * Vérifie si Ollama est joignable. Ne lève jamais d'exception.
     */
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

    /**
     * Génère un quiz au format JSON strict :
     * [{"question":"...","options":["A...","B...","C...","D..."],"answer":"A..."}]
     * Retourne la chaîne JSON nettoyée.
     */
    public String generateQuizQuestions(String subject, String difficulty, int count) throws Exception {
        // Prompt minimal : moins de tokens = réponse plus courte = moins de risque de troncature
        String prompt = "Réponds UNIQUEMENT avec un tableau JSON, rien d'autre, pas de markdown.\n"
                + "Génère " + count + " questions QCM en français sur \"" + safe(subject) + "\", niveau " + safe(difficulty) + ".\n"
                + "Format : [{\"question\":\"...\",\"options\":[\"A. ...\",\"B. ...\",\"C. ...\",\"D. ...\"],\"answer\":\"A. ...\"}]\n"
                + "answer = texte exact d'une option. JSON uniquement, complet, sans troncature.";

        String raw = callGenerate(prompt, Duration.ofSeconds(180));
        return cleanJson(raw);
    }

    /**
     * Génère 1 phrase d'indice qui ne révèle pas la réponse.
     */
    public String generateFlashcardHint(String front, String back) throws Exception {
        String prompt = "Tu es un assistant pédagogique. Voici une flashcard : "
                + "Recto = \"" + safe(front) + "\", Verso = \"" + safe(back) + "\". "
                + "Génère UNE SEULE phrase courte en français qui aide à se souvenir de la réponse "
                + "SANS jamais révéler ni mentionner directement la réponse. "
                + "Réponds uniquement avec la phrase, sans préfixe ni guillemets.";

        String raw = callGenerate(prompt, Duration.ofSeconds(30));
        return raw.trim().replaceAll("^[\"']|[\"']$", "");
    }

    /**
     * Génère un feedback motivant 2-3 phrases en français.
     * Si score < 50% → mentionne ce qu'il faut réviser.
     */
    public String generateScoreFeedback(int score, int total, String quizTitle) throws Exception {
        int pct = total > 0 ? (score * 100 / total) : 0;
        String reviseHint = (pct < 50)
                ? "L'élève a obtenu moins de 50%, mentionne explicitement qu'il doit réviser le sujet du quiz. "
                : "";

        String prompt = "Tu es un coach pédagogique bienveillant. "
                + "Un élève vient de terminer le quiz \"" + safe(quizTitle) + "\" avec "
                + score + " bonnes réponses sur " + total + " (" + pct + "%). "
                + reviseHint
                + "Donne un feedback motivant en français, 2 à 3 phrases maximum. "
                + "Pas de préfixe, pas de guillemets, juste les phrases.";

        String raw = callGenerate(prompt, Duration.ofSeconds(30));
        return raw.trim().replaceAll("^[\"']|[\"']$", "");
    }

    // ═══════════════════════════════════════════
    // INTERNAL
    // ═══════════════════════════════════════════

    private String callGenerate(String prompt, Duration timeout) throws Exception {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("model", MODEL);
        body.put("prompt", prompt);
        body.put("stream", false);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/generate"))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                .build();

        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("Ollama HTTP " + resp.statusCode() + " : " + resp.body());
        }

        JsonNode tree = MAPPER.readTree(resp.body());
        JsonNode r    = tree.path("response");
        if (r.isMissingNode() || r.isNull()) {
            throw new RuntimeException("Réponse Ollama invalide : pas de champ 'response'");
        }
        return r.asText();
    }

    /** Nettoie une réponse LLM : retire backticks markdown, garde le tableau JSON.
     *  Si le tableau est tronqué, tente de récupérer les objets complets. */
    private String cleanJson(String s) {
        if (s == null) return "[]";
        String t = s.trim();
        // Retire ```json … ``` ou ``` … ```
        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            if (firstNl > 0) t = t.substring(firstNl + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
            t = t.trim();
        }
        // Garde uniquement de [ à ] (tolère du texte parasite avant/après)
        int start = t.indexOf('[');
        if (start < 0) return "[]";
        int end = t.lastIndexOf(']');
        if (end > start) {
            t = t.substring(start, end + 1);
            // Vérifie que le JSON est parseable
            try {
                MAPPER.readTree(t);
                return t.trim();
            } catch (Exception ignored) {}
        }
        // JSON tronqué : reconstruit un tableau avec les objets complets extraits
        t = (start < t.length()) ? t.substring(start) : t;
        return extractCompleteObjects(t);
    }

    /**
     * Extrait les objets JSON complets d'un tableau potentiellement tronqué.
     * Ex : [{"a":1},{"b":2},{"c":3   ← tronqué → retourne [{"a":1},{"b":2}]
     */
    private String extractCompleteObjects(String partial) {
        StringBuilder result = new StringBuilder("[");
        int depth = 0;
        int objStart = -1;
        boolean inString = false;
        boolean escape = false;
        boolean first = true;

        for (int i = 0; i < partial.length(); i++) {
            char c = partial.charAt(i);
            if (escape) { escape = false; continue; }
            if (c == '\\' && inString) { escape = true; continue; }
            if (c == '"') { inString = !inString; continue; }
            if (inString) continue;

            if (c == '{') {
                if (depth == 0) objStart = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && objStart >= 0) {
                    String obj = partial.substring(objStart, i + 1);
                    try {
                        MAPPER.readTree(obj); // valide l'objet
                        if (!first) result.append(',');
                        result.append(obj);
                        first = false;
                    } catch (Exception ignored) {}
                    objStart = -1;
                }
            }
        }
        result.append(']');
        String res = result.toString();
        if (res.equals("[]")) return "[]";
        try { MAPPER.readTree(res); return res; } catch (Exception e) { return "[]"; }
    }

    private String safe(String s) {
        if (s == null) return "";
        return s.replace("\"", "'").replace("\n", " ").trim();
    }
}
