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

        String htmlContent = "<div style=\"background-color: #f8f9fa; padding: 50px 0; font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;\">"
                + "  <div style=\"max-width: 600px; margin: 0 auto; background: white; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 30px rgba(108, 92, 231, 0.1);\">"
                + "    <div style=\"background: linear-gradient(135deg, #6c5ce7 0%, #a29bfe 100%); padding: 40px 20px; text-align: center;\">"
                + "      <h1 style=\"color: white; margin: 0; font-size: 32px; font-weight: 800; letter-spacing: -1px;\">StudySprint</h1>"
                + "      <p style=\"color: rgba(255,255,255,0.9); margin-top: 10px; font-size: 16px;\">Boostez votre apprentissage avec passion</p>"
                + "    </div>"
                + "    <div style=\"padding: 40px; color: #2d3436; line-height: 1.6;\">"
                + "      <h2 style=\"margin-top: 0; color: #6c5ce7;\">Bonjour,</h2>"
                + "      <p style=\"font-size: 16px;\">Vous avez demandé la réinitialisation de votre mot de passe. Voici votre code de vérification personnel :</p>"
                + "      <div style=\"margin: 30px 0; background: #f1f2f6; border-radius: 12px; padding: 25px; text-align: center;\">"
                + "        <span style=\"font-family: 'Courier New', monospace; font-size: 36px; font-weight: 800; letter-spacing: 12px; color: #6c5ce7; display: block; filter: drop-shadow(0 2px 4px rgba(108, 92, 231, 0.2));\">" + code + "</span>"
                + "      </div>"
                + "      <p style=\"font-size: 14px; color: #636e72;\">Ce code est valable pendant <b>15 minutes</b>. Pour votre sécurité, ne partagez jamais ce code avec personne.</p>"
                + "      <hr style=\"border: none; border-top: 1px solid #eee; margin: 30px 0;\">"
                + "      <p style=\"font-size: 14px; margin-bottom: 5px;\">Besoin d'aide ?</p>"
                + "      <p style=\"font-size: 14px; color: #636e72; margin-top: 0;\">Contactez-nous à <a href=\"mailto:" + SENDER_EMAIL + "\" style=\"color: #6c5ce7; text-decoration: none;\">" + SENDER_EMAIL + "</a></p>"
                + "    </div>"
                + "    <div style=\"background: #f1f2f6; padding: 20px; text-align: center; color: #b2bec3; font-size: 12px;\">"
                + "      &copy; 2026 StudySprint. Tous droits réservés.<br>"
                + "      Développé avec passion pour l'excellence académique."
                + "    </div>"
                + "  </div>"
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

        String htmlContent = "<div style=\"background-color: #f8f9fa; padding: 50px 0; font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;\">"
                + "  <div style=\"max-width: 600px; margin: 0 auto; background: white; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 30px rgba(231, 76, 60, 0.1);\">"
                + "    <div style=\"background: linear-gradient(135deg, #e74c3c 0%, #ff7675 100%); padding: 40px 20px; text-align: center;\">"
                + "      <h1 style=\"color: white; margin: 0; font-size: 32px; font-weight: 800; letter-spacing: -1px;\">StudySprint</h1>"
                + "      <p style=\"color: rgba(255,255,255,0.9); margin-top: 10px; font-size: 16px;\">Information importante sur votre compte</p>"
                + "    </div>"
                + "    <div style=\"padding: 40px; color: #2d3436; line-height: 1.6;\">"
                + "      <h2 style=\"margin-top: 0; color: #e74c3c;\">Bonjour " + userName + ",</h2>"
                + "      <p style=\"font-size: 16px;\">Nous vous informons que votre compte StudySprint a été temporairement <b>suspendu</b> par un administrateur.</p>"
                + "      <div style=\"margin: 30px 0; background: #fff5f5; border-left: 4px solid #e74c3c; padding: 20px; border-radius: 4px;\">"
                + "        <p style=\"margin: 0; font-size: 15px; color: #c0392b;\">Si vous pensez qu'il s'agit d'une erreur ou si vous souhaitez demander la réactivation, vous pouvez vous connecter pour soumettre un formulaire de réclamation.</p>"
                + "      </div>"
                + "      <p style=\"font-size: 14px; color: #636e72;\">Notre équipe de modération reste à votre disposition pour toute précision.</p>"
                + "      <hr style=\"border: none; border-top: 1px solid #eee; margin: 30px 0;\">"
                + "      <p style=\"font-size: 14px; margin-bottom: 5px;\">Besoin de nous contacter ?</p>"
                + "      <p style=\"font-size: 14px; color: #636e72; margin-top: 0;\">Répondez simplement à cet email ou écrivez à <a href=\"mailto:" + SENDER_EMAIL + "\" style=\"color: #e74c3c; text-decoration: none;\">" + SENDER_EMAIL + "</a></p>"
                + "    </div>"
                + "    <div style=\"background: #f1f2f6; padding: 20px; text-align: center; color: #b2bec3; font-size: 12px;\">"
                + "      &copy; 2026 StudySprint. La sécurité est notre priorité."
                + "    </div>"
                + "  </div>"
                + "</div>";

        message.setContent(htmlContent, "text/html; charset=utf-8");

        Transport.send(message);
        System.out.println("Deactivation notice sent to " + recipientEmail);
    }
}
