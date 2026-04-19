package com.example.studysprint.utils;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class MailerService {

    private static final String SENDER_EMAIL = "studysprintcontact@gmail.com";
    private static final String APP_PASSWORD = "dalwvjubadbqwhdp";

    public static void sendVerificationCode(String recipientEmail, String code) throws MessagingException {
        Properties prop = new Properties();
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true");
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        Session session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, APP_PASSWORD);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(SENDER_EMAIL));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
        message.setSubject("Réinitialisation de votre mot de passe - StudySprint");

        String htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;'>"
                + "<h2 style='color: #6c5ce7; text-align: center;'>StudySprint</h2>"
                + "<p>Bonjour,</p>"
                + "<p>Vous avez demandé la réinitialisation de votre mot de passe. Voici votre code de vérification :</p>"
                + "<div style='background: #f1f2f6; padding: 15px; text-align: center; font-size: 24px; font-weight: bold; letter-spacing: 5px; color: #2d3436; border-radius: 5px;'>"
                + code
                + "</div>"
                + "<p>Ce code est valable pendant <b>15 minutes</b>. Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet email.</p>"
                + "<br><p>L'équipe StudySprint</p>"
                + "</div>";

        message.setContent(htmlContent, "text/html; charset=utf-8");

        Transport.send(message);
        System.out.println("Email sent successfully to " + recipientEmail);
    }

    public static void sendAccountDeactivationNotice(String recipientEmail, String userName) throws MessagingException {
        Properties prop = new Properties();
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true");
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        Session session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, APP_PASSWORD);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(SENDER_EMAIL));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
        message.setSubject("Suspension de votre compte - StudySprint");

        String htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;'>"
                + "<h2 style='color: #e74c3c; text-align: center;'>StudySprint</h2>"
                + "<p>Bonjour <b>" + userName + "</b>,</p>"
                + "<p>Nous vous informons que votre compte a été temporairement <b>désactivé</b> par un administrateur.</p>"
                + "<p>Si vous pensez qu'il s'agit d'une erreur ou si vous souhaitez demander la réactivation de votre compte, vous pouvez tenter de vous connecter pour soumettre une demande via notre formulaire dédié.</p>"
                + "<p>Vous pouvez également nous contacter à l'adresse support : <a href='mailto:" + SENDER_EMAIL + "'>" + SENDER_EMAIL + "</a></p>"
                + "<br><p>L'équipe StudySprint</p>"
                + "</div>";

        message.setContent(htmlContent, "text/html; charset=utf-8");

        Transport.send(message);
        System.out.println("Deactivation notice sent to " + recipientEmail);
    }
}
