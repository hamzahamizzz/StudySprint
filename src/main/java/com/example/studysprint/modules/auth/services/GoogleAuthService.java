package com.example.studysprint.modules.auth.services;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfo;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class GoogleAuthService {

    private static final String APPLICATION_NAME = "StudySprint";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Arrays.asList(
            "openid",
            "https://www.googleapis.com/auth/userinfo.email",
            "https://www.googleapis.com/auth/userinfo.profile"
    );

    private static String CLIENT_ID;
    private static String CLIENT_SECRET;

    static {
        Properties props = new Properties();
        try (InputStream is = GoogleAuthService.class.getResourceAsStream("/google-secrets.properties")) {
            if (is != null) {
                props.load(is);
                CLIENT_ID = props.getProperty("google.client.id");
                CLIENT_SECRET = props.getProperty("google.client.secret");
                System.out.println("Google secrets loaded successfully.");
            } else {
                System.err.println("Google secrets file not found!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Userinfo authenticate() throws IOException, GeneralSecurityException {
        final HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        GoogleClientSecrets.Details web = new GoogleClientSecrets.Details();
        web.setClientId(CLIENT_ID);
        web.setClientSecret(CLIENT_SECRET);
        GoogleClientSecrets clientSecrets = new GoogleClientSecrets();
        clientSecrets.setInstalled(web);

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setAccessType("offline")
                .build();

        // On laisse Google choisir le port libre ou on utilise celui configuré
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        
        AuthorizationCodeInstalledApp authApp = new AuthorizationCodeInstalledApp(flow, receiver);
        Credential credential = authApp.authorize("user");

        Oauth2 oauth2 = new Oauth2.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        return oauth2.userinfo().get().execute();
    }
}
