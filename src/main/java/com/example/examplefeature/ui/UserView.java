package com.example.examplefeature.ui;

import com.example.base.ui.MainLayout;
import com.example.user.User;
import com.example.user.UserService;
import com.vaadin.flow.component.button.Button;
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

@Route(value = "usuarios", layout = MainLayout.class)
@PageTitle("Usuarios")
@Menu(order = 4, icon = "vaadin:users", title = "Usuarios")
public class UserView extends VerticalLayout {

    private final UserService userService;
    private final Grid<User> grid = new Grid<>(User.class);

    public UserView(UserService userService) {
        this.userService = userService;

        setSizeFull();
        configureGrid();

        add(new H2("Vista de Usuarios"), createToolbar(), grid);
        updateList();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();
        grid.addColumn(User::getId).setHeader("ID");
        grid.addColumn(User::getName).setHeader("Nombre");
        grid.addColumn(User::getUvus).setHeader("UVUS");
        grid.addColumn(user -> user.getProject() != null ? user.getProject().getName() : "Sin Proyecto")
                .setHeader("Proyecto");
    }

    private HorizontalLayout createToolbar() {
        Button addUserButton = new Button("Añadir Usuario");
        addUserButton.addClickListener(e -> openCreateUserDialog());

        return new HorizontalLayout(addUserButton);
    }

    private void openCreateUserDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Nuevo Usuario");

        TextField nameField = new TextField("Nombre");
        TextField uvusField = new TextField("UVUS");

        VerticalLayout dialogLayout = new VerticalLayout(nameField, uvusField);
        dialog.add(dialogLayout);

        Button saveButton = new Button("Guardar", e -> {
            if (nameField.isEmpty() || uvusField.isEmpty()) {
                Notification.show("Por favor rellene todos los campos");
                return;
            }

            User newUser = new User();
            newUser.setName(nameField.getValue());
            newUser.setUvus(uvusField.getValue());
            // Project is optional now and removed from form

            userService.createOrUpdateUser(newUser);
            updateList();
            dialog.close();
            Notification.show("Usuario creado exitosamente");
        });

        Button cancelButton = new Button("Cancelar", e -> dialog.close());

        dialog.getFooter().add(cancelButton);
        dialog.getFooter().add(saveButton);

        dialog.open();
    }

    private void updateList() {
        grid.setItems(userService.findAll());
    }
}
