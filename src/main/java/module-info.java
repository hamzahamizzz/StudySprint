module com.example.studysprint {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires jdk.jsobject;
    requires java.sql;
    requires java.desktop;
    requires java.net.http;
    requires jdk.httpserver;
    requires com.google.gson;
    requires jbcrypt;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires java.mail;
    requires webcam.capture;

    // Google Sign-In
    requires google.api.client;
    requires com.google.api.client;
    requires com.google.api.services.oauth2;
    requires com.google.api.client.auth;
    requires com.google.api.client.json.gson;
    requires com.google.api.client.extensions.java6.auth;
    requires com.google.api.client.extensions.jetty.auth;

    opens com.example.studysprint to javafx.fxml;
    opens com.example.studysprint.modules.utilisateurs.models to javafx.base;
    opens com.example.studysprint.modules.utilisateurs.controllers to javafx.fxml;
    opens com.example.studysprint.modules.auth.controllers to javafx.fxml;
    opens com.example.studysprint.utils to javafx.fxml;

    exports com.example.studysprint;
    exports com.example.studysprint.modules.utilisateurs.models;
    exports com.example.studysprint.modules.utilisateurs.controllers;
    exports com.example.studysprint.modules.auth.controllers;
}