package com.example.examplefeature.ui;

import com.example.base.ui.MainLayout;
import com.example.communication.Communication;
import com.example.communication.CommunicationService;
import com.example.communication.Message;
import com.example.communication.MessageService;
import com.example.security.SecurityService;
import com.example.user.User;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;

@Route(value = "ccc/communication", layout = MainLayout.class)
@PageTitle("CCC - Mensajes")
@RolesAllowed({ "USER", "MANAGER", "ADMIN" })
public class CommunicationMessagesView extends VerticalLayout implements HasUrlParameter<Long> {

    private final CommunicationService communicationService;
    private final MessageService messageService;
    private final SecurityService securityService;

    private Communication currentCommunication;
    private VerticalLayout messageList;
    private Scroller messageScroller;

    public CommunicationMessagesView(CommunicationService communicationService, MessageService messageService,
            SecurityService securityService) {
        this.communicationService = communicationService;
        this.messageService = messageService;
        this.securityService = securityService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }

    @Override
    public void setParameter(BeforeEvent event, Long communicationId) {
        Optional<Communication> communicationOptional = communicationService.findById(communicationId);

        if (communicationOptional.isEmpty()) {
            Notification.show("Comunicación no encontrada");
            UI.getCurrent().navigate("mis-proyectos");
            return;
        }

        currentCommunication = communicationOptional.get();
        // Access check could be added here (isDirectorOrSponsor)

        removeAll();
        buildView();
    }

    private void buildView() {
        // Breadcrumb
        HorizontalLayout breadcrumb = new HorizontalLayout();
        breadcrumb.setSpacing(false);
        breadcrumb.getStyle().set("font-size", "0.9em");

        Long projectId = currentCommunication.getCcc().getProject().getId();
        Long cccId = currentCommunication.getCcc().getId();

        Button backToProjectButton = new Button(currentCommunication.getCcc().getProject().getName(), e -> {
            UI.getCurrent().navigate("proyecto-ccc/" + projectId);
        });
        backToProjectButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

        Button backToCccButton = new Button("CCC", e -> {
            UI.getCurrent().navigate("ccc/" + cccId);
        });
        backToCccButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

        breadcrumb.add(new Span("Mis Proyectos > "));
        breadcrumb.add(backToProjectButton);
        breadcrumb.add(new Span(" > "));
        breadcrumb.add(backToCccButton);
        breadcrumb.add(new Span(" > " + currentCommunication.getSubject()));

        add(breadcrumb);

        add(new H3(currentCommunication.getSubject()));

        // Message List
        messageList = new VerticalLayout();
        messageList.setPadding(false);
        messageList.setSpacing(true);

        messageScroller = new Scroller(messageList);
        messageScroller.setSizeFull();
        messageScroller.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        messageScroller.getStyle().set("border-radius", "var(--lumo-border-radius-m)");

        add(messageScroller);
        expand(messageScroller);

        // Input Area
        HorizontalLayout inputLayout = new HorizontalLayout();
        inputLayout.setWidthFull();
        inputLayout.setAlignItems(Alignment.END); // Align button to bottom

        TextArea messageInput = new TextArea();
        messageInput.setPlaceholder("Escribe un mensaje...");
        messageInput.setWidthFull();
        messageInput.setHeight("150px");
        messageInput.setMaxLength(2000);
        messageInput.setHelperText("Máximo 2000 caracteres");

        Button sendButton = new Button("Enviar", e -> sendMessage(messageInput));
        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        // Ctrl+Enter to send
        sendButton.addClickShortcut(com.vaadin.flow.component.Key.ENTER, KeyModifier.CONTROL);

        inputLayout.add(messageInput, sendButton);
        add(inputLayout);

        // Load Messages
        refreshMessages();
    }

    private void sendMessage(TextArea input) {
        String content = input.getValue();
        if (content == null || content.trim().isEmpty()) {
            return;
        }

        User currentUser = securityService.getCurrentUser();
        try {
            messageService.sendMessage(currentCommunication.getId(), currentUser, content);
            input.clear();
            refreshMessages();
            // Scroll to bottom
            UI.getCurrent().getPage().executeJs("var scroller = $0; scroller.scrollTo(0, scroller.scrollHeight);",
                    messageScroller.getElement());
        } catch (Exception e) {
            Notification.show("Error al enviar mensaje: " + e.getMessage());
        }
    }

    private void refreshMessages() {
        messageList.removeAll();
        List<Message> messages = messageService.getAllByCommunication(currentCommunication.getId());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (Message msg : messages) {
            Div messageContainer = new Div();
            messageContainer.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
            messageContainer.getStyle().set("padding", "var(--lumo-space-s)");
            messageContainer.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
            messageContainer.setWidthFull();

            Span senderName = new Span(msg.getSender().getName() + " (" + msg.getSender().getRole().name() + ")");
            senderName.getStyle().set("font-weight", "bold");
            senderName.getStyle().set("font-size", "0.8em");

            Span timestamp = new Span(msg.getSentAt().format(formatter));
            timestamp.getStyle().set("color", "var(--lumo-secondary-text-color)");
            timestamp.getStyle().set("font-size", "0.8em");
            timestamp.getStyle().set("margin-left", "10px");

            Div header = new Div(senderName, timestamp);

            Div content = new Div();
            content.setText(msg.getContent());

            messageContainer.add(header, content);
            messageList.add(messageContainer);
        }

        // Scroll to bottom after loading
        UI.getCurrent().getPage().executeJs(
                "var scroller = $0; setTimeout(function() { scroller.scrollTo(0, scroller.scrollHeight); }, 100);",
                messageScroller.getElement());
    }
}
