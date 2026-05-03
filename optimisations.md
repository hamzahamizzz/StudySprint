c# Optimisations du code — StudySprint

## 1. IDs `int` → `long` dans tous les modèles

**Fichiers touchés :** `Quiz.java`, `FlashcardDeck.java`, `Flashcard.java`, `QuizAttempt.java`, `QuizRating.java`, + services et contrôleurs

**Problème :**
`int` est limité à 2 147 483 647. Les colonnes `AUTO_INCREMENT` en base MariaDB sont de type `BIGINT` (64 bits). Si la table grandit ou si la base est partagée entre modules, un id peut dépasser la limite et provoquer un **silent overflow** — l'id devient négatif sans exception ni erreur visible.

**Correction :**
Tous les champs `id`, `ownerId`, `subjectId`, `chapterId`, `userId`, `quizId`, `deckId` → `long` / `Long`.
Dans les services : `rs.getInt("id")` → `rs.getLong("id")`, `ps.setInt` → `ps.setLong` pour tous les paramètres d'identifiants.

---

## 2. Enum `Difficulty` au lieu de `String` libre

**Fichiers touchés :** `Difficulty.java` (nouveau), `Quiz.java`, `QuizService.java`, `QuizBackController.java`, `QuizFrontController.java`

**Problème :**
`private String difficulty` n'impose aucune contrainte. Rien n'empêche de stocker `"SUPER_HARD"` ou `""` en base. La validation n'existait que dans le ComboBox du contrôleur — la couche modèle et service acceptaient n'importe quelle valeur.

**Correction :**
```java
public enum Difficulty { EASY, MEDIUM, HARD }
```
Avec un helper `Difficulty.fromString(String)` pour désérialiser depuis la DB.
En base on continue de stocker le nom textuel (`EASY`, etc.) via `.name()` / `fromString()`.

---

## 3. `ObjectMapper` en `static final`

**Fichiers touchés :** `QuizBackController.java`, `QuizFrontController.java`

**Problème :**
`ObjectMapper` est coûteux à instancier (charge des modules, caches internes). Le déclarer comme champ d'instance le recrée à chaque instanciation du contrôleur.

**Correction :**
```java
// Avant
private final ObjectMapper mapper = new ObjectMapper();

// Après
private static final ObjectMapper MAPPER = new ObjectMapper();
```
Une seule instance partagée par toutes les instances du contrôleur.

---

## 4. Cache du nombre de questions dans `Quiz`

**Fichier touché :** `Quiz.java`, `QuizBackController.java`, `QuizFrontController.java`

**Problème :**
À chaque rendu de la liste (rafraîchissement, filtre), le JSON des questions de chaque quiz était re-parsé pour compter les questions. Du CPU inutile répété.

**Correction :**
```java
private transient int questionCount = -1;

public int getQuestionCount() {
    if (questionCount >= 0) return questionCount;
    // ... parse une seule fois et met en cache
}

public void setQuestions(String questions) {
    this.questions = questions;
    this.questionCount = -1; // reset cache si les questions changent
}
```
`transient` : le champ n'est pas sérialisé (correct car c'est un cache calculé).

---

## 5. Suppression des N+1 queries sur les ratings

**Fichiers touchés :** `QuizService.java`, `QuizFrontController.java`

**Problème :**
Dans `buildQuizCard()`, pour chaque quiz affiché, une requête SQL était exécutée :
```java
service.getAverageRatingOrNull(q.getId()); // 1 requête par quiz
```
Avec 20 quiz → 20 requêtes supplémentaires à chaque affichage. C'est le problème classique **N+1 queries**.

**Correction :**
Nouvelle méthode dans `QuizService` :
```java
public Map<Long, Double> getAllAverageRatings() throws SQLException {
    // SELECT quiz_id, AVG(score) FROM quiz_ratings GROUP BY quiz_id
    // → 1 seule requête pour tous les quiz
}
```
Dans le contrôleur, on charge le cache une fois lors du `loadPublishedQuizzes()` et on lit dedans lors du rendu :
```java
Double avg = ratingsCache.get(q.getId()); // lecture en O(1) dans une Map
```

---

## 6. Validation du score dans `QuizRating`

**Fichier touché :** `QuizRating.java`

**Problème :**
`private int score` acceptait n'importe quelle valeur. La contrainte 1–5 n'était imposée que par le Slider dans le contrôleur. La couche modèle était aveugle à cette règle métier.

**Correction :**
```java
public void setScore(int score) {
    if (score < 1 || score > 5)
        throw new IllegalArgumentException("Score doit être entre 1 et 5");
    this.score = score;
}
```

---

## 7. `MyDatabase` thread-safe + `isValid()`

**Fichier touché :** `MyDatabase.java`

**Problème 1 — Thread safety :**
`getInstance()` et `getConnection()` n'étaient pas `synchronized`. En environnement multi-thread, deux threads pouvaient créer simultanément deux instances ou deux connexions.

**Problème 2 — Connexion silencieusement cassée :**
`connection.isClosed()` ne détecte pas les connexions mortes (timeout réseau, redémarrage DB). Une connexion peut retourner `false` à `isClosed()` mais échouer à la prochaine requête.

**Correction :**
```java
public static synchronized MyDatabase getInstance() { ... }

public synchronized Connection getConnection() throws SQLException {
    if (connection == null || connection.isClosed() || !connection.isValid(1)) {
        connection = DriverManager.getConnection(URL, USER, PASSWORD);
    }
    return connection;
}
```
`isValid(1)` : envoie un ping léger à la DB avec timeout 1 seconde pour vérifier que la connexion est réellement utilisable.

---

## Récapitulatif

| # | Optimisation | Catégorie | Impact |
|---|-------------|-----------|--------|
| 1 | IDs `int` → `long` | Correction de bug | Silent overflow évité |
| 2 | `String difficulty` → enum `Difficulty` | Robustesse | Valeurs invalides impossibles |
| 3 | `ObjectMapper` static | Performance | Moins d'allocations mémoire |
| 4 | Cache `questionCount` | Performance | Moins de parsing JSON |
| 5 | N+1 queries → 1 requête bulk | Performance | O(n) requêtes → O(1) |
| 6 | Validation score 1–5 dans le modèle | Robustesse | Règle métier au bon endroit |
| 7 | `synchronized` + `isValid()` | Fiabilité | Thread-safe, reconnexion robuste |
