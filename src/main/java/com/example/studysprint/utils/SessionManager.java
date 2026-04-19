package com.example.studysprint.utils;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;

public class SessionManager {
    private static SessionManager instance;
    private Utilisateur currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public Utilisateur getCurrentUser() {
        return currentUser;
    }

    public Integer getCurrentUserId() {
        return currentUser == null ? null : currentUser.getId();
    }

    public void setCurrentUser(Utilisateur currentUser) {
        this.currentUser = currentUser;
    }

    public void logout() {
        this.currentUser = null;
    }
}
