package com.example.studysprint.api;

import com.example.studysprint.api.dto.ChapitreDto;
import com.example.studysprint.api.dto.MatiereDto;
import com.example.studysprint.api.mapper.DtoMapper;
import com.example.studysprint.modules.matieres.models.Chapitre;
import com.example.studysprint.modules.matieres.models.Matiere;
import com.example.studysprint.modules.matieres.services.ChapitreService;
import com.example.studysprint.modules.matieres.services.MatiereService;
import com.example.studysprint.modules.matieres.services.PdfService;
import com.example.studysprint.modules.matieres.services.QrCodeService;
import com.google.gson.Gson;
import static spark.Spark.*;

public class ApiServer {
    private static final Gson gson = new Gson();

    private static MatiereService getMatiereService() {
        return new MatiereService();
    }

    private static ChapitreService getChapitreService() {
        return new ChapitreService();
    }

    private static PdfService getPdfService() {
        return new PdfService();
    }

    private static QrCodeService getQrCodeService() {
        return new QrCodeService();
    }

    public static void start() {
        port(4567);
        // CORS pour développement
        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
            res.header("Access-Control-Allow-Headers", "Content-Type");
        });

        // GET /api/matieres
        get("/api/matieres", (req, res) -> {
            var matieres = getMatiereService().getAll().stream().map(DtoMapper::toDto).toList();
            res.type("application/json");
            return gson.toJson(matieres);
        });

        // GET /api/matieres/:id/chapitres
        get("/api/matieres/:id/chapitres", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            var chapitres = getChapitreService().getBySubjectId(id).stream().map(DtoMapper::toDto).toList();
            res.type("application/json");
            return gson.toJson(chapitres);
        });

        // GET /api/chapitres/:id/pdf (génère PDF du chapitre seul)
        get("/api/chapitres/:id/pdf", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            Chapitre chapitre = getChapitreService().getById(id);
            if (chapitre == null) {
                res.status(404);
                return "Chapitre non trouvé";
            }
            Matiere matiere = getMatiereService().getById(chapitre.getSubjectId());
            byte[] pdfData = getPdfService().genererResumeChapitres(java.util.List.of(chapitre), matiere.getName());
            res.type("application/pdf");
            res.header("Content-Disposition", "attachment; filename=chapitre_" + id + ".pdf");
            return pdfData;
        });

        // POST /api/chapitres/pdf (génère PDF à partir d'une liste d'IDs)
        post("/api/chapitres/pdf", (req, res) -> {
            var body = gson.fromJson(req.body(), java.util.Map.class);
            @SuppressWarnings("unchecked")
            java.util.List<Double> idsDouble = (java.util.List<Double>) body.get("chapitreIds");
            java.util.List<Integer> ids = idsDouble.stream().map(Double::intValue).toList();
            java.util.List<Chapitre> chapitres = ids.stream().map(getChapitreService()::getById).toList();
            if (chapitres.isEmpty() || chapitres.get(0) == null) {
                res.status(400);
                return "IDs invalides";
            }
            Matiere matiere = getMatiereService().getById(chapitres.get(0).getSubjectId());
            byte[] pdfData = getPdfService().genererResumeChapitres(chapitres, matiere.getName());
            res.type("application/pdf");
            res.header("Content-Disposition", "attachment; filename=resume.pdf");
            return pdfData;
        });

        // GET /api/qrcode/chapitre/:id (génère QR code vers le détail du chapitre)
        get("/api/qrcode/chapitre/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            String url = "http://localhost:4567/chapitre/" + id;// URL d'accès direct (voir endpoint suivant)
            byte[] qrPng = getQrCodeService().genererQrCode(url, 300, 300);
            res.type("image/png");
            return qrPng;
        });

        // GET /api/chapitres/:id (détail d'un chapitre pour redirection)
        get("/api/chapitres/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            Chapitre chapitre = getChapitreService().getById(id);
            if (chapitre == null) {
                res.status(404);
                return "Chapitre introuvable";
            }
            res.type("application/json");
            return gson.toJson(DtoMapper.toDto(chapitre));
        });


        // Servir une page HTML statique pour afficher un chapitre
        get("/chapitre/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            // On génère une page simple qui appelle l'API
            String html = """
        <!DOCTYPE html>
        <html>
        <head><meta charset="UTF-8"><title>Chapitre</title>
        <style>body{font-family:sans-serif;margin:20px;}</style>
        </head>
        <body>
        <div id="content">Chargement...</div>
        <script>
            fetch('/api/chapitres/%d')
                .then(r => r.json())
                .then(c => {
                    let html = '<h1>' + c.title + '</h1>';
                    if(c.orderNo) html += '<p>Ordre: ' + c.orderNo + '</p>';
                    if(c.summary) html += '<h3>Résumé</h3><p>' + c.summary + '</p>';
                    if(c.content) html += '<h3>Contenu</h3><div>' + c.content + '</div>';
                    document.getElementById('content').innerHTML = html;
                })
                .catch(e => document.getElementById('content').innerHTML = 'Erreur chargement');
        </script>
        </body>
        </html>
        """.formatted(id);
            res.type("text/html");
            return html;
        });

        // GET /api/health
        get("/api/health", (req, res) -> {
            res.type("application/json");
            return "{\"status\":\"OK\",\"timestamp\":" + System.currentTimeMillis() + "}";
        });
    }
}