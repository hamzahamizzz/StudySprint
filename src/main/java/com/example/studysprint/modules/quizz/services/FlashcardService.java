package com.example.studysprint.modules.quizz.services;

import com.example.studysprint.modules.quizz.models.Flashcard;
import com.example.studysprint.modules.quizz.models.FlashcardDeck;
import com.example.studysprint.utils.MyDatabase;

import com.example.studysprint.utils.SM2Algorithm.SM2Result;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FlashcardService {

    private Connection conn() throws SQLException {
        return MyDatabase.getInstance().getConnection();
    }

    // ═══════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════

    /** Returns the first existing subject_id, or -1 if subjects table is empty. */
    public long getFirstSubjectId() throws SQLException {
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery("SELECT id FROM subjects ORDER BY id LIMIT 1")) {
            if (rs.next()) return rs.getLong(1);
        }
        return -1;
    }

    /** Returns the first existing user id, or -1 if users table is empty. */
    public long getFirstUserId() throws SQLException {
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery("SELECT id FROM users ORDER BY id LIMIT 1")) {
            if (rs.next()) return rs.getLong(1);
        }
        return -1;
    }

    // ═══════════════════════════════════════════
    // FLASHCARD DECK CRUD
    // ═══════════════════════════════════════════

    public void addDeck(FlashcardDeck deck) throws SQLException {
        String sql = "INSERT INTO flashcard_decks (owner_id, subject_id, chapter_id, title, cards, " +
                     "is_published, generated_by_ai, template_key, ai_meta) VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, deck.getOwnerId());
            ps.setLong(2, deck.getSubjectId());
            if (deck.getChapterId() != null) ps.setLong(3, deck.getChapterId()); else ps.setNull(3, Types.BIGINT);
            ps.setString(4, deck.getTitle());
            ps.setString(5, deck.getCards());
            ps.setBoolean(6, deck.isPublished());
            ps.setBoolean(7, deck.isGeneratedByAi());
            ps.setString(8, deck.getTemplateKey());
            ps.setString(9, deck.getAiMeta());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) deck.setId(rs.getLong(1));
            }
        }
    }

    public void updateDeck(FlashcardDeck deck) throws SQLException {
        String sql = "UPDATE flashcard_decks SET subject_id=?, chapter_id=?, title=?, cards=?, " +
                     "is_published=?, generated_by_ai=?, template_key=?, ai_meta=? WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, deck.getSubjectId());
            if (deck.getChapterId() != null) ps.setLong(2, deck.getChapterId()); else ps.setNull(2, Types.BIGINT);
            ps.setString(3, deck.getTitle());
            ps.setString(4, deck.getCards());
            ps.setBoolean(5, deck.isPublished());
            ps.setBoolean(6, deck.isGeneratedByAi());
            ps.setString(7, deck.getTemplateKey());
            ps.setString(8, deck.getAiMeta());
            ps.setLong(9, deck.getId());
            ps.executeUpdate();
        }
    }

    public void deleteDeck(long id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM flashcards WHERE deck_id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM flashcard_decks WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public FlashcardDeck getDeckById(long id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("SELECT * FROM flashcard_decks WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapDeck(rs);
            }
        }
        return null;
    }

    public List<FlashcardDeck> getAllDecks() throws SQLException {
        List<FlashcardDeck> list = new ArrayList<>();
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM flashcard_decks ORDER BY id DESC")) {
            while (rs.next()) list.add(mapDeck(rs));
        }
        return list;
    }

    public List<FlashcardDeck> getDecksByOwner(long ownerId) throws SQLException {
        List<FlashcardDeck> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM flashcard_decks WHERE owner_id=? ORDER BY id DESC")) {
            ps.setLong(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapDeck(rs));
            }
        }
        return list;
    }

    public List<FlashcardDeck> getPublishedDecks() throws SQLException {
        List<FlashcardDeck> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM flashcard_decks WHERE is_published=1 ORDER BY id DESC")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapDeck(rs));
            }
        }
        return list;
    }

    private FlashcardDeck mapDeck(ResultSet rs) throws SQLException {
        FlashcardDeck d = new FlashcardDeck();
        d.setId(rs.getLong("id"));
        d.setOwnerId(rs.getLong("owner_id"));
        d.setSubjectId(rs.getLong("subject_id"));
        long chap = rs.getLong("chapter_id");
        d.setChapterId(rs.wasNull() ? null : chap);
        d.setTitle(rs.getString("title"));
        d.setCards(rs.getString("cards"));
        d.setPublished(rs.getBoolean("is_published"));
        d.setGeneratedByAi(rs.getBoolean("generated_by_ai"));
        d.setTemplateKey(rs.getString("template_key"));
        d.setAiMeta(rs.getString("ai_meta"));
        return d;
    }

    // ═══════════════════════════════════════════
    // FLASHCARD CRUD
    // ═══════════════════════════════════════════

    public void addFlashcard(Flashcard card) throws SQLException {
        String sql = "INSERT INTO flashcards (deck_id, front, back, hint, position) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, card.getDeckId());
            ps.setString(2, card.getFront());
            ps.setString(3, card.getBack());
            if (card.getHint() != null) ps.setString(4, card.getHint()); else ps.setNull(4, Types.VARCHAR);
            ps.setInt(5, card.getPosition());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) card.setId(rs.getLong(1));
            }
        }
    }

    public void updateFlashcard(Flashcard card) throws SQLException {
        String sql = "UPDATE flashcards SET front=?, back=?, hint=?, position=? WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, card.getFront());
            ps.setString(2, card.getBack());
            if (card.getHint() != null) ps.setString(3, card.getHint()); else ps.setNull(3, Types.VARCHAR);
            ps.setInt(4, card.getPosition());
            ps.setLong(5, card.getId());
            ps.executeUpdate();
        }
    }

    public void deleteFlashcard(long id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM flashcards WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public List<Flashcard> getFlashcardsByDeck(long deckId) throws SQLException {
        List<Flashcard> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM flashcards WHERE deck_id=? ORDER BY position")) {
            ps.setLong(1, deckId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapCard(rs));
            }
        }
        return list;
    }

    /** Update only the position field — used by drag & drop reorder. */
    public void updatePosition(long id, int newPosition) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "UPDATE flashcards SET position=? WHERE id=?")) {
            ps.setInt(1, newPosition);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public int countFlashcardsByDeck(long deckId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT COUNT(*) FROM flashcards WHERE deck_id=?")) {
            ps.setLong(1, deckId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    private Flashcard mapCard(ResultSet rs) throws SQLException {
        Flashcard c = new Flashcard();
        c.setId(rs.getLong("id"));
        c.setDeckId(rs.getLong("deck_id"));
        c.setFront(rs.getString("front"));
        c.setBack(rs.getString("back"));
        c.setHint(rs.getString("hint"));
        c.setPosition(rs.getInt("position"));
        c.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        // SM-2 fields — tolerate missing columns if migration not yet run
        try { c.setEaseFactor(rs.getFloat("ease_factor")); } catch (SQLException ignored) {}
        try { c.setRepetitions(rs.getInt("repetitions")); } catch (SQLException ignored) {}
        try { c.setIntervalDays(rs.getInt("interval_days")); } catch (SQLException ignored) {}
        try {
            java.sql.Date d = rs.getDate("next_review");
            if (d != null) c.setNextReview(d.toLocalDate());
        } catch (SQLException ignored) {}
        return c;
    }

    // ═══════════════════════════════════════════
    // SM-2 SPACED REPETITION
    // ═══════════════════════════════════════════

    public void updateSM2(long id, SM2Result r) throws SQLException {
        String sql = "UPDATE flashcards SET ease_factor=?, interval_days=?, repetitions=?, next_review=? WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setFloat(1, r.newEaseFactor());
            ps.setInt(2, r.newIntervalDays());
            ps.setInt(3, r.newRepetitions());
            ps.setDate(4, java.sql.Date.valueOf(r.nextReview()));
            ps.setLong(5, id);
            ps.executeUpdate();
        }
    }

    public List<Flashcard> getDueFlashcards(long deckId) throws SQLException {
        List<Flashcard> list = new ArrayList<>();
        String sql = "SELECT * FROM flashcards WHERE deck_id=? AND next_review<=CURDATE() ORDER BY next_review ASC";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, deckId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapCard(rs));
            }
        }
        return list;
    }

    public int getDueCount(long deckId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM flashcards WHERE deck_id=? AND next_review<=CURDATE()";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, deckId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public LocalDate getNextSessionDate(long deckId) throws SQLException {
        String sql = "SELECT MIN(next_review) FROM flashcards WHERE deck_id=? AND next_review>CURDATE()";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, deckId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.sql.Date d = rs.getDate(1);
                    return d != null ? d.toLocalDate() : null;
                }
            }
        }
        return null;
    }
}
