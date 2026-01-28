package com.example.examplefeature.ui;

import com.example.base.ui.MainLayout;
import com.example.user.Role;
import com.example.user.User;
import com.example.user.UserService;
import com.example.security.PasswordGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;
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

import jakarta.annotation.security.RolesAllowed;

@Route(value = "gestores", layout = MainLayout.class)
@PageTitle("Registro de Gestores")
@Menu(order = 1, icon = "vaadin:users", title = "Registro de Gestores")
@RolesAllowed("ADMIN")
public class GestorView extends VerticalLayout {

    private final UserService userService;
    private final Grid<User> grid = new Grid<>(User.class);

    private final PasswordEncoder passwordEncoder;
    private final PasswordGenerator passwordGenerator;

    public GestorView(UserService userService, PasswordEncoder passwordEncoder, PasswordGenerator passwordGenerator) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.passwordGenerator = passwordGenerator;

        setSizeFull();
        configureGrid();

        add(new H2("Vista de Registro de Gestores"), createToolbar(), grid);
        updateList();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();
        grid.addColumn(User::getId).setHeader("ID");
        grid.addColumn(User::getName).setHeader("Nombre");
        grid.addColumn(User::getUvus).setHeader("UVUS");
        grid.addColumn(user -> user.getRole() == Role.ADMIN ? "Sí" : "No").setHeader("Es Admin");

        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                openGestorDetailDialog(event.getValue());
            }
        });
    }

    private HorizontalLayout createToolbar() {
        Button addManagerButton = new Button("Añadir Gestor");
        addManagerButton.addClickListener(e -> openCreateManagerDialog());

        return new HorizontalLayout(addManagerButton);
    }

    private void openGestorDetailDialog(User user) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Detalles del Gestor");

        TextField nameField = new TextField("Nombre");
        nameField.setValue(user.getName());
        nameField.setReadOnly(true);

        TextField uvusField = new TextField("UVUS");
        uvusField.setValue(user.getUvus());
        uvusField.setReadOnly(true);

        TextField isAdminField = new TextField("Es Admin");
        isAdminField.setValue(user.getRole() == Role.ADMIN ? "Sí" : "No");
        isAdminField.setReadOnly(true);

        VerticalLayout dialogLayout = new VerticalLayout(nameField, uvusField, isAdminField);
        dialog.add(dialogLayout);

        Button closeButton = new Button("Cerrar", e -> dialog.close());

        Button deleteButton = new Button("Eliminar", e -> {
            // Check if trying to delete an admin
            if (user.getRole() == Role.ADMIN) {
                Notification.show("No se puede eliminar un usuario con rol ADMIN", 5000, Notification.Position.MIDDLE);
                return;
            }

            try {
                if (userService.hasAssignedEntities(user.getId())) {
                    Dialog confirmDialog = new Dialog();
                    confirmDialog.setHeaderTitle("Eliminar Gestor");
                    confirmDialog.add(
                            "Este gestor está asignado como director o sponsor en otros elementos. ¿Desea desasignarlo y eliminarlo?");

                    Button confirmDeleteButton = new Button("Eliminar", event -> {
                        try {
                            userService.deleteSafe(user.getId());
                            updateList();
                            dialog.close();
                            confirmDialog.close();
                            Notification.show("Gestor eliminado y desasignado exitosamente");
                        } catch (SecurityException ex) {
                            Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
                        }
                    });
                    confirmDeleteButton.getStyle().set("color", "red");

                    Button cancelDeleteButton = new Button("Cancelar", event -> confirmDialog.close());

                    confirmDialog.getFooter().add(cancelDeleteButton);
                    confirmDialog.getFooter().add(confirmDeleteButton);
                    confirmDialog.open();
                } else {
                    userService.delete(user.getId());
                    updateList();
                    dialog.close();
                    Notification.show("Gestor eliminado exitosamente");
                }
            } catch (SecurityException ex) {
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });
        deleteButton.getStyle().set("color", "red");

        dialog.getFooter().add(closeButton);
        dialog.getFooter().add(deleteButton);

        dialog.open();

        dialog.addOpenedChangeListener(e -> {
            if (!e.isOpened()) {
                grid.asSingleSelect().clear();
            }
        });
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

            User newManager = new User();
            newManager.setName(nameField.getValue());
            newManager.setUvus(uvusField.getValue());
            newManager.setRole(Boolean.TRUE.equals(isAdminCheckbox.getValue()) ? Role.ADMIN : Role.MANAGER);

            String generatedPassword = passwordGenerator.generateStrongPassword();
            newManager.setPassword(passwordEncoder.encode(generatedPassword));

            userService.createOrUpdate(newManager);
            updateList();
            dialog.close();

            Notification notification = Notification.show("Gestor creado. Contraseña: " + generatedPassword);
            notification.setDuration(10000); // 10 seconds to read/copy
        });

        Button cancelButton = new Button("Cancelar", e -> dialog.close());

        dialog.getFooter().add(cancelButton);
        dialog.getFooter().add(saveButton);

        dialog.open();
    }

    private void updateList() {
        // Show both Managers and Admins, or just Managers?
        // "Gestor" usually implies Manager role. Admin implies Admin role.
        // I will show both for now since the view allows creating Admins.
        // Or I can filter. Let's show all for now or filter by Role IN (ADMIN,
        // MANAGER).
        // Since findAllByRole takes one role, I might need to fetch all and filter or
        // add a method.
        // For simplicity and to match previous behavior (which likely showed all
        // 'Manager' entities including admins),
        // I will show Users who are NOT Role.USER (i.e. Managers and Admins).
        grid.setItems(userService.getAll().stream()
                .filter(u -> u.getRole() == Role.MANAGER || u.getRole() == Role.ADMIN)
                .toList());
    }
}