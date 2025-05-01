package com.iyg16260.farmasterrae.utils;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;


public class EmailUtils {
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int SMTP_PORT = 587;

    private static final Dotenv dotenv = Dotenv.load();
    private static final String EMAIL = dotenv.get("SMTP_USERNAME");
    private static final String PASSWORD = dotenv.get("SMTP_PASSWORD");

    /**
     * Envia un correo de recuperación de contraseña
     *
     * @param toEmail     Correo de destino
     * @param newPassword Nueva contraseña
     */
    public static void sendPasswordRecoveryEmail(String toEmail, String newPassword) throws MessagingException {

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        Authenticator auth = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL, PASSWORD);
            }
        };

        Session session = Session.getInstance(props, auth);
        var message = new MimeMessage(session);
        var internetAddress = new InternetAddress(toEmail);

        message.setFrom(internetAddress);
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject("Recuperación de Contraseña - InmoGestor");

        String emailContent = "Estimado usuario,\n\n"
                + "Su contraseña ha sido restablecida. Su nueva contraseña es: " + newPassword;

        message.setText(emailContent);

        Transport.send(message);

        System.out.println("Correo enviado satisfactoriamente a " + toEmail);


    }
}
