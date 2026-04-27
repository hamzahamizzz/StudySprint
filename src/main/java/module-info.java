module com.example.studysprint {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;
    requires jakarta.persistence;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires org.kordamp.bootstrapfx.core;

    opens com.example.studysprint to javafx.fxml;
    opens com.example.studysprint.modules.groupes.controllers to javafx.fxml;
    opens com.example.studysprint.modules.matieres.controllers to javafx.fxml;
    exports com.example.studysprint;
    exports com.example.studysprint.modules.groupes.controllers;
    exports com.example.studysprint.modules.groupes.models;
    exports com.example.studysprint.modules.groupes.services;

    exports com.example.studysprint.modules.matieres.controllers;
    exports com.example.studysprint.modules.matieres.models;
    exports com.example.studysprint.modules.matieres.services;

}