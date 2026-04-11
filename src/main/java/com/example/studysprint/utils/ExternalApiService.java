package com.example.studysprint.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ExternalApiService {

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();

    /**
     * Fetches all countries with their common name and ISO code.
     */
    public static CompletableFuture<List<Country>> fetchCountries() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://restcountries.com/v3.1/all?fields=name,cca2"))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    JsonArray array = gson.fromJson(response.body(), JsonArray.class);
                    List<Country> countries = new ArrayList<>();
                    array.forEach(jsonElement -> {
                        JsonObject obj = jsonElement.getAsJsonObject();
                        String name = obj.getAsJsonObject("name").get("common").getAsString();
                        String code = obj.get("cca2").getAsString();
                        countries.add(new Country(name, code));
                    });
                    countries.sort((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()));
                    return countries;
                });
    }

    /**
     * Fetches universities for a given country name.
     */
    public static CompletableFuture<List<String>> fetchUniversities(String countryName) {
        String encodedCountry = countryName.replace(" ", "%20");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://universities.hipolabs.com/search?country=" + encodedCountry))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    JsonArray array = gson.fromJson(response.body(), JsonArray.class);
                    List<String> universities = new ArrayList<>();
                    array.forEach(jsonElement -> {
                        JsonObject obj = jsonElement.getAsJsonObject();
                        universities.add(obj.get("name").getAsString());
                    });
                    java.util.Collections.sort(universities);
                    return universities;
                });
    }

    public static CompletableFuture<List<String>> fetchLevels() {
        return CompletableFuture.supplyAsync(() -> {
            try { Thread.sleep(300); } catch (InterruptedException e) {}
            return List.of("Licence 1 (L1)", "Licence 2 (L2)", "Licence 3 (L3)",
                           "Master 1 (M1)", "Master 2 (M2)", "Doctorat", "Ingénierie");
        });
    }

    public static CompletableFuture<List<String>> fetchSpecialites() {
        return CompletableFuture.supplyAsync(() -> {
            try { Thread.sleep(200); } catch (InterruptedException e) {}
            return List.of(
                "Mathématiques", "Physique", "Chimie", "Biologie", "Informatique",
                "Intelligence Artificielle", "Réseaux & Télécommunications", "Cybersécurité",
                "Génie Civil", "Génie Électrique", "Génie Mécanique",
                "Médecine", "Pharmacie", "Sciences Économiques",
                "Droit", "Lettres & Linguistique", "Histoire & Géographie",
                "Philosophie", "Sociologie", "Psychologie",
                "Arts & Design", "Architecture", "Marketing", "Gestion"
            );
        });
    }

    public static CompletableFuture<List<String>> fetchNiveauxEnseignement() {
        return CompletableFuture.supplyAsync(() -> {
            try { Thread.sleep(200); } catch (InterruptedException e) {}
            return List.of(
                "Primaire", "Collège (CEM)", "Lycée / Secondaire",
                "Classes Préparatoires", "Licence (Undergraduate)",
                "Master (Graduate)", "Doctorat (PhD)", "Formation Professionnelle",
                "Enseignement à Distance (E-Learning)"
            );
        });
    }

    public static class Country {
        private final String name;
        private final String code;

        public Country(String name, String code) {
            this.name = name;
            this.code = code;
        }

        public String getName() { return name; }
        public String getCode() { return code; }

        @Override
        public String toString() { return name; }
    }
}
