package com.example.studysprint.utils;

import com.example.studysprint.modules.quizz.models.Flashcard;
import com.example.studysprint.modules.quizz.models.FlashcardDeck;
import com.example.studysprint.modules.quizz.models.Quiz;
import com.example.studysprint.modules.quizz.models.QuizAttempt;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;

import java.awt.Desktop;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class PDFExportService {

    // ── Shared colors / fonts ─────────────────────────────────────────────

    private static final BaseColor BLUE_HEADER   = new BaseColor(0x19, 0x76, 0xD2);
    private static final BaseColor BLUE_LIGHT    = new BaseColor(0xE3, 0xF2, 0xFD);
    private static final BaseColor GREEN_LIGHT   = new BaseColor(0xE8, 0xF5, 0xE9);
    private static final BaseColor YELLOW_LIGHT  = new BaseColor(0xFF, 0xFD, 0xE7);
    private static final BaseColor AMBER         = new BaseColor(0xFF, 0xC1, 0x07);
    private static final BaseColor AMBER_BG      = new BaseColor(0xFF, 0xF8, 0xE1);
    private static final BaseColor GRAY          = new BaseColor(0x9E, 0x9E, 0x9E);
    private static final BaseColor GRAY_LIGHT    = new BaseColor(0xF5, 0xF5, 0xF5);
    private static final BaseColor GREEN_TEXT    = new BaseColor(0x2E, 0x7D, 0x32);
    private static final BaseColor ORANGE_TEXT   = new BaseColor(0xE6, 0x51, 0x00);
    private static final BaseColor RED_TEXT      = new BaseColor(0xC6, 0x28, 0x28);
    private static final BaseColor DARK          = new BaseColor(0x4A, 0x56, 0x73);
    private static final BaseColor WHITE         = BaseColor.WHITE;

    private static final Font F_TITLE      = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD,   DARK);
    private static final Font F_SUBTITLE   = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, GRAY);
    private static final Font F_SECTION    = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD,   DARK);
    private static final Font F_BODY       = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, DARK);
    private static final Font F_SMALL_GRAY = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, GRAY);
    private static final Font F_SMALL_GRN  = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, GREEN_TEXT);
    private static final Font F_HINT       = new Font(Font.FontFamily.HELVETICA, 11, Font.ITALIC, DARK);
    private static final Font F_BOLD_WH    = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD,   WHITE);
    private static final Font F_SCORE_GRN  = new Font(Font.FontFamily.HELVETICA, 32, Font.BOLD,   GREEN_TEXT);
    private static final Font F_SCORE_ORG  = new Font(Font.FontFamily.HELVETICA, 32, Font.BOLD,   ORANGE_TEXT);
    private static final Font F_SCORE_RED  = new Font(Font.FontFamily.HELVETICA, 32, Font.BOLD,   RED_TEXT);
    private static final Font F_Q_BOLD     = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD,   DARK);
    private static final Font F_ANS_GRN    = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, GREEN_TEXT);
    private static final Font F_ANS_RED    = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, RED_TEXT);
    private static final Font F_AI_TITLE   = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD,   ORANGE_TEXT);
    private static final Font F_AI_BODY    = new Font(Font.FontFamily.HELVETICA, 11, Font.ITALIC, DARK);
    private static final Font F_FOOTER     = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, GRAY);

    // ─────────────────────────────────────────────────────────────────────
    // Method A — Flashcard deck
    // ─────────────────────────────────────────────────────────────────────

    public byte[] exportFlashcardDeck(FlashcardDeck deck, List<Flashcard> cards) throws DocumentException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
        PdfWriter.getInstance(doc, out);
        doc.open();

        // ── Header
        Paragraph titleP = new Paragraph(deck.getTitle(), F_TITLE);
        titleP.setAlignment(Element.ALIGN_CENTER);
        doc.add(titleP);

        String dateStr = LocalDate.now().toString();
        Paragraph subP = new Paragraph(cards.size() + " flashcards  •  " + dateStr, F_SUBTITLE);
        subP.setAlignment(Element.ALIGN_CENTER);
        subP.setSpacingBefore(4);
        doc.add(subP);

        doc.add(new Chunk(new LineSeparator(0.5f, 100, GRAY, Element.ALIGN_CENTER, -2)));
        doc.add(Chunk.NEWLINE);

        // ── Cards
        int total = cards.size();
        for (int i = 0; i < total; i++) {
            Flashcard c = cards.get(i);

            // Recto / Verso row
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(6);
            table.setSpacingAfter(0);

            // Col 1 — Recto
            Phrase rectoPhrase = new Phrase();
            rectoPhrase.add(new Chunk("RECTO\n", F_SMALL_GRAY));
            rectoPhrase.add(new Chunk(c.getFront(), F_BODY));
            PdfPCell rectoCell = new PdfPCell(rectoPhrase);
            rectoCell.setBackgroundColor(BLUE_LIGHT);
            rectoCell.setPadding(10);
            rectoCell.setBorder(Rectangle.NO_BORDER);
            table.addCell(rectoCell);

            // Col 2 — Verso
            Phrase versoPhrase = new Phrase();
            versoPhrase.add(new Chunk("VERSO\n", F_SMALL_GRN));
            versoPhrase.add(new Chunk(c.getBack(), F_BODY));
            PdfPCell versoCell = new PdfPCell(versoPhrase);
            versoCell.setBackgroundColor(GREEN_LIGHT);
            versoCell.setPadding(10);
            versoCell.setBorder(Rectangle.NO_BORDER);
            table.addCell(versoCell);

            // Hint row (colspan=2)
            if (c.getHint() != null && !c.getHint().isBlank()) {
                PdfPCell hintCell = new PdfPCell(new Phrase("💡 Indice : " + c.getHint(), F_HINT));
                hintCell.setColspan(2);
                hintCell.setBackgroundColor(YELLOW_LIGHT);
                hintCell.setPadding(8);
                hintCell.setBorder(Rectangle.NO_BORDER);
                table.addCell(hintCell);
            }

            // Card number (colspan=2, right-aligned, no border)
            PdfPCell numCell = new PdfPCell(new Phrase("Carte " + (i + 1) + "/" + total, F_SMALL_GRAY));
            numCell.setColspan(2);
            numCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            numCell.setBorder(Rectangle.NO_BORDER);
            numCell.setBackgroundColor(GRAY_LIGHT);
            numCell.setPadding(4);
            table.addCell(numCell);

            doc.add(table);
            doc.add(Chunk.NEWLINE);
        }

        // ── Footer
        Paragraph footer = new Paragraph(
                "Généré par StudySprint — SM-2 Spaced Repetition | ESPRIT PIDEV 2025-2026",
                F_FOOTER);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(16);
        doc.add(footer);

        doc.close();
        return out.toByteArray();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Method B — Quiz result
    // ─────────────────────────────────────────────────────────────────────

    public byte[] exportQuizResult(Quiz quiz, QuizAttempt attempt,
                                   List<Map<String, Object>> questions,
                                   String aiFeedback) throws DocumentException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
        PdfWriter writer = PdfWriter.getInstance(doc, out);
        doc.open();

        // ── Header table with blue background
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);
        headerTable.setSpacingAfter(16);

        PdfPCell headerCell = new PdfPCell(new Phrase(quiz.getTitle(), F_BOLD_WH));
        headerCell.setBackgroundColor(BLUE_HEADER);
        headerCell.setPadding(16);
        headerCell.setBorder(Rectangle.NO_BORDER);
        headerCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        headerTable.addCell(headerCell);
        doc.add(headerTable);

        // ── Score
        double pct = (attempt.getTotalQuestions() > 0)
                ? (attempt.getCorrectCount() * 100.0 / attempt.getTotalQuestions()) : 0;
        Font scoreFont = pct >= 70 ? F_SCORE_GRN : pct >= 50 ? F_SCORE_ORG : F_SCORE_RED;
        String pctStr  = String.format("%.0f%%", pct);

        Paragraph scorePara = new Paragraph(pctStr, scoreFont);
        scorePara.setAlignment(Element.ALIGN_CENTER);
        scorePara.setSpacingAfter(4);
        doc.add(scorePara);

        Paragraph scoreDetail = new Paragraph(
                attempt.getCorrectCount() + " / " + attempt.getTotalQuestions() + " réponses correctes",
                F_SUBTITLE);
        scoreDetail.setAlignment(Element.ALIGN_CENTER);
        scoreDetail.setSpacingAfter(8);
        doc.add(scoreDetail);

        // ── Visual score bar via PdfContentByte
        PdfContentByte cb = writer.getDirectContent();
        float pageWidth = doc.right() - doc.left();
        float barX      = doc.left();
        float barY      = writer.getVerticalPosition(false) - 12f;
        float barH      = 8f;
        float barW      = pageWidth;

        // Background track
        cb.setColorFill(GRAY_LIGHT);
        cb.rectangle(barX, barY, barW, barH);
        cb.fill();

        // Fill
        BaseColor fillColor = pct >= 70 ? new BaseColor(0x4C, 0xAF, 0x50)
                            : pct >= 50 ? new BaseColor(0xFF, 0x98, 0x00)
                            :             new BaseColor(0xF4, 0x43, 0x36);
        cb.setColorFill(fillColor);
        cb.rectangle(barX, barY, (float)(barW * pct / 100.0), barH);
        cb.fill();

        doc.add(Chunk.NEWLINE);
        doc.add(Chunk.NEWLINE);

        // ── Questions
        if (questions != null && !questions.isEmpty()) {
            Paragraph qHeader = new Paragraph("Détail des questions", F_SECTION);
            qHeader.setSpacingBefore(12);
            qHeader.setSpacingAfter(8);
            doc.add(qHeader);

            for (int i = 0; i < questions.size(); i++) {
                Map<String, Object> q    = questions.get(i);
                String questionText      = str(q.get("question"));
                String userAnswer        = str(q.get("userAnswer"));
                String correctAnswer     = str(q.get("correctAnswer"));
                boolean isCorrect        = Boolean.TRUE.equals(q.get("isCorrect"));

                Paragraph qText = new Paragraph((i + 1) + ". " + questionText, F_Q_BOLD);
                qText.setSpacingBefore(8);
                doc.add(qText);

                Phrase answerPhrase = new Phrase();
                answerPhrase.add(new Chunk("Votre réponse : ", F_BODY));
                answerPhrase.add(new Chunk(userAnswer, isCorrect ? F_ANS_GRN : F_ANS_RED));
                answerPhrase.add(new Chunk(isCorrect ? "  ✓" : "  ✗", isCorrect ? F_ANS_GRN : F_ANS_RED));
                doc.add(new Paragraph(answerPhrase));

                if (!isCorrect && correctAnswer != null && !correctAnswer.isBlank()) {
                    Phrase correctPhrase = new Phrase();
                    correctPhrase.add(new Chunk("Bonne réponse : ", F_BODY));
                    correctPhrase.add(new Chunk(correctAnswer, F_ANS_GRN));
                    doc.add(new Paragraph(correctPhrase));
                }

                doc.add(new Chunk(new LineSeparator(0.3f, 100, GRAY_LIGHT, Element.ALIGN_LEFT, -2)));
            }
        }

        // ── AI feedback section
        if (aiFeedback != null && !aiFeedback.isBlank()) {
            doc.add(Chunk.NEWLINE);
            PdfPTable aiTable = new PdfPTable(1);
            aiTable.setWidthPercentage(100);
            aiTable.setSpacingBefore(12);

            PdfPCell aiTitleCell = new PdfPCell(new Phrase("Feedback IA", F_AI_TITLE));
            aiTitleCell.setBackgroundColor(AMBER_BG);
            aiTitleCell.setBorderColor(AMBER);
            aiTitleCell.setBorderWidth(1);
            aiTitleCell.setPadding(10);
            aiTable.addCell(aiTitleCell);

            PdfPCell aiBodyCell = new PdfPCell(new Phrase(aiFeedback, F_AI_BODY));
            aiBodyCell.setBackgroundColor(AMBER_BG);
            aiBodyCell.setBorderColor(AMBER);
            aiBodyCell.setBorderWidth(1);
            aiBodyCell.setPadding(10);
            aiTable.addCell(aiBodyCell);

            doc.add(aiTable);
        }

        // ── Footer
        Paragraph footer = new Paragraph(
                "Généré par StudySprint — SM-2 Spaced Repetition | ESPRIT PIDEV 2025-2026",
                F_FOOTER);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(20);
        doc.add(footer);

        doc.close();
        return out.toByteArray();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Method C — Save to ~/Documents and open
    // ─────────────────────────────────────────────────────────────────────

    public void saveToFile(byte[] pdfBytes, String fileName) throws Exception {
        File dir  = new File(System.getProperty("user.home") + File.separator + "Documents");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(pdfBytes);
        }
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(file);
        }
    }

    // ── Util ──────────────────────────────────────────────────────────────

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
