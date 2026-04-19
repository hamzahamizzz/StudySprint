package com.example.studysprint.modules.utilisateurs.controllers;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.modules.utilisateurs.services.UtilisateurService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.geometry.Side;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class UsersStatsController implements Initializable {

    @FXML private Label totalUsersLabel, studentsLabel, professorsLabel, activeLabel, inactiveLabel;
    @FXML private PieChart countryChart;
    @FXML private BarChart<String, Number> experienceChart;
    @FXML private StackedBarChart<Number, String> ageChart;
    @FXML private StackedBarChart<String, Number> profDestChart;
    @FXML private StackedBarChart<String, Number> studentDestChart;
    @FXML private LineChart<String, Number> signupsChart;

    private final UtilisateurService userService = new UtilisateurService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Optimisation de l'affichage des axes
        ((CategoryAxis) experienceChart.getXAxis()).setTickLabelRotation(-20);
        ((CategoryAxis) profDestChart.getXAxis()).setTickLabelRotation(-30);
        ((CategoryAxis) studentDestChart.getXAxis()).setTickLabelRotation(-30);
        
        // Force integer values on all Y or X axes representing counts
        setupIntegerAxis((NumberAxis) experienceChart.getYAxis());
        setupIntegerAxis((NumberAxis) ageChart.getXAxis());
        setupIntegerAxis((NumberAxis) signupsChart.getYAxis());
        setupIntegerAxis((NumberAxis) profDestChart.getYAxis());
        setupIntegerAxis((NumberAxis) studentDestChart.getYAxis());
        
        experienceChart.setAnimated(false);
        ageChart.setAnimated(false);
        signupsChart.setAnimated(false);
        countryChart.setAnimated(false);
        profDestChart.setAnimated(false);
        studentDestChart.setAnimated(false);
        profDestChart.setLegendVisible(true);
        studentDestChart.setLegendVisible(true);
        profDestChart.setLegendSide(Side.BOTTOM);
        studentDestChart.setLegendSide(Side.BOTTOM);
        ageChart.setLegendVisible(true);
        ageChart.setLegendSide(Side.BOTTOM);

        new Thread(() -> {
            List<Utilisateur> users = userService.getAll();
            Platform.runLater(() -> populateAll(users));
        }).start();
    }

    private void setupIntegerAxis(NumberAxis axis) {
        axis.setTickUnit(1);
        axis.setMinorTickCount(0);
        axis.setMinorTickVisible(false);
        axis.setAutoRanging(true);
        axis.setForceZeroInRange(true);
        axis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(axis) {
            @Override
            public String toString(Number object) {
                if (object.doubleValue() % 1 == 0) {
                    return String.format("%.0f", object.doubleValue());
                }
                return "";
            }
        });
    }

    private void addLabelsToBars(XYChart<String, Number> chart) {
        chart.getData().forEach(series -> {
            series.getData().forEach(data -> {
                Label label = new Label(data.getYValue().toString());
                label.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3436; -fx-padding: 0 0 5 0;");
                data.nodeProperty().addListener((ov, oldNode, newNode) -> {
                    if (newNode != null) {
                        // Position label above bar (simplified for vertical bars)
                    }
                });
            });
        });
    }
    
    // Version simplifiée : On ajoute les chiffres dans la légende ou à côté des noms
    private void populateAll(List<Utilisateur> users) {
        System.out.println("Populating Stats Board with " + users.size() + " users.");
        // Clear old data (essential for reliability)
        countryChart.getData().clear();
        experienceChart.getData().clear();
        ageChart.getData().clear();
        signupsChart.getData().clear();
        profDestChart.getData().clear();
        studentDestChart.getData().clear();

        // --- 1. KPIs ---
        totalUsersLabel.setText(String.valueOf(users.size()));
        
        long students = users.stream().filter(u -> "ROLE_STUDENT".equals(u.getRole())).count();
        long professors = users.stream().filter(u -> "ROLE_PROFESSOR".equals(u.getRole())).count();
        studentsLabel.setText(String.valueOf(students));
        professorsLabel.setText(String.valueOf(professors));

        long active = users.stream().filter(this::isActif).count();
        activeLabel.setText(String.valueOf(active));
        inactiveLabel.setText(String.valueOf(users.size() - active));

        // --- 2. Pays (PieChart) ---
        Map<String, Long> countryData = users.stream()
                .filter(u -> u.getPays() != null && !u.getPays().isEmpty())
                .collect(Collectors.groupingBy(Utilisateur::getPays, Collectors.counting()));
        
        System.out.println("Country Stats Map: " + countryData);
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        countryData.forEach((k, v) -> {
            PieChart.Data data = new PieChart.Data(k + " (" + v + ")", v);
            pieData.add(data);
        });
        countryChart.setData(pieData);

        activeLabel.setText(String.valueOf(active));
        inactiveLabel.setText(String.valueOf(users.size() - active));

        // --- 3. Expertise Profs (BarChart Vertical) ---
        Map<String, Long> expGroups = users.stream()
                .filter(u -> "ROLE_PROFESSOR".equalsIgnoreCase(u.getRole()) && u.getAnneesExperience() != null)
                .collect(Collectors.groupingBy(u -> getExpRange(u.getAnneesExperience()), Collectors.counting()));

        XYChart.Series<String, Number> expSeries = new XYChart.Series<>();
        Arrays.asList("0-2 ans", "3-5 ans", "5-10 ans", "10+ ans").forEach(range -> {
            long count = expGroups.getOrDefault(range, 0L);
            // On ajoute le chiffre directement dans le nom de la catégorie
            expSeries.getData().add(new XYChart.Data<>(range + " [" + count + "]", count));
        });
        experienceChart.getData().add(expSeries);

        // --- 4. Âge Étudiants par Sexe (StackedBarChart Horizontal) ---
        // Grouping: AgeRange -> Sex -> Count
        Map<String, Map<String, Long>> ageBySex = users.stream()
                .filter(u -> "ROLE_STUDENT".equalsIgnoreCase(u.getRole()) && u.getAge() != null)
                .collect(Collectors.groupingBy(u -> getAgeRange(u.getAge()), 
                        Collectors.groupingBy(u -> u.getSexe() == null ? "Autre" : u.getSexe(), Collectors.counting())));

        XYChart.Series<Number, String> maleSeries = new XYChart.Series<>();
        maleSeries.setName("Hommes");
        XYChart.Series<Number, String> femaleSeries = new XYChart.Series<>();
        femaleSeries.setName("Femmes");

        List<String> ranges = Arrays.asList("15-18", "19-22", "23-26", "26+");
        ranges.forEach(range -> {
            Map<String, Long> sexes = ageBySex.getOrDefault(range, Collections.emptyMap());
            long mCount = sexes.getOrDefault("M", 0L);
            long fCount = sexes.getOrDefault("F", 0L);
            long total = mCount + fCount;
            
            // On inclut le total dans le label de l'axe pour la visibilité
            String axisLabel = range + " (" + total + ")";
            
            XYChart.Data<Number, String> mData = new XYChart.Data<>(mCount, axisLabel);
            XYChart.Data<Number, String> fData = new XYChart.Data<>(fCount, axisLabel);
            
            maleSeries.getData().add(mData);
            femaleSeries.getData().add(fData);
            
            installTooltip(mData, "Hommes : " + mCount);
            installTooltip(fData, "Femmes : " + fCount);
        });
        
        ageChart.getData().addAll(maleSeries, femaleSeries);
        applyGenderColors(maleSeries, femaleSeries);

        // --- 5. Répartition Profs : Pays / Établissement (StackedBarChart) ---
        populateDistribution(profDestChart, users, "ROLE_PROFESSOR");

        // --- 6. Répartition Étudiants : Pays / Établissement (StackedBarChart) ---
        populateDistribution(studentDestChart, users, "ROLE_STUDENT");

        // --- 7. Évolution des Inscriptions (LineChart) ---
        DateTimeFormatter monthYear = DateTimeFormatter.ofPattern("MMM yyyy");
        Map<String, Long> signupTrends = users.stream()
                .filter(u -> u.getDateInscription() != null)
                .sorted(Comparator.comparing(Utilisateur::getDateInscription))
                .collect(Collectors.groupingBy(u -> u.getDateInscription().format(monthYear), 
                        LinkedHashMap::new, Collectors.counting()));

        XYChart.Series<String, Number> lineSeries = new XYChart.Series<>();
        lineSeries.setName("Inscriptions");
        signupTrends.forEach((date, count) -> {
            lineSeries.getData().add(new XYChart.Data<>(date, count));
        });
        signupsChart.getData().add(lineSeries);
    }

    private String getExpRange(int exp) {
        if (exp <= 2) return "0-2 ans";
        if (exp <= 5) return "3-5 ans";
        if (exp <= 10) return "5-10 ans";
        return "10+ ans";
    }

    private String getAgeRange(int age) {
        if (age <= 18) return "15-18";
        if (age <= 22) return "19-22";
        if (age <= 26) return "23-26";
        return "26+";
    }

    private void populateDistribution(StackedBarChart<String, Number> chart, List<Utilisateur> users, String role) {
        // Grouping: Country -> (Establishment -> Count)
        Map<String, Map<String, Long>> dataMap = users.stream()
                .filter(u -> role.equalsIgnoreCase(u.getRole()) && u.getPays() != null && u.getEtablissement() != null)
                .collect(Collectors.groupingBy(Utilisateur::getPays, 
                        Collectors.groupingBy(Utilisateur::getEtablissement, Collectors.counting())));

        // Unique Establishments for series
        Set<String> establishments = users.stream()
                .filter(u -> role.equalsIgnoreCase(u.getRole()) && u.getEtablissement() != null)
                .map(Utilisateur::getEtablissement)
                .collect(Collectors.toSet());

        establishments.forEach(est -> {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(est);
            dataMap.forEach((country, ests) -> {
                long count = ests.getOrDefault(est, 0L);
                if (count > 0) {
                    XYChart.Data<String, Number> data = new XYChart.Data<>(country, count);
                    series.getData().add(data);
                    installTooltip(data, est + " : " + count);
                }
            });
            if (!series.getData().isEmpty()) {
                chart.getData().add(series);
            }
        });
    }

    private void installTooltip(XYChart.Data<?, ?> data, String message) {
        data.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode instanceof javafx.scene.layout.StackPane bar) {
                // Add Tooltip
                Tooltip tooltip = new Tooltip(message);
                tooltip.setShowDelay(javafx.util.Duration.ZERO);
                Tooltip.install(bar, tooltip);
                
                // Add explicit number label inside the bar
                String value = String.valueOf(data.getXValue() instanceof Number ? data.getXValue() : data.getYValue());
                Label label = new Label(value);
                label.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");
                bar.getChildren().add(label);
                
                Platform.runLater(() -> {
                    bar.setStyle(bar.getStyle() + "; -fx-cursor: hand;");
                });
            }
        });
    }

    private void applyGenderColors(XYChart.Series<Number, String> male, XYChart.Series<Number, String> female) {
        Platform.runLater(() -> {
            male.getData().forEach(d -> {
                if (d.getNode() != null) d.getNode().setStyle("-fx-bar-fill: #3498db;"); // Blue
            });
            female.getData().forEach(d -> {
                if (d.getNode() != null) d.getNode().setStyle("-fx-bar-fill: #e84393;"); // Pink
            });
            // Update Legend
            for (javafx.scene.Node node : ageChart.lookupAll(".chart-legend-item")) {
                if (node instanceof Label label) {
                    if ("Hommes".equals(label.getText())) {
                        label.getGraphic().setStyle("-fx-background-color: #3498db;");
                    } else if ("Femmes".equals(label.getText())) {
                        label.getGraphic().setStyle("-fx-background-color: #e84393;");
                    }
                }
            }
        });
    }

    private boolean isActif(Utilisateur u) {
        return u.getStatut() == null || u.getStatut().isEmpty() || "actif".equalsIgnoreCase(u.getStatut());
    }
}
