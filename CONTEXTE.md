# Contexte Projet — StudySprint

## Vue d'ensemble
**StudySprint** est une application JavaFX de gestion de quiz et flashcards pour un projet académique PIDEV à ESPRIT (2025-2026).

**Technologie :** JavaFX + JDBC + MariaDB (XAMPP)  
**Langage :** Java 17+  
**Build :** Maven (`mvnw.cmd javafx:run`)

---

## Architecture

### Structure des modules
```
src/main/java/com/example/studysprint/
├── modules/quizz/
│   ├── models/          # Entités métier (Quiz, Flashcard, QuizAttempt, etc.)
│   ├── services/        # Logique JDBC (QuizService, FlashcardService)
│   ├── controllers/      # Contrôleurs JavaFX (FrontController, BackController)
├── utils/
│   └── MyDatabase.java  # Singleton thread-safe pour la connexion DB
├── MainApp.java         # Point d'entrée JavaFX
└── Launcher.java        # Lanceur alternatif
```

### Design patterns utilisés
- **Singleton** : `MyDatabase.getInstance()` (thread-safe avec `synchronized`)
- **MVC** : Séparation Models / Services / Controllers
- **JDBC raw** : Requêtes SQL manuelles avec PreparedStatement (prévention SQL injection)

---

## Modèles de données

### Entités principales
| Entité | Table | ID | Champs clés |
|--------|-------|--|----|
| **Quiz** | `quizzes` | `long` | `ownerId`, `subjectId`, `chapterId`, `difficulty` (Enum), `questions` (JSON), `published` |
| **FlashcardDeck** | `flashcard_decks` | `long` | `ownerId`, `subjectId`, `chapterId`, `cardCount` |
| **Flashcard** | `flashcards` | `long` | `deckId`, `front`, `back`, `hint`, `position` |
| **QuizAttempt** | `quiz_attempts` | `long` | `userId`, `quizId`, `score`, `correctCount`, `totalQuestions` |
| **QuizRating** | `quiz_ratings` | `long` | `userId`, `quizId`, `score` (1–5, validé) |
| **Difficulty** | Enum (Java) | — | `EASY`, `MEDIUM`, `HARD` |

### Caractéristiques des IDs
- **Type** : `long` (64 bits, évite overflow sur `AUTO_INCREMENT`)
- **Stockage DB** : `BIGINT` (migré d'INT)
- **Contraintes** : FK bien définie pour intégrité référentielle

---

## Interface utilisateur

### Paradigme
- **Pas de TableView** (interdit par académie) → Construites avec **VBox + cartes personnalisées**
- **Cartes interactives** : Hover effects, sélection, suppression inline
- **Recherche et filtres** : TextField pour recherche temps réel
- **FXML** : Layouts structurés en FXML, logique de rendu en Java

### Écrans principaux
1. **QuizFront** : Liste des quiz publiés (recherche, filtre difficulté, rating moyen)
2. **QuizBack** : Gestion des quiz (créer, modifier, supprimer, publier)
3. **FlashcardFront** : Liste des decks publiés (recherche, cartes interactives)
4. **FlashcardBack** : Gestion des decks et cartes (CRUD complet)

---

## Base de données

### Schéma
```sql
-- Identifiants (BIGINT, AUTO_INCREMENT)
CREATE TABLE quizzes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    subject_id BIGINT,
    chapter_id BIGINT,
    title VARCHAR(255),
    description TEXT,
    difficulty ENUM('EASY', 'MEDIUM', 'HARD'),
    questions LONGTEXT,  -- JSON
    published BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(id),
    FOREIGN KEY (subject_id) REFERENCES subjects(id),
    FOREIGN KEY (chapter_id) REFERENCES chapters(id)
);

-- Flashcards
CREATE TABLE flashcard_decks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    subject_id BIGINT,
    chapter_id BIGINT,
    title VARCHAR(255),
    -- ...
);

CREATE TABLE flashcards (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    deck_id BIGINT NOT NULL,
    front TEXT,
    back TEXT,
    hint TEXT,
    position INT,
    created_at TIMESTAMP,
    FOREIGN KEY (deck_id) REFERENCES flashcard_decks(id)
);

-- Tentatives et ratings
CREATE TABLE quiz_attempts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    quiz_id BIGINT NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    score DECIMAL(5,2),
    correct_count INT,
    total_questions INT,
    duration_seconds INT,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (quiz_id) REFERENCES quizzes(id)
);

CREATE TABLE quiz_ratings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    quiz_id BIGINT NOT NULL,
    score INT (CHECK 1-5),
    created_at TIMESTAMP,
    UNIQUE(user_id, quiz_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (quiz_id) REFERENCES quizzes(id)
);
```

### Connexion DB
- **Driver** : MariaDB JDBC
- **URL** : `jdbc:mariadb://localhost:3306/studysprint`
  - **User/Pass** : `root` / `""` (vide, X/mAMPP default)
- **Connection pooling** : Singleton `MyDatabase` avec `isValid(1)` check

---

## Optimisations appliquées

| # | Optimisation | Impact |
|---|-------------|--------|
| 1 | IDs `int` → `long` | Évite overflow silencieux sur AUTO_INCREMENT |
| 2 | `String difficulty` → Enum `Difficulty` | Valeurs invalides impossibles |
| 3 | `ObjectMapper` → `static final MAPPER` | Moins d'allocations mémoire |
| 4 | Cache `questionCount` dans Quiz | Moins de parsing JSON répété |
| 5 | Bulk fetch `getAllAverageRatings()` | Élimine N+1 queries sur ratings |
| 6 | Validation score 1–5 dans setter | Règle métier au bon endroit (couche modèle) |
| 7 | `MyDatabase` synchronized + `isValid()` | Thread-safe, reconnexion robuste |

📄 Détails complets : **optimisations.md**

---

## Dépendances Maven clés

```xml
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
    <version>21.0.1</version>
</dependency>
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-fxml</artifactId>
    <version>21.0.1</version>
</dependency>
<dependency>
    <groupId>org.mariadb.jdbc</groupId>
    <artifactId>mariadb-java-client</artifactId>
    <version>3.1.4</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.2</version>
</dependency>
```

---

## Workflows courants

### Lancer l'application
```bash
.\mvnw.cmd javafx:run
```

### Compiler
```bash
.\mvnw.cmd clean compile
```

### Mettre à jour la DB (migration)
```bash
cd C:\xampp\mysql\bin
mysql.exe -u root studysprint < C:\Users\charaf\Desktop\migrate_bigint.sql
```

### Vérifier la structure DB
```sql
DESCRIBE quizzes;
DESCRIBE flashcards;
DESCRIBE quiz_attempts;
```

---

## État actuel

✅ **Fait**
- CRUD complet pour Quiz et Flashcards
- Recherche et filtres
- UI sans TableView (VBox + cartes)
- IDs migrés à `long` (Java + DB BIGINT)
- Enum `Difficulty`
- Validations métier
- Thread-safe DB connection
- Bulk fetch ratings (N+1 résolu)
- 7 optimisations documentées

⏳ **À valider**
- Application fonctionne avec schema BIGINT (à tester après migration DB)
- Performance des requêtes optimisée en production
- Pas d'erreurs JDBC après changement d'ID type

📋 **Non implémenté** (hors scope)
- Authentification utilisateur avancée
- Export/Import de données
- Statistiques détaillées
- Notifications temps réel

---

## Contacts et ressources

- **Projet académique** : ESPRIT PIDEV 2025-2026
- **Fichiers clés** : 
  - `optimisations.md` — détail des optimisations
  - `migrate_bigint.sql` — script migration DB
  - `MEMORY.md` — contexte mémorisé

---

**Dernière mise à jour** : 26 avril 2026  
**État** : DB migrée, code optimisé, prêt pour test intégration
