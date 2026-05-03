module com.example.studysprint {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

    requires java.sql;
    requires java.net.http;
    requires java.desktop;
    requires java.naming;
    requires com.fasterxml.jackson.databind;

    // iText 5 and JavaMail are non-modular jars — opened via unnamed module
    requires itextpdf;
    requires java.mail;

    opens com.example.studysprint to javafx.fxml;
    exports com.example.studysprint;

    opens com.example.studysprint.modules.quizz.controllers to javafx.fxml;
    exports com.example.studysprint.modules.quizz.controllers;

    exports com.example.studysprint.modules.quizz.models;
    exports com.example.studysprint.modules.quizz.services;
    exports com.example.studysprint.utils;
}
