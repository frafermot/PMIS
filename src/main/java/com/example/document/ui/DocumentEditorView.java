package com.example.document.ui;

import com.example.base.ui.MainLayout;
import com.example.document.Document;
import com.example.document.DocumentService;
import com.example.document.DocumentStatus;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.example.security.SecurityService;
import com.example.user.User;
import com.example.user.UserService;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "document/:doc", layout = MainLayout.class)
@PageTitle("Editor de Documento")
@RolesAllowed({ "ADMIN", "MANAGER", "USER" })
public class DocumentEditorView extends VerticalLayout implements BeforeEnterObserver {

    private final DocumentService documentService;
    private final SecurityService securityService;
    private final UserService userService;
    private Document currentDocument;

    private final NumberField ratingField = new NumberField("Valoración");
    private final CKEditorField editor = new CKEditorField();
    private final Button saveButton = new Button("Guardar");

    public DocumentEditorView(DocumentService documentService, SecurityService securityService,
            UserService userService) {
        this.documentService = documentService;
        this.securityService = securityService;
        this.userService = userService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        editor.setSizeFull();

        ratingField.setMin(0);
        ratingField.setMax(10);
        ratingField.setStep(0.01);
        ratingField.setPlaceholder("Por concretar");
        ratingField.setWidth("160px");

        saveButton.addClickListener(e -> saveDocument());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button backButton = new Button("Volver al Proyecto", new Icon(VaadinIcon.ARROW_LEFT));
        backButton.addClickListener(e -> {
            if (currentDocument != null && currentDocument.getProject() != null) {
                UI.getCurrent().navigate("proyecto/" + currentDocument.getProject().getId());
            } else {
                UI.getCurrent().navigate("");
            }
        });

        HorizontalLayout topBar = new HorizontalLayout(backButton);
        topBar.setWidthFull();

        HorizontalLayout footer = new HorizontalLayout(saveButton);
        footer.setWidthFull();
        footer.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.END);
        footer.getStyle().set("padding-top", "16px");

        add(topBar, editor, footer);
        expand(editor);
    }

    private void loadDocument() {
        // Title shown as heading (read-only, derived from type)
        String heading = currentDocument.getType() != null
                ? currentDocument.getType().getLabel()
                : currentDocument.getTitle();

        // Insert heading above editor
        getChildren()
                .filter(c -> c instanceof H3)
                .findFirst()
                .ifPresent(this::remove);
        H3 titleHeading = new H3(heading);
        addComponentAtIndex(1, titleHeading);

        // Rating
        boolean canRate = false;
        if (currentDocument.getProject() != null && currentDocument.getProject().getProgram() != null) {
            canRate = securityService.isProgramDirector(currentDocument.getProject().getProgram().getId());
        }
        ratingField.setValue(currentDocument.getRating());
        ratingField.setReadOnly(!canRate);
        ratingField.setVisible(canRate || currentDocument.getRating() != null);

        editor.setValue(currentDocument.getContent() != null ? currentDocument.getContent() : "");

        // Determine edit permission
        boolean canEdit = false;
        User currentUser = securityService.getCurrentUser();
        if (currentUser != null) {
            User fullUser = userService.findByUvusWithProject(currentUser.getUvus());
            if (fullUser != null && currentDocument.getProject() != null) {
                canEdit = currentDocument.getProject().equals(fullUser.getProject());
            } else if (currentDocument.getProject() == null) {
                canEdit = true;
            }
        }

        // Cannot edit if POR_CREAR (should not normally happen — editor is opened only
        // after Crear)
        if (currentDocument.getStatus() == DocumentStatus.POR_CREAR) {
            canEdit = false;
        }

        editor.setReadOnly(!canEdit);
        saveButton.setVisible(canEdit || canRate);
    }

    private void saveDocument() {
        try {
            currentDocument.setContent(editor.getValue());
            currentDocument.setRating(ratingField.getValue());
            currentDocument = documentService.createOrUpdate(currentDocument);
            Notification.show("Documento guardado");
        } catch (Exception e) {
            Notification.show(
                    "Error al guardar el documento: " + e.getMessage(),
                    5000,
                    Notification.Position.MIDDLE);
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String param = event.getRouteParameters().get("doc").orElse(null);

        if (param == null) {
            event.rerouteToError(NotFoundException.class);
            return;
        }

        Long id = extractId(param);
        if (id == null) {
            event.rerouteToError(NotFoundException.class);
            return;
        }

        currentDocument = documentService.get(id);
        if (currentDocument == null) {
            event.rerouteToError(NotFoundException.class);
            return;
        }
        loadDocument();
    }

    private Long extractId(String param) {
        try {
            return Long.parseLong(param.split("-")[0]);
        } catch (Exception e) {
            try {
                return Long.parseLong(param);
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
