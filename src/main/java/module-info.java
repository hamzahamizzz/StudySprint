module com.example.studysprint {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires java.net.http;
    requires com.google.gson;
    requires jbcrypt;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

    opens com.example.studysprint to javafx.fxml;
    opens com.example.studysprint.modules.utilisateurs.models to org.hibernate.orm.core, javafx.base;
    opens com.example.studysprint.modules.utilisateurs.controllers to javafx.fxml;
    opens com.example.studysprint.modules.auth.controllers to javafx.fxml;

    exports com.example.studysprint;
    exports com.example.studysprint.modules.utilisateurs.models;
    exports com.example.studysprint.modules.utilisateurs.controllers;
    exports com.example.studysprint.modules.auth.controllers;
}