package com.example.base.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MainErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(MainErrorHandler.class);

    @Bean
    public VaadinServiceInitListener errorHandlerInitializer() {
        return (event) -> event.getSource().addSessionInitListener(
                sessionInitEvent -> sessionInitEvent.getSession().setErrorHandler(errorEvent -> {
                    Throwable t = errorEvent.getThrowable();
                    log.error("An unexpected error occurred", t);
                    errorEvent.getComponent().flatMap(Component::getUI).ifPresent(ui -> {
                        String errorMessage = getPersonalizedMessage(t);
                        var notification = new Notification("Error: " + errorMessage);
                        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                        notification.setPosition(Notification.Position.TOP_CENTER);
                        notification.setDuration(5000);
                        ui.access(notification::open);
                    });
                }));
    }

    public static String getPersonalizedMessage(Throwable t) {
        if (t instanceof SecurityException || t instanceof org.springframework.security.access.AccessDeniedException) {
            return "Error de seguridad: " + (t.getMessage() != null && !t.getMessage().isEmpty() ? t.getMessage() : "No tiene permisos para realizar esta acción.");
        }
        if (t instanceof IllegalArgumentException) {
            return "Datos inválidos: " + (t.getMessage() != null && !t.getMessage().isEmpty() ? t.getMessage() : "Por favor, revise la información introducida.");
        }
        if (t instanceof IllegalStateException) {
            return "Operación no permitida: " + (t.getMessage() != null && !t.getMessage().isEmpty() ? t.getMessage() : "El estado actual no permite esta acción.");
        }
        if (t instanceof org.springframework.dao.DataIntegrityViolationException) {
            return "Error de integridad de datos: No se puede procesar la solicitud porque existen datos relacionados o duplicados.";
        }
        if (t instanceof org.springframework.orm.ObjectOptimisticLockingFailureException) {
            return "Conflicto de edición: El documento o registro ha sido modificado por otro usuario. Por favor, recargue e inténtelo de nuevo.";
        }
        if (t instanceof NullPointerException) {
            return "Ha ocurrido un error inesperado (Referencia nula). Por favor, contacte con soporte técnico o revise los datos.";
        }
        if (t instanceof java.io.IOException) {
            return "Error de lectura/escritura: " + (t.getMessage() != null && !t.getMessage().isEmpty() ? t.getMessage() : "No se pudo acceder al archivo o recurso.");
        }
        return (t.getMessage() != null && !t.getMessage().isEmpty()) ? t.getMessage() : "Ha ocurrido un error inesperado del tipo " + t.getClass().getSimpleName();
    }
}
