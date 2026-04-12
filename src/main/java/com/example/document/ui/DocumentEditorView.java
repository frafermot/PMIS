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
    private String initialContent = "";
    private boolean canEditContent = false;

    private final NumberField ratingField = new NumberField("Valoración");
    private final Button saveRatingButton = new Button("Guardar Valoración");
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
        ratingField.setPlaceholder("Sin valorar");
        ratingField.setWidth("160px");

        saveRatingButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
        saveRatingButton.addClickListener(e -> saveRating());
        saveRatingButton.setVisible(false); // only visible for program director

        saveButton.addClickListener(e -> saveDocument());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.getStyle().set("position", "fixed");
        saveButton.getStyle().set("bottom", "20px");
        saveButton.getStyle().set("right", "20px");
        saveButton.getStyle().set("z-index", "1000");
        saveButton.setVisible(false);

        editor.addValueChangeListener(html -> {
            if (canEditContent && html != null && !html.equals(initialContent)) {
                saveButton.setVisible(true);
            } else {
                saveButton.setVisible(false);
            }
        });

        Button backButton = new Button("Volver al Proyecto", new Icon(VaadinIcon.ARROW_LEFT));
        backButton.addClickListener(e -> {
            if (currentDocument != null && currentDocument.getProject() != null) {
                UI.getCurrent().navigate("proyecto/" + currentDocument.getProject().getId());
            } else {
                UI.getCurrent().navigate("");
            }
        });

        // Rating group: field + its own save button (shown only to program director)
        HorizontalLayout ratingGroup = new HorizontalLayout(ratingField, saveRatingButton);
        ratingGroup.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.END);
        ratingGroup.setSpacing(true);
        ratingField.setVisible(false); // hidden until loadDocument sets it

        HorizontalLayout topBar = new HorizontalLayout(backButton, ratingGroup);
        topBar.setWidthFull();
        topBar.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        topBar.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);

        HorizontalLayout footer = new HorizontalLayout(saveButton);
        footer.setWidthFull();
        footer.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.END);
        footer.getStyle().set("padding-top", "16px");

        add(topBar, editor, footer);
        expand(editor);
    }

    private void loadDocument() {
        // Title heading (read-only, derived from type)
        String heading = currentDocument.getType() != null
                ? currentDocument.getType().getLabel()
                : currentDocument.getTitle();
        getChildren().filter(c -> c instanceof H3).findFirst().ifPresent(this::remove);
        addComponentAtIndex(1, new H3(heading));

        // ── Permission flags ──────────────────────────────────────────────────
        boolean canRate = false;
        if (currentDocument.getProject() != null && currentDocument.getProject().getProgram() != null) {
            canRate = securityService.isProgramDirector(currentDocument.getProject().getProgram().getId());
        }

        boolean canEdit = false;
        User currentUser = securityService.getCurrentUser();
        if (currentUser != null && !canRate) { // program director cannot edit content
            User fullUser = userService.findByUvusWithProject(currentUser.getUvus());
            if (fullUser != null && currentDocument.getProject() != null) {
                canEdit = currentDocument.getProject().equals(fullUser.getProject());
            } else if (currentDocument.getProject() == null) {
                canEdit = true;
            }
        }
        if (currentDocument.getStatus() == DocumentStatus.POR_CREAR) {
            canEdit = false; // should not happen via normal flow, but guard anyway
        }

        // ── Editor ───────────────────────────────────────────────────────────
        initialContent = currentDocument.getContent() != null ? currentDocument.getContent() : "";
        editor.setValue(initialContent);
        this.canEditContent = canEdit;
        editor.setReadOnly(!canEdit);

        // ── Rating field + save button ─────────────────────────────────────
        ratingField.setValue(currentDocument.getRating());
        if (canRate) {
            // Program director: field editable, dedicated save button shown
            ratingField.setReadOnly(false);
            ratingField.setVisible(true);
            saveRatingButton.setVisible(true);
        } else if (currentDocument.getRating() != null) {
            // Others who can see doc: field read-only, value shown, no save button
            ratingField.setReadOnly(true);
            ratingField.setVisible(true);
            saveRatingButton.setVisible(false);
        } else {
            // No rating yet and user cannot rate: hide both
            ratingField.setVisible(false);
            saveRatingButton.setVisible(false);
        }

        // ── Main save button (content only) ───────────────────────────────
        saveButton.setVisible(false);
    }

    /** Saves only the document content (not the rating). */
    private void saveDocument() {
        try {
            currentDocument.setContent(editor.getValue());
            currentDocument = documentService.createOrUpdate(currentDocument);
            initialContent = currentDocument.getContent() != null ? currentDocument.getContent() : "";
            saveButton.setVisible(false);
            Notification.show("Documento guardado");
        } catch (Exception e) {
            Notification.show("Error al guardar el documento: " + e.getMessage(),
                    5000, Notification.Position.MIDDLE);
        }
    }

    /** Saves only the rating (program director action). */
    private void saveRating() {
        try {
            currentDocument.setRating(ratingField.getValue());
            currentDocument = documentService.createOrUpdate(currentDocument);
            Notification.show("Valoración guardada");
        } catch (Exception e) {
            Notification.show("Error al guardar la valoración: " + e.getMessage(),
                    5000, Notification.Position.MIDDLE);
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
