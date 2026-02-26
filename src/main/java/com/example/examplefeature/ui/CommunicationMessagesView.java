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
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.example.communication.CommunicationStatus;
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
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;

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
        setPadding(false);
        setSpacing(false);
        // Force the view to fit within the viewport minus header/footer approximations
        // This ensures the flex layout (with fixed bottom input) works as expected
        // without body scrolling
        setHeight("calc(100vh - 120px)");
        getStyle().set("overflow", "hidden");
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
        addClassName("chat-view"); // Good practice to add a class to the view itself

        // Ensure relative positioning for absolute children
        getStyle().set("position", "relative");

        // 1. Header (Top, Fixed height)
        VerticalLayout headerLayout = new VerticalLayout();
        headerLayout.setPadding(true);
        headerLayout.setSpacing(true);
        headerLayout.setWidthFull();
        headerLayout.setFlexGrow(0, headerLayout); // Don't grow

        // Breadcrumb
        HorizontalLayout breadcrumb = new HorizontalLayout();
        breadcrumb.setSpacing(false);
        breadcrumb.getStyle().set("font-size", "0.9em");

        Long projectId = currentCommunication.getCcc().getProject().getId();
        Long cccId = currentCommunication.getCcc().getId();

        Button backToProjectButton = new Button(currentCommunication.getCcc().getProject().getName(), e -> {
            UI.getCurrent().navigate("proyecto/" + projectId);
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

        headerLayout.add(breadcrumb);

        // Subject + Status (Right aligned)
        HorizontalLayout subjectLayout = new HorizontalLayout();
        subjectLayout.setWidthFull();
        subjectLayout.setAlignItems(Alignment.CENTER);
        subjectLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);

        H3 subject = new H3(currentCommunication.getSubject());
        subject.getStyle().set("margin", "0");
        subjectLayout.add(subject);

        // Right side info (Type + Status)
        VerticalLayout rightInfoLayout = new VerticalLayout();
        rightInfoLayout.setSpacing(false);
        rightInfoLayout.setPadding(false);
        rightInfoLayout.setAlignItems(Alignment.END);
        rightInfoLayout.setWidth("auto"); // Don't take full width

        // Communication Type
        Span typeSpan = new Span(currentCommunication.getType().getLabel());
        typeSpan.getStyle().set("font-size", "0.9em");
        typeSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");
        rightInfoLayout.add(typeSpan);

        // Check if current user is Sponsor
        if (securityService.isProjectSponsor(projectId)) {
            Select<CommunicationStatus> statusSelect = new Select<>();
            statusSelect.setItems(CommunicationStatus.values());
            statusSelect.setValue(currentCommunication.getStatus());
            // statusSelect.setLabel("Estado"); // Optional label
            statusSelect.setWidth("150px");
            statusSelect.addValueChangeListener(event -> {
                if (event.isFromClient()) {
                    try {
                        communicationService.updateStatus(currentCommunication.getId(), event.getValue());
                        currentCommunication.setStatus(event.getValue()); // Update local state
                        Notification.show("Estado actualizado a " + event.getValue())
                                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    } catch (Exception e) {
                        statusSelect.setValue(event.getOldValue()); // Revert
                        Notification.show("Error al actualizar estado: " + e.getMessage())
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                }
            });
            rightInfoLayout.add(statusSelect);
        } else {
            // For non-sponsors, show the status as text
            Span statusSpan = new Span(currentCommunication.getStatus().toString()); // Enum toString should strictly be
                                                                                     // used if localized, but currently
                                                                                     // Status doesn't have localized
                                                                                     // toString?
            // Wait, CommunicationStatus DOES NOT have localized toString yet (I only did it
            // for CommunicationType).
            // I should probably fix that or just use name(). The previous request was for
            // "solicitud de cambio", "incidencia", "reunion".
            // Status translation was not requested yet, but "Abierto", "En Progreso" etc
            // are in comments in the file.
            // I'll just use name() or toString() for now. CommunicationStatus.java showed
            // comments like // Abierto.
            // I'll stick to displaying it.
            statusSpan.getStyle().set("font-weight", "bold");
            rightInfoLayout.add(statusSpan);
        }

        subjectLayout.add(rightInfoLayout);
        headerLayout.add(subjectLayout);
        add(headerLayout);

        // 2. Message List Container (Middle, Grows, Scrollable)
        messageList = new VerticalLayout();
        // messageList.addClassName("chat-container"); // Moved to Scroller
        messageList.setPadding(true);
        messageList.setSpacing(true);
        messageList.setWidthFull();
        // Remove min-height/height constraints here, handled by Scroller/Parent
        // behavior
        // Add padding at bottom so last messages aren't hidden behind fixed footer
        messageList.getStyle().set("padding-bottom", "100px");

        messageScroller = new Scroller(messageList);
        messageScroller.addClassName("chat-container"); // Apply background here to cover full height
        messageScroller.setSizeFull(); // Takes all available space
        messageScroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
        messageScroller.getStyle().set("border-top", "1px solid var(--lumo-contrast-10pct)");
        // messageScroller.getStyle().set("border-bottom", "1px solid
        // var(--lumo-contrast-10pct)"); // Handled by footer overlap visual

        add(messageScroller);
        expand(messageScroller); // This is crucial: makes scroller take all remaining vertical space

        // 3. Input Area (Bottom, Fixed height)
        HorizontalLayout inputLayout = new HorizontalLayout();
        inputLayout.setWidthFull();
        inputLayout.setPadding(true);
        inputLayout.setAlignItems(Alignment.END);
        inputLayout.getStyle().set("background-color", "var(--lumo-base-color)");
        inputLayout.getStyle().set("z-index", "20"); // Ensure it stays on top/visible
        inputLayout.getStyle().set("position", "absolute");
        inputLayout.getStyle().set("bottom", "0");
        inputLayout.getStyle().set("left", "0");
        inputLayout.getStyle().set("right", "0");
        inputLayout.getStyle().set("box-shadow", "0 -1px 4px rgba(0,0,0,0.1)");
        // inputLayout.setMinHeight("auto"); // Let it size based on content

        TextArea messageInput = new TextArea();
        messageInput.setPlaceholder("Escribe un mensaje");
        messageInput.setWidthFull();
        messageInput.setMaxHeight("150px");
        messageInput.setMinHeight("40px"); // Reasonable default
        messageInput.setMaxLength(2000);

        Button sendButton = new Button("Enviar", e -> sendMessage(messageInput));
        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
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

        User currentUser = securityService.getCurrentUser();

        for (Message msg : messages) {
            boolean isMe = msg.getSender().getId().equals(currentUser.getId());

            Div messageContainer = new Div();
            messageContainer.addClassName("chat-bubble");
            messageContainer.addClassName(isMe ? "me" : "other");

            // Header: Name + Date
            Div header = new Div();
            header.addClassName("chat-bubble-header");

            Span senderName = new Span(msg.getSender().getName());
            senderName.addClassName("chat-sender-name");

            Span timestamp = new Span(msg.getSentAt().format(formatter));
            timestamp.addClassName("chat-timestamp");

            header.add(senderName, timestamp);

            // Content
            Span content = new Span(msg.getContent());
            content.getStyle().set("white-space", "pre-wrap");
            content.getStyle().set("word-break", "break-word");
            content.getStyle().set("display", "block"); // Ensure it takes new line

            messageContainer.add(header, content);

            messageList.add(messageContainer);
        }

        // Scroll to bottom after loading
        UI.getCurrent().getPage().executeJs(
                "var scroller = $0; setTimeout(function() { scroller.scrollTo(0, scroller.scrollHeight); }, 100);",
                messageScroller.getElement());
    }
}
