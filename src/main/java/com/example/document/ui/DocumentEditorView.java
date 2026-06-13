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
import com.example.document.DocumentVersion;
import com.vaadin.flow.component.combobox.ComboBox;
import java.util.List;
import java.time.format.DateTimeFormatter;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;

import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Patch;
import java.util.LinkedList;

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

    private final ComboBox<DocumentVersion> versionSelector = new ComboBox<>();
    private final Button restoreVersionButton = new Button("Restaurar versión", new Icon(VaadinIcon.RECYCLE));

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

        restoreVersionButton.addClickListener(e -> restoreSelectedVersion());
        restoreVersionButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        restoreVersionButton.setVisible(false);

        versionSelector.setPlaceholder("Seleccionar versión...");
        versionSelector.setWidth("300px");
        versionSelector.setClearButtonVisible(false);
        versionSelector.setItemLabelGenerator(v -> {
            if (v.getId() != null && v.getId() == -1L) {
                return "★ Versión Actual en Edición";
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String date = v.getCreatedAt() != null ? v.getCreatedAt().format(formatter) : "";
            String author = v.getCreatedBy() != null ? v.getCreatedBy().getName() : "Sistema";
            return date + " - " + author;
        });
        versionSelector.addValueChangeListener(e -> {
            DocumentVersion selectedVersion = e.getValue();
            if (selectedVersion != null && selectedVersion.getId() != -1L) {
                editor.setValue(selectedVersion.getContent() != null ? selectedVersion.getContent() : "");
                editor.setReadOnly(true);
                saveButton.setVisible(false);
                if (canEditContent) {
                    restoreVersionButton.setVisible(true);
                }
            } else {
                editor.setValue(
                        currentDocument != null && currentDocument.getContent() != null ? currentDocument.getContent()
                                : "");
                editor.setReadOnly(!canEditContent);
                saveButton.setVisible(canEditContent);
                restoreVersionButton.setVisible(false);
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

        HorizontalLayout topCenter = new HorizontalLayout(versionSelector, restoreVersionButton);
        topCenter.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        topCenter.setSpacing(true);

        HorizontalLayout topBar = new HorizontalLayout(backButton, topCenter, ratingGroup);
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
        boolean canRate = securityService.isAdminOrManager();

        boolean canEdit = false;
        User currentUser = securityService.getCurrentUser();
        if (currentUser != null && !canRate) { // sponsor cannot edit content
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
        if (currentDocument.getStatus() == DocumentStatus.FIRMADO) {
            if (currentDocument.getRating() == null || currentDocument.getRating() >= 4.0) {
                canEdit = false;
            }
        }
        this.canEditContent = canEdit;

        List<DocumentVersion> versions = documentService.getVersions(currentDocument.getId());

        List<DocumentVersion> allVersions = new java.util.ArrayList<>();
        DocumentVersion currentDummy = new DocumentVersion();
        currentDummy.setId(-1L);
        allVersions.add(currentDummy);
        allVersions.addAll(versions);

        versionSelector.setItems(allVersions);
        versionSelector.setVisible(!versions.isEmpty());
        versionSelector.setValue(currentDummy);

        // ── Editor ───────────────────────────────────────────────────────────
        initialContent = currentDocument.getContent() != null ? currentDocument.getContent() : "";
        editor.setValue(initialContent);
        this.canEditContent = canEdit;
        editor.setReadOnly(!canEdit);

        // ── Rating field + save button ─────────────────────────────────────
        ratingField.setValue(currentDocument.getRating());
        if (canRate) {
            // Sponsor: field editable, dedicated save button shown
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
            loadDocument(); // Refresh versions
        } catch (ObjectOptimisticLockingFailureException e) {
            handleOptimisticLockingFailure();
        } catch (Exception e) {
            if (e.getCause() instanceof ObjectOptimisticLockingFailureException ||
                    (e.getCause() != null
                            && e.getCause().getCause() instanceof ObjectOptimisticLockingFailureException)) {
                handleOptimisticLockingFailure();
            } else {
                Notification.show("Error al guardar el documento: " + com.example.base.ui.MainErrorHandler.getPersonalizedMessage(e),
                        5000, Notification.Position.MIDDLE);
            }
        }
    }

    private void handleOptimisticLockingFailure() {
        Document latestFromDb = documentService.get(currentDocument.getId());
        
        String baseContent = initialContent != null ? initialContent : "";
        String theirContent = latestFromDb.getContent() != null ? latestFromDb.getContent() : "";
        String myContent = editor.getValue() != null ? editor.getValue() : "";

        // Normalizamos el HTML para evitar que el algoritmo se enfrente a conflictos de indentación:
        // Las plantillas originales tienen saltos de línea y espacios, mientras que CKEditor lo devuelve minificado.
        // Unificamos el formato a minificado para los tres antes de matemáticamente compararlos.
        final String baseContentNorm = baseContent.replaceAll(">\\s+<", "><").replaceAll("\\r?\\n", "");
        final String theirContentNorm = theirContent.replaceAll(">\\s+<", "><").replaceAll("\\r?\\n", "");
        final String myContentNorm = myContent.replaceAll(">\\s+<", "><").replaceAll("\\r?\\n", "");

        diff_match_patch dmp = new diff_match_patch();
        LinkedList<Patch> patches = dmp.patch_make(baseContentNorm, myContentNorm);
        Object[] results = dmp.patch_apply(patches, theirContentNorm);
        String mergedText = (String) results[0];
        boolean[] applied = (boolean[]) results[1];

        boolean allApplied = true;
        for (boolean b : applied) {
            if (!b) {
                allApplied = false;
                break;
            }
        }

        if (allApplied) {
            // Fusión limpia
            latestFromDb.setContent(mergedText);
            currentDocument = documentService.createOrUpdate(latestFromDb);
            initialContent = currentDocument.getContent() != null ? currentDocument.getContent() : "";
            saveButton.setVisible(false);
            Notification.show("Fusión automática exitosa. Tus cambios y los de tu compañero se han combinado.",
                    5000, Notification.Position.MIDDLE);
            loadDocument();
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Conflicto de Edición Parcial");
        dialog.add(new Paragraph(
                "Tú y tu compañero habéis intentado reescribir prácticamente a la vez una misma zona del documento."));
        dialog.add(new Paragraph("¿Qué deseas hacer para resolverlo?"));

        Button partialMergeButton = new Button("Aplicar Fusión Parcial (Salvar lo posible)", e -> {
            latestFromDb.setContent(mergedText);
            currentDocument = documentService.createOrUpdate(latestFromDb);
            initialContent = currentDocument.getContent() != null ? currentDocument.getContent() : "";
            saveButton.setVisible(false);
            Notification.show("Documento guardado con fusión parcial.");
            dialog.close();
            loadDocument();
        });
        partialMergeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        Button overwriteButton = new Button("Forzar mis cambios (Pisar todo lo del otro usuario)", e -> {
            latestFromDb.setContent(myContent);
            currentDocument = documentService.createOrUpdate(latestFromDb);
            initialContent = currentDocument.getContent() != null ? currentDocument.getContent() : "";
            saveButton.setVisible(false);
            Notification.show("Documento sobrescrito exitosamente");
            dialog.close();
            loadDocument();
        });
        overwriteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);

        Button reloadButton = new Button("Recargar documento (Aceptar lo de él)", e -> {
            currentDocument = documentService.get(currentDocument.getId());
            loadDocument();
            dialog.close();
            Notification.show("Documento recargado con los cambios del otro usuario");
        });

        Button cancelButton = new Button("Cancelar", e -> dialog.close());

        VerticalLayout layout = new VerticalLayout(partialMergeButton, overwriteButton, reloadButton, cancelButton);
        layout.setSpacing(true);
        dialog.add(layout);
        dialog.open();
    }

    private void restoreSelectedVersion() {
        DocumentVersion selectedVersion = versionSelector.getValue();
        if (selectedVersion != null && currentDocument != null) {
            try {
                currentDocument.setContent(selectedVersion.getContent());
                currentDocument = documentService.createOrUpdate(currentDocument);
                Notification.show("Versión restaurada correctamente.");

                versionSelector.clear();
                loadDocument();
            } catch (Exception ex) {
                Notification.show("Error al restaurar la versión: " + com.example.base.ui.MainErrorHandler.getPersonalizedMessage(ex),
                        5000, Notification.Position.MIDDLE);
            }
        }
    }

    /** Saves only the rating (program director action). */
    private void saveRating() {
        try {
            currentDocument.setRating(ratingField.getValue());
            currentDocument = documentService.createOrUpdate(currentDocument);
            Notification.show("Valoración guardada");
        } catch (Exception e) {
            Notification.show("Error al guardar la valoración: " + com.example.base.ui.MainErrorHandler.getPersonalizedMessage(e),
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
