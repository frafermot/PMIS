package com.example.document.ui;

import com.example.base.ui.MainLayout;
import com.example.document.Document;
import com.example.document.DocumentService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "document/:doc", layout = MainLayout.class)
@PageTitle("Editor de Documento")
@RolesAllowed({ "ADMIN", "MANAGER", "USER" })
public class DocumentEditorView extends VerticalLayout implements BeforeEnterObserver{

    private final DocumentService documentService;
    private Document currentDocument;

    private final TextField titleField = new TextField();
    private final QuillEditor editor = new QuillEditor();
    private final Button saveButton = new Button("Guardar");

    public DocumentEditorView(DocumentService documentService) {
        this.documentService = documentService;

        
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        editor.setSizeFull();
        titleField.setWidthFull();
        titleField.setPlaceholder("Título del documento");
        saveButton.addClickListener(e -> saveDocument());

        add(titleField, editor, new HorizontalLayout(saveButton));
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
                : "Documento"
        );

        editor.setValue(
                currentDocument.getContent() != null
                        ? currentDocument.getContent()
                        : ""
        );
    }

    private void saveDocument() {
        try {
            currentDocument.setTitle(titleField.getValue());
            currentDocument.setContent(editor.getValue());
            currentDocument =documentService.createOrUpdate(currentDocument);
            String newUrl = documentService.buildUrl(currentDocument);
            UI.getCurrent().navigate(newUrl);
            Notification.show("Documento guardado");
        } catch (Exception e) {
            Notification.show(
                    "Error al guardar el documento: " + e.getMessage(),
                    5000,
                    Notification.Position.MIDDLE
            );
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {

        String param = event.getRouteParameters().get("doc").orElse(null);

        if (param== null) {
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
