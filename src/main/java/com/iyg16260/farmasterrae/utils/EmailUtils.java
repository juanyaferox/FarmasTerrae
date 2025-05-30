package com.iyg16260.farmasterrae.utils;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmailUtils {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailUtils(
            JavaMailSender mailSender,
            @Value ("${spring.mail.username}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    /**
     * Envía un correo de recuperación de contraseña usando Spring Email
     *
     * @param toEmail     Correo de destino
     * @param newPassword Nueva contraseña
     */
    public void sendPasswordRecoveryEmail(String toEmail, String newPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, /* multipart */ false, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Recuperación de Contraseña - FarmasTerrae");

            String content = String.format(
                    "Estimado usuario,%n%n" +
                            "Su contraseña ha sido restablecida. Su nueva contraseña es:%n%n%s",
                    newPassword
            );
            helper.setText(content);

            mailSender.send(message);

        } catch (Exception e) {
            // Loguea o reprocesa según tu política de errores
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error enviando correo de recuperación");
        }
    }
}
