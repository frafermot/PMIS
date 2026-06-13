package com.example.examplefeature.ui;

import com.example.base.ui.MainLayout;
import com.example.user.User;
import com.example.user.UserService;
import com.example.user.Role;
import com.example.security.PasswordGenerator;
import com.example.security.SecurityService;
import com.example.resource.Resource;
import com.example.resource.ResourceService;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "usuarios", layout = MainLayout.class)
@PageTitle("Usuarios")
@Menu(order = 2, icon = "vaadin:users", title = "Usuarios")
@RolesAllowed({ "ADMIN", "MANAGER" })
public class UserView extends VerticalLayout {

    private final UserService userService;
    private final SecurityService securityService;
    private final ResourceService resourceService;
    private final Grid<User> grid = new Grid<>(User.class);

    private final PasswordEncoder passwordEncoder;
    private final PasswordGenerator passwordGenerator;

    public UserView(UserService userService, PasswordEncoder passwordEncoder, PasswordGenerator passwordGenerator,
            SecurityService securityService, ResourceService resourceService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.passwordGenerator = passwordGenerator;
        this.securityService = securityService;
        this.resourceService = resourceService;

        setSizeFull();
        configureGrid();

        add(new H2("Vista de Usuarios"), createToolbar(), grid);
        updateList();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();
        grid.addColumn(User::getId).setHeader("ID").setWidth("60px").setFlexGrow(0);
        grid.addColumn(User::getName).setHeader("Nombre").setFlexGrow(2);
        grid.addColumn(User::getUvus).setHeader("UVUS").setFlexGrow(1);
        grid.addColumn(user -> user.getProject() != null ? user.getProject().getName() : "Sin Proyecto")
                .setHeader("Proyecto").setFlexGrow(1);
        grid.addColumn(user -> user.getResource() != null ? user.getResource().getResourceType() + " - " + user.getResource().getProfessionalProfile() : "Sin Recurso")
                .setHeader("Perfil de Recurso").setFlexGrow(1);

        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                openUserDetailDialog(event.getValue());
            }
        });
    }

    private HorizontalLayout createToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();

        // Only PMO Directors can add new Users (Role.USER)
        // Admins and Managers can also manage users globally
        if (securityService.isPmoDirector() || securityService.isAdminOrManager()) {
            Button addUserButton = new Button("Añadir Usuario");
            addUserButton.addClickListener(e -> openCreateUserDialog());
            Button addUsersCsvButton = new Button("Añadir Usuarios desde CSV");
            addUsersCsvButton.addClickListener(e -> openCreateUsersFromCsvDialog());
            toolbar.add(addUserButton, addUsersCsvButton);
        }

        return toolbar;
    }

    private void openUserDetailDialog(User user) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Detalles del Usuario");

        TextField nameField = new TextField("Nombre");
        nameField.setValue(user.getName());
        nameField.setReadOnly(true);

        TextField uvusField = new TextField("UVUS");
        uvusField.setValue(user.getUvus());
        uvusField.setReadOnly(true);

        TextField projectField = new TextField("Proyecto");
        projectField.setValue(user.getProject() != null ? user.getProject().getName() : "Sin Proyecto");
        projectField.setReadOnly(true);

        ComboBox<Resource> resourceComboBox = new ComboBox<>("Perfil de Recurso");
        resourceComboBox.setItems(resourceService.findAll());
        resourceComboBox.setItemLabelGenerator(r -> r.getResourceType() + " - " + r.getProfessionalProfile());
        resourceComboBox.setValue(user.getResource());
        resourceComboBox.setReadOnly(!(securityService.isPmoDirector() || securityService.isAdminOrManager()));

        VerticalLayout dialogLayout = new VerticalLayout(nameField, uvusField, projectField, resourceComboBox);
        dialog.add(dialogLayout);

        Button closeButton = new Button("Cerrar", e -> dialog.close());
        dialog.getFooter().add(closeButton);

        if (securityService.isPmoDirector() || securityService.isAdminOrManager()) {
            Button saveButton = new Button("Guardar Cambios", e -> {
                user.setResource(resourceComboBox.getValue());
                userService.createOrUpdate(user);
                updateList();
                dialog.close();
                Notification.show("Cambios guardados exitosamente");
            });
            dialog.getFooter().add(saveButton);
        }

        // PMO Directors, Admins and Managers can delete Users (Role.USER)
        if (securityService.isPmoDirector() || securityService.isAdminOrManager()) {
            Button deleteButton = new Button("Eliminar", e -> {
                try {
                    if (userService.hasAssignedEntities(user.getId())) {
                        Dialog confirmDialog = new Dialog();
                        confirmDialog.setHeaderTitle("Eliminar Usuario");
                        confirmDialog.add(
                                "Este usuario está asignado como director o sponsor en otros elementos. ¿Desea desasignarlo y eliminarlo?");

                        Button confirmDeleteButton = new Button("Eliminar", event -> {
                            try {
                                userService.deleteSafe(user.getId());
                                updateList();
                                dialog.close();
                                confirmDialog.close();
                                Notification.show("Usuario eliminado y desasignado exitosamente");
                            } catch (SecurityException ex) {
                                Notification.show("Error: " + com.example.base.ui.MainErrorHandler.getPersonalizedMessage(ex), 5000, Notification.Position.MIDDLE);
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
                        Notification.show("Usuario eliminado exitosamente");
                    }
                } catch (SecurityException ex) {
                    Notification.show("Error: " + com.example.base.ui.MainErrorHandler.getPersonalizedMessage(ex), 5000, Notification.Position.MIDDLE);
                }
            });
            deleteButton.getStyle().set("color", "red");
            dialog.getFooter().add(deleteButton);
        }

        dialog.open();

        dialog.addOpenedChangeListener(e -> {
            if (!e.isOpened()) {
                grid.asSingleSelect().clear();
            }
        });
    }

    private void openCreateUserDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Nuevo Usuario");

        TextField nameField = new TextField("Nombre");
        TextField uvusField = new TextField("UVUS");

        ComboBox<Resource> resourceComboBox = new ComboBox<>("Perfil de Recurso");
        resourceComboBox.setItems(resourceService.findAll());
        resourceComboBox.setItemLabelGenerator(r -> r.getResourceType() + " - " + r.getProfessionalProfile());

        VerticalLayout dialogLayout = new VerticalLayout(nameField, uvusField, resourceComboBox);
        dialog.add(dialogLayout);

        Button saveButton = new Button("Guardar", e -> {
            if (nameField.isEmpty() || uvusField.isEmpty()) {
                Notification.show("Por favor rellene todos los campos");
                return;
            }

            User newUser = new User();
            newUser.setName(nameField.getValue());
            newUser.setUvus(uvusField.getValue());
            newUser.setRole(Role.USER); // Explicitly set Role.USER
            newUser.setResource(resourceComboBox.getValue());

            String generatedPassword = passwordGenerator.generateStrongPassword();
            newUser.setPassword(passwordEncoder.encode(generatedPassword));

            userService.createOrUpdate(newUser);
            updateList();
            dialog.close();

            Notification notification = Notification.show("Usuario creado. Contraseña: " + generatedPassword);
            notification.setDuration(10000);
        });

        Button cancelButton = new Button("Cancelar", e -> dialog.close());

        dialog.getFooter().add(cancelButton);
        dialog.getFooter().add(saveButton);

        dialog.open();
    }

    private void openCreateUsersFromCsvDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Añadir Usuarios desde CSV");

        TextArea uvusListArea = new TextArea("Lista de UVUS (separados por comas o saltos de línea)");
        uvusListArea.setWidthFull();
        uvusListArea.setHeight("200px");
        uvusListArea.setPlaceholder("ejemplo1, ejemplo2\nejemplo3");

        VerticalLayout dialogLayout = new VerticalLayout(uvusListArea);
        dialogLayout.setWidth("400px");
        dialog.add(dialogLayout);

        Button saveButton = new Button("Crear Usuarios", e -> {
            if (uvusListArea.isEmpty()) {
                Notification.show("Por favor introduzca al menos un UVUS");
                return;
            }

            String[] uvusArray = uvusListArea.getValue().split("[,\\s\\n\\r]+");
            int createdCount = 0;
            
            for (String uvus : uvusArray) {
                if (!uvus.trim().isEmpty()) {
                    // Check if user already exists
                    if (userService.findByUvus(uvus.trim()) != null) {
                        Notification.show("El usuario " + uvus.trim() + " ya existe, omitiendo...");
                        continue;
                    }

                    User newUser = new User();
                    newUser.setName(uvus.trim());
                    newUser.setUvus(uvus.trim());
                    newUser.setRole(Role.USER);

                    String generatedPassword = passwordGenerator.generateStrongPassword();
                    newUser.setPassword(passwordEncoder.encode(generatedPassword));

                    try {
                        userService.createOrUpdate(newUser);
                        createdCount++;
                    } catch (Exception ex) {
                        Notification.show("Error creando el usuario: " + uvus.trim());
                    }
                }
            }

            updateList();
            dialog.close();

            Notification notification = Notification.show("Se han creado " + createdCount + " usuarios exitosamente.");
            notification.setDuration(5000);
        });

        Button cancelButton = new Button("Cancelar", e -> dialog.close());

        dialog.getFooter().add(cancelButton);
        dialog.getFooter().add(saveButton);

        dialog.open();
    }

    private void updateList() {
        grid.setItems(userService.getAll());
    }
}
