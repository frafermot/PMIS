package com.example.examplefeature.ui;

import java.util.List;

import com.example.base.ui.MainLayout;
import com.example.user.User;
import com.example.user.UserService;
import com.example.user.Role;
import com.example.portfolio.Portfolio;
import com.example.portfolio.PortfolioService;
import com.example.pmo.PMO;
import com.example.pmo.PMOService;
import com.example.resource.Resource;
import com.example.resource.ResourceService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "pmo", layout = MainLayout.class)
@PageTitle("Oficina de Proyectos")
@Menu(order = 2, icon = "vaadin:chart-timeline", title = "Oficina de Proyectos")
@RolesAllowed("ADMIN")
public class PmoView extends VerticalLayout {

    private final PMOService pmoService;
    private final PortfolioService portfolioService;
    private final UserService userService;
    private final ResourceService resourceService;
    private final Grid<PMO> grid = new Grid<>(PMO.class);
    private final Grid<Resource> resourceGrid = new Grid<>(Resource.class);

    public PmoView(PMOService pmoService, PortfolioService portfolioService, UserService userService, ResourceService resourceService) {
        this.pmoService = pmoService;
        this.portfolioService = portfolioService;
        this.userService = userService;
        this.resourceService = resourceService;

        setSizeFull();
        configureGrid();
        configureResourceGrid();

        add(new H2("Vista de Oficina de Dirección de Proyectos (PMO)"), createToolbar(), grid);
        add(new H2("Gestión de Recursos"), createResourceToolbar(), resourceGrid);
        updateList();
        updateResourceList();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();
        grid.addColumn(PMO::getId).setHeader("ID").setWidth("60px").setFlexGrow(0);
        grid.addColumn(PMO::getName).setHeader("Nombre").setFlexGrow(2);
        grid.addColumn(pmo -> pmo.getPortfolio() != null ? pmo.getPortfolio().getName() : "Sin Portafolio")
                .setHeader("Portafolio").setFlexGrow(1);
        grid.addColumn(pmo -> pmo.getDirector() != null ? pmo.getDirector().getName() : "Sin Director")
                .setHeader("Director").setFlexGrow(1);

        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                openPmoDialog(event.getValue());
            }
        });
    }

    private HorizontalLayout createToolbar() {
        Button addPmoButton = new Button("Añadir PMO");
        addPmoButton.addClickListener(e -> openPmoDialog(null));

        return new HorizontalLayout(addPmoButton);
    }

    private void openPmoDialog(PMO pmo) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(pmo == null ? "Nueva PMO" : "Editar PMO");

        TextField nameField = new TextField("Nombre");

        ComboBox<Portfolio> portfolioComboBox = new ComboBox<>("Portafolio");
        portfolioComboBox.setItems(portfolioService.getAll());
        portfolioComboBox.setItemLabelGenerator(Portfolio::getName);

        ComboBox<User> directorComboBox = new ComboBox<>("Director");
        directorComboBox.setItems(userService.findAllByRoles(List.of(Role.MANAGER, Role.ADMIN)));
        directorComboBox.setItemLabelGenerator(User::getName);

        if (pmo != null) {
            nameField.setValue(pmo.getName());
            portfolioComboBox.setValue(pmo.getPortfolio());
            directorComboBox.setValue(pmo.getDirector());
        }

        VerticalLayout dialogLayout = new VerticalLayout(nameField, portfolioComboBox, directorComboBox);
        dialog.add(dialogLayout);

        Button saveButton = new Button("Guardar", e -> {
            if (nameField.isEmpty() || portfolioComboBox.isEmpty() || directorComboBox.isEmpty()) {
                Notification.show("Por favor rellene todos los campos");
                return;
            }

            try {
                PMO pmoToSave = pmo == null ? new PMO() : pmo;
                pmoToSave.setName(nameField.getValue());
                pmoToSave.setPortfolio(portfolioComboBox.getValue());
                pmoToSave.setDirector(directorComboBox.getValue());

                pmoService.createOrUpdate(pmoToSave);
                updateList();
                dialog.close();
                Notification.show("PMO guardada exitosamente");
            } catch (SecurityException ex) {
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });

        Button cancelButton = new Button("Cancelar", e -> dialog.close());

        Button deleteButton = new Button("Eliminar", e -> {
            if (pmo != null) {
                try {
                    pmoService.delete(pmo.getId());
                    updateList();
                    dialog.close();
                    Notification.show("PMO eliminada exitosamente");
                } catch (SecurityException ex) {
                    Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
                }
            }
        });
        deleteButton.getStyle().set("color", "red");

        dialog.getFooter().add(cancelButton);
        if (pmo != null) {
            dialog.getFooter().add(deleteButton);
        }
        dialog.getFooter().add(saveButton);

        dialog.open();

        dialog.addOpenedChangeListener(e -> {
            if (!e.isOpened()) {
                grid.asSingleSelect().clear();
            }
        });
    }

    private void configureResourceGrid() {
        resourceGrid.setSizeFull();
        resourceGrid.removeAllColumns();
        resourceGrid.addColumn(Resource::getId).setHeader("ID").setWidth("60px").setFlexGrow(0);
        resourceGrid.addColumn(Resource::getResourceType).setHeader("Tipo de Recurso").setFlexGrow(1);
        resourceGrid.addColumn(Resource::getProfessionalProfile).setHeader("Perfil Profesional").setFlexGrow(1);
        resourceGrid.addColumn(res -> res.getCostPerHour() != null ? res.getCostPerHour() + " €/h" : "-")
                .setHeader("Coste/Hora").setFlexGrow(1);

        resourceGrid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                openResourceDialog(event.getValue());
            }
        });
    }

    private HorizontalLayout createResourceToolbar() {
        Button addResourceButton = new Button("Añadir Recurso");
        addResourceButton.addClickListener(e -> openResourceDialog(null));
        return new HorizontalLayout(addResourceButton);
    }

    private void openResourceDialog(Resource resource) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(resource == null ? "Nuevo Recurso" : "Editar Recurso");

        TextField typeField = new TextField("Tipo de Recurso");
        TextField profileField = new TextField("Perfil Profesional");
        NumberField costField = new NumberField("Coste (€/h)");
        costField.setSuffixComponent(new Span("€/h"));

        if (resource != null) {
            typeField.setValue(resource.getResourceType() != null ? resource.getResourceType() : "");
            profileField.setValue(resource.getProfessionalProfile() != null ? resource.getProfessionalProfile() : "");
            costField.setValue(resource.getCostPerHour());
        }

        VerticalLayout dialogLayout = new VerticalLayout(typeField, profileField, costField);
        dialog.add(dialogLayout);

        Button saveButton = new Button("Guardar", e -> {
            if (typeField.isEmpty() || profileField.isEmpty()) {
                Notification.show("Por favor rellene todos los campos");
                return;
            }

            Resource resToSave = resource == null ? new Resource() : resource;
            resToSave.setResourceType(typeField.getValue());
            resToSave.setProfessionalProfile(profileField.getValue());
            resToSave.setCostPerHour(costField.getValue());

            resourceService.save(resToSave);
            updateResourceList();
            dialog.close();
            Notification.show("Recurso guardado exitosamente");
        });

        Button cancelButton = new Button("Cancelar", e -> dialog.close());

        Button deleteButton = new Button("Eliminar", e -> {
            if (resource != null) {
                resourceService.deleteById(resource.getId());
                updateResourceList();
                dialog.close();
                Notification.show("Recurso eliminado exitosamente");
            }
        });
        deleteButton.getStyle().set("color", "red");

        dialog.getFooter().add(cancelButton);
        if (resource != null) {
            dialog.getFooter().add(deleteButton);
        }
        dialog.getFooter().add(saveButton);

        dialog.open();

        dialog.addOpenedChangeListener(e -> {
            if (!e.isOpened()) {
                resourceGrid.asSingleSelect().clear();
            }
        });
    }

    private void updateList() {
        grid.setItems(pmoService.getAll());
    }

    private void updateResourceList() {
        resourceGrid.setItems(resourceService.findAll());
    }
}