package com.example.document.ui;

import com.example.base.ui.MainLayout;
import com.example.document.Document;
import com.example.document.DocumentService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
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

    private final TextField titleField = new TextField();
    private final NumberField ratingField = new NumberField("Valoración");
    private final TextField readonlyRatingField = new TextField("Valoración");
    private final QuillEditor editor = new QuillEditor();
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
        titleField.setWidthFull();
        titleField.setPlaceholder("Título del documento");

        ratingField.setMin(0);
        ratingField.setMax(10);
        ratingField.setStep(0.01);
        ratingField.setPlaceholder("Por concretar");
        ratingField.setWidth("160px");

        readonlyRatingField.setValue("Por concretar");
        readonlyRatingField.setReadOnly(true);
        readonlyRatingField.setWidth("160px");
        readonlyRatingField.setVisible(false);

        saveButton.addClickListener(e -> saveDocument());

        HorizontalLayout headerBar = new HorizontalLayout(titleField, ratingField, readonlyRatingField);
        headerBar.setWidthFull();
        headerBar.setFlexGrow(1, titleField);

        HorizontalLayout footer = new HorizontalLayout(saveButton);
        footer.setWidthFull();
        footer.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.END);
        footer.getStyle().set("padding-top", "40px"); // Increased padding to push button down
        footer.getStyle().set("margin-top", "auto"); // Pushes footer to the bottom of the flex layout

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

        add(topBar, headerBar, editor, footer);
        expand(editor);
    }

    private void loadDocument() {
        if (currentDocument == null) {
            currentDocument = new Document();
            currentDocument.setTitle("Nuevo Documento");
            currentDocument = documentService.createOrUpdate(currentDocument);
        }

        titleField.setValue(
                currentDocument.getTitle() != null
                        ? currentDocument.getTitle()
                        : "Documento");

        ratingField.setValue(currentDocument.getRating());

        boolean canRate = false;
        if (currentDocument.getProject() != null && currentDocument.getProject().getProgram() != null) {
            canRate = securityService.isProgramDirector(currentDocument.getProject().getProgram().getId());
        }
        ratingField.setReadOnly(!canRate);

        if (currentDocument.getRating() == null) {
            // Document has no rating
            ratingField.setVisible(false);
            readonlyRatingField.setVisible(true);
            if (canRate) {
                // If they can rate, we still show the text field, but maybe we shouldn't?
                // The prompt says "dentro del documento quiero que para los usuarios tambien
                // vean 'por concretar' si no hay ninguna valoracion".
                // This means ALL users (even those who might rate? But if they can rate, they
                // need the input field to rate it).
                // Actually, if they are the director and want to set it, `ratingField` with
                // placeholder "Por concretar" is best,
                // but NumberField might not show the placeholder properly when empty or maybe
                // the user just wants the explicit text.
                // However, they MUST be able to edit it if canRate.
                // We'll let `ratingField` handle the input if `canRate`, because it has
                // `setPlaceholder("Por concretar")`.
                ratingField.setVisible(true);
                readonlyRatingField.setVisible(false);
            }
        } else {
            // Document has a rating
            ratingField.setVisible(true);
            readonlyRatingField.setVisible(false);
        }

        editor.setValue(
                currentDocument.getContent() != null
                        ? currentDocument.getContent()
                        : "");

        // Determine if user can edit this document
        boolean canEdit = false;
        User currentUser = securityService.getCurrentUser();
        if (currentUser != null) {
            User fullUser = userService.findByUvusWithProject(currentUser.getUvus());
            if (fullUser != null && currentDocument.getProject() != null) {
                // Edit is allowed only if the user is explicitly assigned to this project
                if (currentDocument.getProject().equals(fullUser.getProject())) {
                    canEdit = true;
                }
            } else if (currentDocument.getProject() == null) {
                // New unassigned document (edge case before saving)
                canEdit = true;
            }
        }

        titleField.setReadOnly(!canEdit);
        editor.setReadOnly(!canEdit);
        saveButton.setVisible(canEdit || canRate);
    }

    private void saveDocument() {
        try {
            currentDocument.setTitle(titleField.getValue());
            currentDocument.setContent(editor.getValue());
            currentDocument.setRating(ratingField.getValue());
            currentDocument = documentService.createOrUpdate(currentDocument);
            String newUrl = documentService.buildUrl(currentDocument);
            UI.getCurrent().navigate(newUrl);
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
            String idPart = param.split("-")[0];
            return Long.parseLong(idPart);
        } catch (Exception e) {
            return null;
        }
    }
}
