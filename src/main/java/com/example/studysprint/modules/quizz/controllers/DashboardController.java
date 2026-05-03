package com.example.studysprint.modules.quizz.controllers;

import com.example.studysprint.modules.quizz.services.DashboardService;
import com.example.studysprint.modules.quizz.services.DashboardService.DashboardData;
import com.example.studysprint.modules.quizz.services.DashboardService.DueCard;
import com.example.studysprint.modules.quizz.services.DashboardService.TopQuiz;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML private ProgressIndicator loadingSpinner;
    @FXML private VBox            contentBox;

    private final DashboardService service = new DashboardService();

    // ── Entry point ───────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Content hidden until data arrives
        contentBox.setVisible(false);
        contentBox.setManaged(false);
        loadingSpinner.setVisible(true);
        loadingSpinner.setManaged(true);
    }

    public void initDashboard(long userId) {
        loadingSpinner.setVisible(true);
        loadingSpinner.setManaged(true);
        contentBox.setVisible(false);
        contentBox.setManaged(false);

        Thread t = new Thread(() -> {
            try {
                DashboardData data = service.loadAll(userId);
                Platform.runLater(() -> buildUI(data));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loadingSpinner.setVisible(false);
                    loadingSpinner.setManaged(false);
                    Label err = new Label("Erreur chargement : " + e.getMessage());
                    err.setStyle("-fx-text-fill:#D32F2F;-fx-padding:24;");
                    contentBox.getChildren().add(err);
                    contentBox.setVisible(true);
                    contentBox.setManaged(true);
                });
            }
        }, "dashboard-loader");
        t.setDaemon(true);
        t.start();
    }

    // ── UI builder ────────────────────────────────────────────────────────

    private void buildUI(DashboardData data) {
        contentBox.getChildren().clear();

        contentBox.getChildren().addAll(
                buildKpiRow(data),
                buildLineChart(data),
                buildHeatmap(data.heatmap()),
                buildBottomRow(data)
        );

        loadingSpinner.setVisible(false);
        loadingSpinner.setManaged(false);
        contentBox.setVisible(true);
        contentBox.setManaged(true);
    }

    // ── KPI row ───────────────────────────────────────────────────────────

    private HBox buildKpiRow(DashboardData data) {
        VBox kpi1 = kpiCard("Quiz tentés",
                String.valueOf(data.totalAttempts()), "#1565C0", "#E3F2FD");

        String avgColor = data.avgScore() >= 70 ? "#2E7D32" : "#1565C0";
        String avgBg    = data.avgScore() >= 70 ? "#E8F5E9" : "#E3F2FD";
        VBox kpi2 = kpiCard("Score moyen",
                String.format("%.0f%%", data.avgScore()), avgColor, avgBg);

        String streakColor = data.streak() > 3 ? "#E65100" : "#1565C0";
        String streakBg    = data.streak() > 3 ? "#FFF3E0" : "#E3F2FD";
        VBox kpi3 = kpiCard("Streak",
                data.streak() + " j", streakColor, streakBg);

        String dueColor = data.dueCount() > 0 ? "#C62828" : "#2E7D32";
        String dueBg    = data.dueCount() > 0 ? "#FFEBEE" : "#E8F5E9";
        VBox kpi4 = kpiCard("Révisions dues",
                String.valueOf(data.dueCount()), dueColor, dueBg);

        HBox row = new HBox(16, kpi1, kpi2, kpi3, kpi4);
        row.setAlignment(Pos.CENTER_LEFT);
        for (var kpi : List.of(kpi1, kpi2, kpi3, kpi4)) HBox.setHgrow(kpi, Priority.ALWAYS);
        return row;
    }

    private VBox kpiCard(String label, String value, String color, String bg) {
        Label lblValue = new Label(value);
        lblValue.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        lblValue.setStyle("-fx-text-fill:" + color + ";");

        Label lblLabel = new Label(label);
        lblLabel.setStyle("-fx-text-fill:#6B7C94;-fx-font-size:12;");

        VBox card = new VBox(4, lblValue, lblLabel);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle("-fx-background-color:" + bg + ";"
                + "-fx-border-color:" + color + ";"
                + "-fx-border-width:0 0 0 4;"
                + "-fx-background-radius:10;"
                + "-fx-border-radius:10;");
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    // ── Line chart — score over last 7 days ───────────────────────────────

    private LineChart<String, Number> buildLineChart(DashboardData data) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Jour");

        NumberAxis yAxis = new NumberAxis(0, 100, 10);
        yAxis.setLabel("Score (%)");

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Scores — 7 derniers jours");
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(260);
        chart.setCreateSymbols(true);
        chart.setStyle("-fx-background-color:transparent;");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Score moyen");

        List<String> days   = data.chartDays();
        List<Double> scores = data.chartScores();
        for (int i = 0; i < days.size(); i++) {
            double s = scores.get(i);
            if (!Double.isNaN(s)) {
                series.getData().add(new XYChart.Data<>(days.get(i), s));
            }
        }

        chart.getData().add(series);
        return chart;
    }

    // ── Activity heatmap — 28 days ────────────────────────────────────────

    private VBox buildHeatmap(Map<String, Integer> heatmap) {
        Label title = sectionLabel("Activité — 28 derniers jours");

        // 4 rows × 7 columns
        GridPane grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(5);

        LocalDate today = LocalDate.now();
        // Start from 27 days ago so last cell = today
        LocalDate startDay = today.minusDays(27);

        for (int i = 0; i < 28; i++) {
            LocalDate d     = startDay.plusDays(i);
            int       count = heatmap.getOrDefault(d.toString(), 0);
            String    bg    = heatmapColor(count);

            Label cell = new Label();
            cell.setPrefSize(22, 22);
            cell.setStyle("-fx-background-color:" + bg + ";"
                    + "-fx-background-radius:4;"
                    + "-fx-cursor:hand;");
            Tooltip.install(cell, new Tooltip(d + " — " + count + " tentative(s)"));

            grid.add(cell, i % 7, i / 7);
        }

        // Day-of-week headers
        HBox header = new HBox(5);
        for (String dow : List.of("L", "M", "M", "J", "V", "S", "D")) {
            Label lh = new Label(dow);
            lh.setPrefWidth(22);
            lh.setAlignment(Pos.CENTER);
            lh.setStyle("-fx-font-size:10;-fx-text-fill:#B0BBC8;-fx-font-weight:bold;");
            header.getChildren().add(lh);
        }

        VBox box = new VBox(8, title, header, grid);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color:#FFFFFF;-fx-border-color:#E5EAF2;"
                + "-fx-border-radius:10;-fx-background-radius:10;");
        return box;
    }

    private String heatmapColor(int count) {
        if (count == 0) return "#F0F4F8";
        if (count == 1) return "#BBDEFB";
        if (count == 2) return "#64B5F6";
        if (count <= 4) return "#1976D2";
        return "#0D47A1";
    }

    // ── Bottom row: top quizzes + due flashcards ──────────────────────────

    private HBox buildBottomRow(DashboardData data) {
        VBox topQuizzesBox = buildTopQuizzes(data.topQuizzes());
        VBox dueCardsBox   = buildDueCards(data.dueCards());
        HBox.setHgrow(topQuizzesBox, Priority.ALWAYS);
        HBox.setHgrow(dueCardsBox,   Priority.ALWAYS);
        HBox row = new HBox(16, topQuizzesBox, dueCardsBox);
        return row;
    }

    private VBox buildTopQuizzes(List<TopQuiz> top) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color:#FFFFFF;-fx-border-color:#E5EAF2;"
                + "-fx-border-radius:10;-fx-background-radius:10;");
        box.getChildren().add(sectionLabel("🏆 Top 3 quizzes notés"));

        if (top.isEmpty()) {
            box.getChildren().add(emptyLabel("Aucune note encore."));
        } else {
            String[] medals = {"🥇", "🥈", "🥉"};
            for (int i = 0; i < top.size(); i++) {
                TopQuiz q = top.get(i);
                String  m = i < medals.length ? medals[i] : "  ";

                Label lblTitle = new Label(m + "  " + q.title());
                lblTitle.setStyle("-fx-font-weight:bold;-fx-text-fill:#4A5673;-fx-font-size:13;");
                lblTitle.setWrapText(true);

                Label lblScore = new Label(q.avgScore() + " ★");
                lblScore.setStyle("-fx-text-fill:#FFA000;-fx-font-weight:bold;-fx-font-size:13;");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                HBox row = new HBox(8, lblTitle, spacer, lblScore);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(8, 12, 8, 12));
                row.setStyle("-fx-background-color:#F7FFFC;-fx-border-color:#E5EAF2;"
                        + "-fx-border-radius:8;-fx-background-radius:8;");
                box.getChildren().add(row);
            }
        }
        return box;
    }

    private VBox buildDueCards(List<DueCard> cards) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color:#FFFFFF;-fx-border-color:#E5EAF2;"
                + "-fx-border-radius:10;-fx-background-radius:10;");
        box.getChildren().add(sectionLabel("📚 Flashcards à réviser"));

        if (cards.isEmpty()) {
            box.getChildren().add(emptyLabel("Toutes les cartes sont à jour ✅"));
        } else {
            for (DueCard c : cards) {
                boolean overdue = c.nextReview().isBefore(LocalDate.now());
                String  tagBg   = overdue ? "#FFEBEE" : "#E8F5E9";
                String  tagFg   = overdue ? "#C62828" : "#2E7D32";
                String  dateStr = overdue ? "En retard" : c.nextReview().toString();

                Label lblFront = new Label(c.front());
                lblFront.setStyle("-fx-text-fill:#4A5673;-fx-font-size:12;");
                lblFront.setWrapText(true);
                HBox.setHgrow(lblFront, Priority.ALWAYS);

                Label lblDate = new Label(dateStr);
                lblDate.setStyle("-fx-text-fill:" + tagFg + ";-fx-font-size:11;-fx-font-weight:bold;"
                        + "-fx-background-color:" + tagBg + ";-fx-padding:2 8;"
                        + "-fx-background-radius:10;");

                HBox row = new HBox(8, lblFront, lblDate);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(7, 12, 7, 12));
                row.setStyle("-fx-background-color:#FAFAFA;-fx-border-color:#E5EAF2;"
                        + "-fx-border-radius:8;-fx-background-radius:8;");
                box.getChildren().add(row);
            }
        }
        return box;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        l.setStyle("-fx-text-fill:#4A5673;");
        return l;
    }

    private Label emptyLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill:#B0BBC8;-fx-font-size:12;-fx-font-style:italic;");
        return l;
    }
}
