package com.example.examplefeature.ui;

import com.example.base.ui.MainLayout;
import com.example.manager.Manager;
import com.example.manager.ManagerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "gestores", layout = MainLayout.class)
@PageTitle("Registro de Gestores")
@Menu(order = 0, icon = "vaadin:users", title = "Registro de Gestores")
public class GestorView extends VerticalLayout {

    private final ManagerService managerService;
    private final Grid<Manager> grid = new Grid<>(Manager.class);

    public GestorView(ManagerService managerService) {
        this.managerService = managerService;

        setSizeFull();
        configureGrid();

        add(new H2("Vista de Registro de Gestores"), createToolbar(), grid);
        updateList();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();
        grid.addColumn(Manager::getId).setHeader("ID");
        grid.addColumn(Manager::getName).setHeader("Nombre");
        grid.addColumn(Manager::getUvus).setHeader("UVUS");
        grid.addColumn(manager -> manager.getIsAdmin() ? "Sí" : "No").setHeader("Es Admin");
    }

    private HorizontalLayout createToolbar() {
        Button addManagerButton = new Button("Añadir Gestor");
        addManagerButton.addClickListener(e -> openCreateManagerDialog());

        return new HorizontalLayout(addManagerButton);
    }

    private void openCreateManagerDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Nuevo Gestor");

        TextField nameField = new TextField("Nombre");
        TextField uvusField = new TextField("UVUS");
        Checkbox isAdminCheckbox = new Checkbox("Es Admin");

        VerticalLayout dialogLayout = new VerticalLayout(nameField, uvusField, isAdminCheckbox);
        dialog.add(dialogLayout);

        Button saveButton = new Button("Guardar", e -> {
            if (nameField.isEmpty() || uvusField.isEmpty()) {
                Notification.show("Por favor rellene todos los campos obligatorios");
                return;
            }

            Manager newManager = new Manager();
            newManager.setName(nameField.getValue());
            newManager.setUvus(uvusField.getValue());
            newManager.setIsAdmin(isAdminCheckbox.getValue());

            managerService.createOrUpdateManager(newManager);
            updateList();
            dialog.close();
            Notification.show("Gestor creado exitosamente");
        });

        Button cancelButton = new Button("Cancelar", e -> dialog.close());

        dialog.getFooter().add(cancelButton);
        dialog.getFooter().add(saveButton);

        dialog.open();
    }

    private void updateList() {
        grid.setItems(managerService.findAll());
    }
}