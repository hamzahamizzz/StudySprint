package com.example.studysprint;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("/fxml/matieres/MatiereListView.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("StudySprint - Groupes");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }
}   
