module com.example.studysprint {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires jdk.jsobject;
    requires java.sql;
    requires java.desktop;
    requires java.net.http;
    requires com.google.gson;
    requires jbcrypt;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires java.mail;
    requires webcam.capture;

    opens com.example.studysprint to javafx.fxml;
    opens com.example.studysprint.modules.utilisateurs.models to javafx.base;
    opens com.example.studysprint.modules.utilisateurs.controllers to javafx.fxml;
    opens com.example.studysprint.modules.auth.controllers to javafx.fxml;

    exports com.example.studysprint;
    exports com.example.studysprint.modules.utilisateurs.models;
    exports com.example.studysprint.modules.utilisateurs.controllers;
    exports com.example.studysprint.modules.auth.controllers;
}