package com.example.examplefeature.ui;

import com.example.notification.EmailService;
import com.example.user.User;
import com.example.user.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.util.UUID;

@Route("forgot-password")
@PageTitle("Recuperar Contraseña | PMIS")
@AnonymousAllowed
public class PasswordRecoveryView extends VerticalLayout {

    private final UserService userService;
    private final EmailService emailService;

    public PasswordRecoveryView(UserService userService, EmailService emailService) {
        this.userService = userService;
        this.emailService = emailService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        H1 title = new H1("Recuperar Contraseña");
        Paragraph description = new Paragraph("Introduce tu correo electrónico. Se enviará un enlace de recuperación si existe una cuenta asociada al UVUS.");
        description.getStyle().set("text-align", "center");
        description.setWidth("400px");

        EmailField emailField = new EmailField("Correo Electrónico");
        emailField.setWidth("300px");
        emailField.setErrorMessage("Introduce un correo válido");
        emailField.setClearButtonVisible(true);

        Button submitButton = new Button("Enviar correo");
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitButton.setWidth("300px");
        
        Button backButton = new Button("Volver al Login");
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backButton.addClickListener(e -> UI.getCurrent().navigate("login"));

        submitButton.addClickListener(e -> {
            String email = emailField.getValue();
            if (email != null && email.contains("@") && email.trim().length() > 3) {
                String uvus = email.substring(0, email.indexOf("@")).trim();
                
                try {
                    User user = userService.findByUvus(uvus);
                    if (user != null) {
                        // En un entorno real se generaría y guardaría el token en DB asociado a la cuenta
                        String token = UUID.randomUUID().toString();
                        userService.updateResetToken(user, token);
                        // Asumimos localhost:8080 pero en prod sería el dominio real
                        String resetLink = "http://localhost:8080/reset-password?token=" + token;
                        
                        emailService.sendPasswordRecoveryEmail(email, resetLink);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace(); // Log del error interno (ej. fallo conexión SMTP)
                }
                
                Notification notification = Notification.show("Si el correo pertenece a un usuario registrado, se ha enviado un enlace de recuperación.", 6000, Notification.Position.TOP_CENTER);
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
                UI.getCurrent().navigate("login");
            } else {
                emailField.setInvalid(true);
            }
        });

        add(title, description, emailField, submitButton, backButton);
    }
}
