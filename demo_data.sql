-- ═══════════════════════════════════════════════════════════════
-- CLEANUP
-- ═══════════════════════════════════════════════════════════════
DELETE FROM quiz_ratings    WHERE (user_id,quiz_id) IN ((1,1),(2,1),(2,2),(3,2));
DELETE FROM quiz_attempts   WHERE id IN (1,2,3,4,5,6,7,8,9);
DELETE FROM flashcards      WHERE id IN (1,2,3);
DELETE FROM flashcard_decks WHERE id = 1;
DELETE FROM quizzes         WHERE id IN (1,2,3);

-- ═══════════════════════════════════════════════════════════════
-- QUIZZES
-- ═══════════════════════════════════════════════════════════════
INSERT INTO quizzes (id, owner_id, subject_id, title, difficulty, questions, is_published, generated_by_ai, template_key, ai_meta) VALUES
(1, 1, 1, 'Java POO', 'HARD',
 '[{"question":"Quelle est la différence entre une classe abstraite et une interface en Java ?","options":["Une classe abstraite peut avoir des méthodes concrètes, une interface non (avant Java 8)","Une interface peut avoir un constructeur","Une classe abstraite ne peut pas être héritée","Aucune différence"],"correct":0},{"question":"Que se passe-t-il si on ne redéfinit pas une méthode abstraite dans une sous-classe concrète ?","options":["Erreur à l exécution","Erreur de compilation","La méthode retourne null","La classe hérite de Object"],"correct":1},{"question":"Quel modificateur empêche la surcharge d une méthode dans les sous-classes ?","options":["static","abstract","final","private"],"correct":2}]',
 1, 0, NULL, NULL),

(2, 1, 1, 'Polymorphisme', 'EASY',
 '[{"question":"Le polymorphisme permet de...","options":["Avoir plusieurs constructeurs","Utiliser une référence parent pour appeler des méthodes enfant","Cacher les attributs d une classe","Instancier une interface"],"correct":1},{"question":"Quelle annotation Java indique une surcharge correcte ?","options":["@Override","@Overload","@Extends","@Implements"],"correct":0}]',
 1, 0, NULL, NULL),

(3, 1, 1, 'Structures de données', 'MEDIUM',
 '[{"question":"Quelle structure garantit l ordre d insertion et autorise les doublons ?","options":["HashSet","TreeSet","ArrayList","HashMap"],"correct":2}]',
 1, 0, NULL, NULL);

-- ═══════════════════════════════════════════════════════════════
-- FLASHCARD DECK
-- ═══════════════════════════════════════════════════════════════
INSERT INTO flashcard_decks (id, owner_id, subject_id, title, cards, is_published, generated_by_ai, ai_meta, template_key, chapter_id) VALUES
(1, 1, 1, 'Java POO — Concepts clés', '[]', 1, 0, NULL, NULL, NULL);

-- ═══════════════════════════════════════════════════════════════
-- FLASHCARDS
-- ═══════════════════════════════════════════════════════════════
INSERT INTO flashcards (id, deck_id, front, back, hint, position, ease_factor, interval_days, repetitions, next_review) VALUES
(1, 1, 'Qu est-ce que l encapsulation ?',          'Mécanisme qui restreint l accès direct aux attributs d un objet via des modificateurs d accès.', 'Pense aux getters/setters',      1, 2.5, 1, 0, CURDATE()),
(2, 1, 'Qu est-ce que l héritage en Java ?',       'Mécanisme permettant à une classe fille de réutiliser les attributs et méthodes d une classe mère via extends.', 'Mot-clé : extends', 2, 2.5, 1, 0, CURDATE()),
(3, 1, 'Que signifie le mot-clé "super" ?',        'Référence à la classe parente — appel constructeur (super()) ou méthodes surchargées.', 'Utilisé dans le constructeur fils', 3, 2.5, 6, 1, CURDATE());

-- ═══════════════════════════════════════════════════════════════
-- QUIZ ATTEMPTS
-- ═══════════════════════════════════════════════════════════════
INSERT INTO quiz_attempts (id, user_id, quiz_id, started_at, completed_at, correct_count, total_questions, score, duration_seconds) VALUES
(1, 1, 1, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY + INTERVAL 4 MINUTE, 1, 3, 33.33, 240),
(2, 1, 1, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY + INTERVAL 3 MINUTE, 1, 3, 33.33, 180),
(3, 1, 1, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY + INTERVAL 5 MINUTE, 1, 3, 33.33, 300),
(4, 2, 1, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 5 DAY + INTERVAL 6 MINUTE, 3, 3, 100.0, 360),
(5, 2, 2, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 4 DAY + INTERVAL 3 MINUTE, 2, 2, 100.0, 180),
(6, 2, 3, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY + INTERVAL 2 MINUTE, 1, 1, 100.0, 120),
(7, 3, 2, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 4 DAY + INTERVAL 4 MINUTE, 2, 2, 100.0, 240),
(8, 3, 3, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY + INTERVAL 2 MINUTE, 1, 1, 100.0, 120),
(9, 1, 2, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 6 DAY + INTERVAL 3 MINUTE, 0, 2,   0.0, 180);

-- ═══════════════════════════════════════════════════════════════
-- QUIZ RATINGS
-- ═══════════════════════════════════════════════════════════════
INSERT INTO quiz_ratings (user_id, quiz_id, score) VALUES
(1, 1, 3),
(2, 1, 4),
(2, 2, 5),
(3, 2, 4);

-- ═══════════════════════════════════════════════════════════════
-- VERIFICATION
-- ═══════════════════════════════════════════════════════════════
SELECT 'quizzes'    AS table_name, COUNT(*) AS count FROM quizzes
UNION SELECT 'attempts',           COUNT(*) FROM quiz_attempts
UNION SELECT 'flashcards',         COUNT(*) FROM flashcards;
