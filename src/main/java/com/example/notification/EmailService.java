package com.example.notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class EmailService {

    private final JavaMailSender emailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${GMAIL_USERNAME:}")
    private String gmailUsername;

    @Value("${GMAIL_PASSWORD:}")
    private String gmailPassword;

    @Autowired
    public EmailService(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void sendPasswordRecoveryEmail(String to, String resetLink) {
        to = to != null ? to.trim() : "";
        String safeFromEmail = fromEmail != null ? fromEmail.replace("'", "").replace("\"", "").trim() : "";

        System.out.println("DEBUG - Preparando correo principal.");
        System.out.println("DEBUG - TO: '" + to + "'");
        System.out.println("DEBUG - FROM: '" + safeFromEmail + "'");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(safeFromEmail);
        message.setTo(to);
        message.setSubject("Recuperación de contraseña");
        message.setText("Has solicitado restablecer tu contraseña.\n\n" +
                "Haz clic en el siguiente enlace para crear una nueva contraseña:\n" +
                resetLink + "\n\n" +
                "Si no has solicitado esto, puedes ignorar este correo.");
        
        try {
            System.out.println("Intentando enviar correo mediante servidor principal (Office 365)...");
            emailSender.send(message);
            System.out.println("Correo enviado correctamente por el servidor principal.");
        } catch (MailException e) {
            System.err.println("Fallo al enviar correo por Office 365: " + e.getMessage());
            System.out.println("Intentando enviar mediante cuenta de respaldo (Gmail)...");
            sendWithGmailFallback(to, resetLink);
        }
    }

    private void sendWithGmailFallback(String to, String resetLink) {
        if (gmailUsername == null || gmailUsername.isEmpty() || gmailPassword == null || gmailPassword.isEmpty()) {
            System.err.println("No se han configurado credenciales de Gmail de respaldo en el .env (GMAIL_USERNAME / GMAIL_PASSWORD). Imposible enviar.");
            return;
        }

        String safeGmailUsername = gmailUsername.replace("'", "").replace("\"", "").trim();
        String safeGmailPassword = gmailPassword.replace("'", "").replace("\"", "").replace(" ", "").trim();

        System.out.println("DEBUG - Preparando correo de respaldo.");
        System.out.println("DEBUG - TO: '" + to + "'");
        System.out.println("DEBUG - FROM (Gmail): '" + safeGmailUsername + "'");

        JavaMailSenderImpl gmailSender = new JavaMailSenderImpl();
        gmailSender.setHost("smtp.gmail.com");
        gmailSender.setPort(587);
        gmailSender.setUsername(safeGmailUsername);
        gmailSender.setPassword(safeGmailPassword);

        Properties props = gmailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "false");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(safeGmailUsername);
        message.setTo(to);
        message.setSubject("Recuperación de contraseña");
        message.setText("Has solicitado restablecer tu contraseña.\n\n" +
                "Haz clic en el siguiente enlace para crear una nueva contraseña:\n" +
                resetLink + "\n\n" +
                "Si no has solicitado esto, puedes ignorar este correo.");

        try {
            gmailSender.send(message);
            System.out.println("Correo enviado correctamente por cuenta de respaldo (Gmail).");
        } catch (Exception ex) {
            System.err.println("Fallo al enviar correo por Gmail: " + ex.getMessage());
            ex.printStackTrace(); // Print full stack trace for debugging
        }
    }
}
