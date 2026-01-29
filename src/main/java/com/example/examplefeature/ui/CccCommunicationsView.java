package com.example.examplefeature.ui;

import com.example.base.ui.MainLayout;
import com.example.communication.Ccc;
import com.example.communication.CccService;
import com.example.communication.Communication;
import com.example.communication.CommunicationService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import com.example.security.SecurityService;
import com.example.communication.CommunicationType;
import com.example.user.User;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.textfield.TextField;
import java.util.Optional;

@Route(value = "ccc", layout = MainLayout.class)
@PageTitle("CCC - Comunicaciones")
@RolesAllowed({ "USER", "MANAGER", "ADMIN" })
public class CccCommunicationsView extends VerticalLayout implements HasUrlParameter<Long> {

    private final CccService cccService;
    private final CommunicationService communicationService;
    private final SecurityService securityService;
    private Ccc currentCcc;

    private Grid<Communication> grid = new Grid<>(Communication.class, false);

    public CccCommunicationsView(CccService cccService, CommunicationService communicationService,
            SecurityService securityService) {
        this.cccService = cccService;
        this.communicationService = communicationService;
        this.securityService = securityService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        configureGrid();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(Communication::getStatus).setHeader("Estado").setAutoWidth(true);
        grid.addColumn(Communication::getType).setHeader("Tipo").setAutoWidth(true);
        grid.addColumn(Communication::getSubject).setHeader("Asunto").setAutoWidth(true);
        grid.addColumn(c -> c.getUpdatedAt().toLocalDate()).setHeader("Última Actualización").setAutoWidth(true);

        grid.addItemClickListener(event -> {
            UI.getCurrent().navigate("ccc/communication/" + event.getItem().getId());
        });
    }

    @Override
    public void setParameter(BeforeEvent event, Long cccId) {
        Optional<Ccc> cccOptional = cccService.findById(cccId);

        if (cccOptional.isEmpty()) {
            Notification.show("CCC no encontrado");
            UI.getCurrent().navigate("mis-proyectos");
            return;
        }

        currentCcc = cccOptional.get();

        removeAll();
        buildView();
    }

    private void buildView() {
        // Breadcrumb
        HorizontalLayout breadcrumb = new HorizontalLayout();
        breadcrumb.setSpacing(false);
        breadcrumb.getStyle().set("font-size", "0.9em");

        Button backToProjectButton = new Button(currentCcc.getProject().getName(), e -> {
            UI.getCurrent().navigate("proyecto-ccc/" + currentCcc.getProject().getId());
        });
        backToProjectButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

        breadcrumb.add(new Span("Mis Proyectos > "));
        breadcrumb.add(backToProjectButton);
        breadcrumb.add(new Span(" > Comunicaciones"));

        add(breadcrumb);

        add(new H2("Comunicaciones del Proyecto"));

        // Create Button (Director Only)
        if (securityService.isProjectDirector(currentCcc.getProject().getId())) {
            Button createButton = new Button("Nueva Comunicación", e -> openCreateDialog());
            createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            add(createButton);
        }

        // Refresh grid
        refreshGrid();
        add(grid);
    }

    private void refreshGrid() {
        if (currentCcc != null) {
            grid.setItems(communicationService.getAllByCcc(currentCcc.getId()));
        }
    }

    private void openCreateDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Nueva Comunicación");

        VerticalLayout dialogLayout = new VerticalLayout();
        TextField subjectField = new TextField("Asunto");
        ComboBox<CommunicationType> typeComboBox = new ComboBox<>("Tipo");
        typeComboBox.setItems(CommunicationType.values());
        typeComboBox.setItemLabelGenerator(CommunicationType::name);

        dialogLayout.add(subjectField, typeComboBox);
        dialog.add(dialogLayout);

        Button saveButton = new Button("Crear", e -> {
            if (subjectField.isEmpty() || typeComboBox.isEmpty()) {
                Notification.show("Por favor, rellene todos los campos");
                return;
            }

            try {
                User currentUser = securityService.getCurrentUser();
                communicationService.createCommunication(currentCcc.getId(), subjectField.getValue(),
                        typeComboBox.getValue(), currentUser);
                refreshGrid();
                dialog.close();
                Notification.show("Comunicación creada exitosamente");
            } catch (Exception ex) {
                Notification.show("Error al crear la comunicación: " + ex.getMessage());
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelButton = new Button("Cancelar", e -> dialog.close());

        dialog.getFooter().add(cancelButton);
        dialog.getFooter().add(saveButton);

        dialog.open();
    }
}
