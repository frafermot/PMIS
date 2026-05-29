package com.example.examplefeature.ui;

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
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;

@Route("reset-password")
@PageTitle("Establecer nueva contraseña | PMIS")
@AnonymousAllowed
public class PasswordResetView extends VerticalLayout implements BeforeEnterObserver {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private String token;
    private User targetUser;

    private final H1 title = new H1("Nueva Contraseña");
    private final Paragraph description = new Paragraph("La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial.");
    private final PasswordField passwordField = new PasswordField("Nueva Contraseña");
    private final PasswordField confirmPasswordField = new PasswordField("Confirmar Contraseña");
    private final Button submitButton = new Button("Cambiar contraseña");

    public PasswordResetView(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        description.getStyle().set("text-align", "center");
        description.setWidth("400px");

        passwordField.setWidth("300px");
        confirmPasswordField.setWidth("300px");

        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitButton.setWidth("300px");

        submitButton.addClickListener(e -> resetPassword());

        add(title, description, passwordField, confirmPasswordField, submitButton);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Map<String, List<String>> parameters = event.getLocation().getQueryParameters().getParameters();
        if (parameters.containsKey("token")) {
            token = parameters.get("token").get(0);
            targetUser = userService.findByResetToken(token);
            
            if (targetUser == null) {
                event.forwardTo("login");
                Notification.show("Enlace inválido o expirado. Vuelve a solicitar la recuperación.", 5000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } else {
            event.forwardTo("login");
        }
    }

    private void resetPassword() {
        String pwd1 = passwordField.getValue();
        String pwd2 = confirmPasswordField.getValue();

        if (pwd1 == null || pwd1.isEmpty()) {
            passwordField.setErrorMessage("La contraseña no puede estar vacía");
            passwordField.setInvalid(true);
            return;
        }

        if (!pwd1.equals(pwd2)) {
            confirmPasswordField.setErrorMessage("Las contraseñas no coinciden");
            confirmPasswordField.setInvalid(true);
            return;
        }

        if (!isValidPassword(pwd1)) {
            passwordField.setErrorMessage("No cumple los requisitos de seguridad");
            passwordField.setInvalid(true);
            Notification.show("Mínimo 8 caracteres, 1 mayúscula, 1 minúscula, 1 número y 1 carácter especial.", 5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        if (targetUser != null) {
            String encodedPassword = passwordEncoder.encode(pwd1);
            userService.updatePassword(targetUser, encodedPassword);
            
            Notification success = Notification.show("Contraseña actualizada con éxito. Ya puedes iniciar sesión.", 5000, Notification.Position.TOP_CENTER);
            success.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            UI.getCurrent().navigate("login");
        }
    }

    private boolean isValidPassword(String password) {
        // Al menos 8 caracteres
        if (password.length() < 8) return false;
        // Al menos una minúscula
        if (!password.matches(".*[a-z].*")) return false;
        // Al menos una mayúscula
        if (!password.matches(".*[A-Z].*")) return false;
        // Al menos un número
        if (!password.matches(".*\\d.*")) return false;
        // Al menos un carácter especial
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) return false;

        return true;
    }
}
