package com.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false) // Optional so app doesn't crash if unconfigured
    private JavaMailSender mailSender;

    public void sendCredentials(String to, String uvus, String password) {
        String subject = "Credenciales de Acceso - PMIS";
        String text = "Bienvenido a PMIS.\n\n" +
                "Sus credenciales de acceso son:\n" +
                "Usuario (UVUS): " + uvus + "\n" +
                "Contraseña: " + password + "\n\n" +
                "Por favor cambie su contraseña una vez acceda al sistema.";

        try {
            if (mailSender != null) {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(to);
                message.setSubject(subject);
                message.setText(text);
                // message.setFrom("tu_email@gmail.com"); // Configure default sender in
                // properties usually
                mailSender.send(message);
                logger.info("Email sent to user: {}", to);
            } else {
                logger.warn("JavaMailSender not configured. Email not sent to: {}", to);
                logger.info("Content would be:\nSubject: {}\n{}", subject, text);
            }
        } catch (Exception e) {
            logger.error("Failed to send email to {}", to, e);
        }
    }
}
