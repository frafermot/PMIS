package com.example.examplefeature.ui;

import java.util.List;

import com.example.base.ui.MainLayout;

import com.example.user.User;
import com.example.user.UserService;
import com.example.user.Role;
import com.example.portfolio.Portfolio;
import com.example.portfolio.PortfolioService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.select.Select;
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
import com.vaadin.flow.router.RouteAlias;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "portfolios", layout = MainLayout.class)
@PageTitle("Portafolios")
@Menu(order = 1, icon = "vaadin:briefcase", title = "Portafolios")
@RolesAllowed("ADMIN")
public class PortfolioView extends VerticalLayout {

    private final PortfolioService portfolioService;
    private final UserService userService;
    private final Grid<Portfolio> grid = new Grid<>(Portfolio.class);

    public PortfolioView(PortfolioService portfolioService, UserService userService) {
        this.portfolioService = portfolioService;
        this.userService = userService;

        setSizeFull();
        configureGrid();

        add(new H2("Vista de Portafolios"), createToolbar(), grid);
        updateList();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();
        grid.addColumn(Portfolio::getId).setHeader("ID");
        grid.addColumn(Portfolio::getName).setHeader("Nombre");
        grid.addColumn(
                portfolio -> portfolio.getDirector() != null ? portfolio.getDirector().getName() : "Sin Director")
                .setHeader("Director");

        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                openPortfolioDialog(event.getValue());
            }
        });
    }

    private HorizontalLayout createToolbar() {
        Button addPortfolioButton = new Button("Añadir Portfolio");
        addPortfolioButton.addClickListener(e -> openPortfolioDialog(null));

        return new HorizontalLayout(addPortfolioButton);
    }

    private void openPortfolioDialog(Portfolio portfolio) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(portfolio == null ? "Nuevo Grid Portfolio" : "Editar Portfolio");

        TextField nameField = new TextField("Nombre");
        Select<User> directorSelect = new Select<>();
        directorSelect.setLabel("Director");
        directorSelect.setItems(userService.findAllByRoles(List.of(Role.MANAGER, Role.ADMIN)));
        directorSelect.setItemLabelGenerator(User::getName);

        if (portfolio != null) {
            nameField.setValue(portfolio.getName());
            directorSelect.setValue(portfolio.getDirector());
        }

        VerticalLayout dialogLayout = new VerticalLayout(nameField, directorSelect);
        dialog.add(dialogLayout);

        Button saveButton = new Button("Guardar", e -> {
            if (nameField.isEmpty() || directorSelect.isEmpty()) {
                Notification.show("Por favor rellene todos los campos");
                return;
            }

            try {
                Portfolio portfolioToSave = portfolio == null ? new Portfolio() : portfolio;
                portfolioToSave.setName(nameField.getValue());
                portfolioToSave.setDirector(directorSelect.getValue());

                portfolioService.createOrUpdate(portfolioToSave);
                updateList();
                dialog.close();
                Notification.show("Portfolio guardado exitosamente");
            } catch (SecurityException ex) {
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });

        Button cancelButton = new Button("Cancelar", e -> dialog.close());
        Button deleteButton = new Button("Eliminar", e -> {
            if (portfolio != null) {
                try {
                    if (portfolioService.hasPrograms(portfolio.getId())) {
                        Dialog confirmDialog = new Dialog();
                        confirmDialog.setHeaderTitle("Eliminar Portfolio");
                        confirmDialog.add(
                                "Este portfolio tiene programas asociados. ¿Desea eliminarlo junto con todos sus programas y proyectos?");

                        Button confirmDeleteButton = new Button("Eliminar Todo", event -> {
                            try {
                                portfolioService.deleteWithCascade(portfolio.getId());
                                updateList();
                                dialog.close();
                                confirmDialog.close();
                                Notification.show("Portfolio, programas y proyectos eliminados exitosamente");
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
                        portfolioService.delete(portfolio.getId());
                        updateList();
                        dialog.close();
                        Notification.show("Portfolio eliminado exitosamente");
                    }
                } catch (SecurityException ex) {
                    Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
                }
            }
        });
        deleteButton.getStyle().set("color", "red");

        dialog.getFooter().add(cancelButton);
        if (portfolio != null) {
            dialog.getFooter().add(deleteButton);
        }
        dialog.getFooter().add(saveButton);

        dialog.open();

        // Clear selection when dialog is closed so the same item can be selected again
        // if needed
        dialog.addOpenedChangeListener(e -> {
            if (!e.isOpened()) {
                grid.asSingleSelect().clear();
            }
        });
    }

    private void updateList() {
        grid.setItems(portfolioService.getAll());
    }
}