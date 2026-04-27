package com.example.studysprint.modules.quizz.models;

public enum Difficulty {
    EASY, MEDIUM, HARD;

    public static Difficulty fromString(String s) {
        if (s == null) return null;
        try { return valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }
}
