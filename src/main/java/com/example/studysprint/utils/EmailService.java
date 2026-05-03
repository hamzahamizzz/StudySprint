package com.example.studysprint.utils;

import javafx.application.Platform;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import java.util.function.Consumer;

public class EmailService {

    static final String SMTP_HOST        = "smtp.gmail.com";
    static final int    SMTP_PORT        = 587;
    static final String SENDER_EMAIL     = "mezazighcharaf@gmail.com";
    static final String SENDER_PASSWORD  = "ckkfftlzkapuaccd";

    public boolean isConfigured() {
        return !SENDER_PASSWORD.equals("ckkfftlzkapuaccd");
    }

    // ── Private session factory ───────────────────────────────────────────

    private Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            String.valueOf(SMTP_PORT));

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
            }
        });
    }

    // ── Method A : quiz result email ──────────────────────────────────────

    public void sendQuizResult(String toEmail, String studentName, String quizTitle,
                               int score, int total,
                               String aiFeedback,
                               Runnable onSuccess, Consumer<String> onError) {
        Thread t = new Thread(() -> {
            try {
                double pct        = total > 0 ? (score * 100.0 / total) : 0;
                String scoreColor = pct >= 70 ? "#4CAF50" : pct >= 50 ? "#FF9800" : "#F44336";
                String pctStr     = String.format("%.0f%%", pct);

                String feedbackBlock = (aiFeedback != null && !aiFeedback.isBlank())
                        ? "<div style='margin:24px 0;padding:16px 20px;"
                          + "background:#F5F5F5;border-left:4px solid #1976D2;"
                          + "border-radius:4px;font-style:italic;color:#555;'>"
                          + escapeHtml(aiFeedback) + "</div>"
                        : "";

                String html = "<!DOCTYPE html><html><body style='margin:0;padding:0;"
                        + "font-family:Segoe UI,Arial,sans-serif;background:#F7FFFC;'>"
                        + "<div style='max-width:600px;margin:32px auto;background:#fff;"
                        + "border-radius:12px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,.08);'>"

                        // Header
                        + "<div style='background:#1976D2;padding:28px 32px;text-align:center;'>"
                        + "<h1 style='color:#fff;margin:0;font-size:24px;letter-spacing:1px;'>"
                        + "StudySprint</h1>"
                        + "<p style='color:rgba(255,255,255,.8);margin:6px 0 0;font-size:14px;'>"
                        + "Résultat de quiz</p>"
                        + "</div>"

                        // Body
                        + "<div style='padding:32px;'>"
                        + "<p style='font-size:16px;color:#333;margin:0 0 24px;'>Bonjour "
                        + escapeHtml(studentName) + ",</p>"
                        + "<p style='font-size:15px;color:#555;margin:0 0 20px;'>Voici ton résultat pour le quiz "
                        + "<strong>" + escapeHtml(quizTitle) + "</strong>&nbsp;:</p>"

                        // Score
                        + "<div style='text-align:center;margin:28px 0;'>"
                        + "<span style='font-size:72px;font-weight:bold;color:" + scoreColor + ";'>"
                        + pctStr + "</span>"
                        + "<p style='margin:8px 0 0;font-size:15px;color:#777;'>"
                        + score + " bonne(s) réponse(s) sur " + total + "</p>"
                        + "</div>"

                        // AI feedback
                        + feedbackBlock

                        // Footer
                        + "<hr style='border:none;border-top:1px solid #E5EAF2;margin:28px 0;'/>"
                        + "<p style='font-size:12px;color:#B0BBC8;text-align:center;margin:0;'>"
                        + "StudySprint — Continue à apprendre chaque jour !</p>"
                        + "</div></div></body></html>";

                Session session = createSession();
                MimeMessage msg = new MimeMessage(session);
                msg.setFrom(new InternetAddress(SENDER_EMAIL, "StudySprint"));
                msg.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
                msg.setSubject("Ton résultat — " + quizTitle, "UTF-8");
                msg.setContent(html, "text/html; charset=utf-8");
                Transport.send(msg);

                Platform.runLater(onSuccess);
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                Platform.runLater(() -> onError.accept(msg));
            }
        }, "mail-quiz-result");
        t.setDaemon(true);
        t.start();
    }

    // ── Method B : deck PDF attachment ───────────────────────────────────

    public void sendDeckPDF(String toEmail, String studentName, String deckTitle,
                            byte[] pdfBytes,
                            Runnable onSuccess, Consumer<String> onError) {
        Thread t = new Thread(() -> {
            try {
                Session session = createSession();
                MimeMessage msg = new MimeMessage(session);
                msg.setFrom(new InternetAddress(SENDER_EMAIL, "StudySprint"));
                msg.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
                msg.setSubject("Ton deck — " + deckTitle, "UTF-8");

                // Text part
                MimeBodyPart textPart = new MimeBodyPart();
                textPart.setContent(
                        "<p style='font-family:Segoe UI,Arial,sans-serif;'>Bonjour "
                        + escapeHtml(studentName) + ",</p>"
                        + "<p>Voici ton deck <strong>" + escapeHtml(deckTitle)
                        + "</strong> en pièce jointe.</p>"
                        + "<p style='color:#B0BBC8;font-size:12px;'>StudySprint</p>",
                        "text/html; charset=utf-8");

                // PDF attachment
                MimeBodyPart pdfPart = new MimeBodyPart();
                pdfPart.setContent(pdfBytes, "application/pdf");
                pdfPart.setFileName(deckTitle.replaceAll("[^\\w\\- ]", "_") + ".pdf");

                MimeMultipart multipart = new MimeMultipart();
                multipart.addBodyPart(textPart);
                multipart.addBodyPart(pdfPart);
                msg.setContent(multipart);

                Transport.send(msg);
                Platform.runLater(onSuccess);
            } catch (Exception e) {
                String errMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                Platform.runLater(() -> onError.accept(errMsg));
            }
        }, "mail-deck-pdf");
        t.setDaemon(true);
        t.start();
    }

    // ── Utility ───────────────────────────────────────────────────────────

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
