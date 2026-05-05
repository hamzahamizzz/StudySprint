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
import com.google.api.client.util.store.FileDataStoreFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfo;

public class GoogleAuthService {

    private static final String APPLICATION_NAME = "StudySprint";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    // Scopes required for user profile and email
    private static final List<String> SCOPES = Arrays.asList(
            "openid",
            "https://www.googleapis.com/auth/userinfo.email",
            "https://www.googleapis.com/auth/userinfo.profile"
    );

    private static String CLIENT_ID;
    private static String CLIENT_SECRET;

    static {
        java.util.Properties props = new java.util.Properties();
        try (java.io.InputStream is = GoogleAuthService.class.getResourceAsStream("/google-secrets.txt")) {
            if (is != null) {
                props.load(is);
                String rawId = props.getProperty("google.client.id");
                String rawSecret = props.getProperty("google.client.secret");
                
                if (rawId != null) CLIENT_ID = new String(java.util.Base64.getDecoder().decode(rawId));
                if (rawSecret != null) CLIENT_SECRET = new String(java.util.Base64.getDecoder().decode(rawSecret));
            } else {
                System.err.println("Google secrets file not found!");
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    public static Userinfo authenticate() throws IOException, GeneralSecurityException {
        final HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        // Manual construction of client secrets since we don't have the JSON file locally
        GoogleClientSecrets.Details web = new GoogleClientSecrets.Details();
        web.setClientId(CLIENT_ID);
        web.setClientSecret(CLIENT_SECRET);
        GoogleClientSecrets clientSecrets = new GoogleClientSecrets();
        clientSecrets.setInstalled(web);

        // Build flow without a persistent data store to force new login every time if desired,
        // and add a prompt to force account selection.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().build(); // Use a random free port
        
        // Custom authorization app to inject the "prompt" parameter
        AuthorizationCodeInstalledApp authApp = new AuthorizationCodeInstalledApp(flow, receiver) {
            @Override
            protected void onAuthorization(com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl authorizationUrl) {
                try {
                    authorizationUrl.set("prompt", "select_account");
                    super.onAuthorization(authorizationUrl);
                } catch (java.io.IOException e) {
                    throw new RuntimeException("Échec de l'ouverture du navigateur pour l'authentification Google", e);
                }
            }
        };
        
        Credential credential = authApp.authorize("user");

        // Use the credential to get user info
        Oauth2 oauth2 = new Oauth2.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        return oauth2.userinfo().get().execute();
    }
}
