package com.example.studysprint.modules.matieres.services;

import com.example.studysprint.modules.matieres.models.Chapitre;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.util.List;

public class PdfService {

    public byte[] genererResumeChapitres(List<Chapitre> chapitres, String matiereNom) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            // Titre principal
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Résumé des chapitres : " + matiereNom, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(Chunk.NEWLINE);

            for (Chapitre chapitre : chapitres) {
                // Titre du chapitre
                Font chapFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
                Paragraph chapTitle = new Paragraph(
                        (chapitre.getOrderNo() != null ? chapitre.getOrderNo() + ". " : "") + chapitre.getTitle(),
                        chapFont
                );
                document.add(chapTitle);

                // Résumé (summary) ou contenu tronqué
                String resume = chapitre.getSummary();
                if (resume == null || resume.isBlank()) {
                    resume = chapitre.getContent();
                    if (resume != null && resume.length() > 500) resume = resume.substring(0, 500) + "...";
                }
                if (resume != null && !resume.isBlank()) {
                    Paragraph summaryPara = new Paragraph(resume, FontFactory.getFont(FontFactory.HELVETICA, 11));
                    document.add(summaryPara);
                }

                // Points clés IA (ai_key_point)
                if (chapitre.getAiKeyPoint() != null && !chapitre.getAiKeyPoint().isBlank()) {
                    Paragraph aiKey = new Paragraph("Points clés : " + chapitre.getAiKeyPoint(), FontFactory.getFont(FontFactory.HELVETICA, 10));
                    aiKey.setFont(FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10));
                    document.add(aiKey);
                }

                document.add(Chunk.NEWLINE);
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur génération PDF", e);
        }
    }
}