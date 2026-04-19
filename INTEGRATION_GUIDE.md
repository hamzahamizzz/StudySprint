# Guide d'Intégration — Module Utilisateurs

> **Auteur** : Module Utilisateurs (Meriem Dimassi)  
> **Version JPA** : Hibernate 6.4.4 / Jakarta Persistence 3.0  
> **Base de données** : MySQL — table `user`

---

## 1. La table que vous devez référencer

```sql
TABLE user (
    id               INT PRIMARY KEY AUTO_INCREMENT,
    nom              VARCHAR(255) NOT NULL,
    prenom           VARCHAR(255) NOT NULL,
    email            VARCHAR(255) NOT NULL UNIQUE,
    mot_de_passe     VARCHAR(255) NOT NULL,
    role             VARCHAR(255) NOT NULL,   -- 'ROLE_ADMIN' | 'ROLE_STUDENT' | 'ROLE_PROFESSOR'
    discr            VARCHAR(50)  NOT NULL,   -- 'admin' | 'student' | 'professor'
    statut           VARCHAR(50)  DEFAULT 'actif',
    date_inscription DATETIME     NOT NULL,
    pays             VARCHAR(255),
    age              INT,
    sexe             VARCHAR(10),
    etablissement    VARCHAR(255),
    niveau           VARCHAR(255),
    specialite       VARCHAR(255),
    niveau_enseignement VARCHAR(255),
    annees_experience   INT,
    telephone        VARCHAR(50),
    face_descriptor  LONGTEXT
)
```

---

## 2. Entité Java à référencer

```java
import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
```

L'entité `Utilisateur` est mappée sur `@Table(name = "user")`.  
Utilisez `user.id` comme clé étrangère dans vos entités.

---

## 3. Cardinalités à implémenter côté VOS entités

### Module `Groupe`

```java
@Entity
@Table(name = "groupe")
public class Groupe {

    // Un groupe est géré par 1 professeur
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professeur_id")           // ← FK vers user.id
    private Utilisateur professeur;

    // Un groupe contient N étudiants
    @ManyToMany
    @JoinTable(
        name = "groupe_etudiant",
        joinColumns = @JoinColumn(name = "groupe_id"),
        inverseJoinColumns = @JoinColumn(name = "etudiant_id") // ← FK vers user.id
    )
    private List<Utilisateur> etudiants;
}
```

### Module `Matiere`

```java
@Entity
@Table(name = "matiere")
public class Matiere {

    // Une matière est enseignée par 1 professeur
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professeur_id")           // ← FK vers user.id
    private Utilisateur professeur;
}
```

### Module `Quizz`

```java
@Entity
@Table(name = "quizz")
public class Quizz {

    // Un quiz est créé par 1 professeur
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "createur_id")             // ← FK vers user.id
    private Utilisateur createur;
}
```

### Module `Objectif`

```java
@Entity
@Table(name = "objectif")
public class Objectif {

    // Un objectif appartient à 1 étudiant
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etudiant_id")             // ← FK vers user.id
    private Utilisateur etudiant;
}
```

---

## 4. Convention de nommage des clés étrangères

| FK Column              | Table source       | Référence     | Rôle attendu    |
|------------------------|--------------------|---------------|-----------------|
| `professeur_id`        | `groupe`           | `user.id`     | ROLE_PROFESSOR  |
| `professeur_id`        | `matiere`          | `user.id`     | ROLE_PROFESSOR  |
| `createur_id`          | `quizz`            | `user.id`     | ROLE_PROFESSOR  |
| `etudiant_id`          | `groupe_etudiant`  | `user.id`     | ROLE_STUDENT    |
| `etudiant_id`          | `objectif`         | `user.id`     | ROLE_STUDENT    |

---

## 5. Enregistrer votre entité dans `persistence.xml`

Ajoutez votre entité dans :  
`src/main/resources/META-INF/persistence.xml`

```xml
<!-- Ajoutez APRÈS la ligne de Utilisateur -->
<class>com.example.studysprint.modules.groupes.models.Groupe</class>
<class>com.example.studysprint.modules.matieres.models.Matiere</class>
<class>com.example.studysprint.modules.quizz.models.Quizz</class>
<class>com.example.studysprint.modules.objectifs.models.Objectif</class>
```

---

## 6. Pattern obligatoire : getAll() + Java Stream

**Toutes les requêtes de lecture** doivent suivre ce pattern :

```java
// ✅ BON — Une seule requête, filtres en Stream
public class GroupeService {

    public List<Groupe> getAll() {
        EntityManager em = JpaUtils.getEntityManager();
        return em.createQuery("SELECT g FROM Groupe g", Groupe.class).getResultList();
    }

    // Filtrage par professeur via Stream
    public List<Groupe> getByProfesseur(int profId) {
        return getAll().stream()
            .filter(g -> g.getProfesseur() != null 
                      && g.getProfesseur().getId() == profId)
            .toList();
    }

    // Recherche par nom via Stream
    public List<Groupe> searchByNom(String query) {
        String q = query.toLowerCase();
        return getAll().stream()
            .filter(g -> g.getNom() != null && g.getNom().toLowerCase().contains(q))
            .toList();
    }
}
```

```java
// ❌ MAUVAIS — Requêtes multiples filtrées en SQL
em.createQuery("SELECT g FROM Groupe g WHERE g.professeur.id = :id")
```

---

## 7. Activer les relations dans Utilisateur.java lors de l'intégration

Ouvrir `Utilisateur.java` et décommenter les sections marquées  
**`// RELATIONS JPA — Cardinalités (activées lors de l'intégration)`**

```java
// Exemple : décommenter ceci pour Groupe
@OneToMany(mappedBy = "professeur", fetch = FetchType.LAZY)
private List<Groupe> groupesGeres;
```

---

## 8. SessionManager — accès à l'utilisateur connecté

Pour récupérer l'utilisateur connecté dans vos contrôleurs :

```java
import com.example.studysprint.utils.SessionManager;

Utilisateur currentUser = SessionManager.getInstance().getCurrentUser();
String role = currentUser.getRole(); // "ROLE_ADMIN" | "ROLE_STUDENT" | "ROLE_PROFESSOR"
int userId  = currentUser.getId();
```

---

*Pour toute question d'intégration, référez-vous à ce guide ou contactez le responsable du module Utilisateurs.*
