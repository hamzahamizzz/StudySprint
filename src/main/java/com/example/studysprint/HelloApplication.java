package com.example.studysprint;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.utils.AppNavigator;
import com.example.studysprint.utils.SessionManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Utilisateur currentUser = SessionManager.getInstance().getCurrentUser();
        String fxmlPath = currentUser == null ? AppNavigator.LOGIN_FXML : AppNavigator.defaultFxmlFor(currentUser);
        String title = currentUser == null ? AppNavigator.LOGIN_TITLE : AppNavigator.defaultTitleFor(currentUser);

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource(fxmlPath));
        Parent root = fxmlLoader.load();
        Scene scene = currentUser == null ? new Scene(root, 900, 600) : new Scene(root, 1280, 820);

        stage.setTitle(title);
        stage.setScene(scene);
        stage.show();
    }
}   
