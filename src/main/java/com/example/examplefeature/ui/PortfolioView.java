package com.example.examplefeature.ui;

import com.example.base.ui.MainLayout;

import com.example.user.User;
import com.example.user.UserService;
import com.example.user.Role;
import com.example.portfolio.Portfolio;
import com.example.portfolio.PortfolioService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
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
@RolesAllowed({ "ADMIN", "MANAGER" })
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
    }

    private HorizontalLayout createToolbar() {
        Button addPortfolioButton = new Button("Añadir Portfolio");
        addPortfolioButton.addClickListener(e -> openCreatePortfolioDialog());

        return new HorizontalLayout(addPortfolioButton);
    }

    private void openCreatePortfolioDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Nuevo Portfolio");

        TextField nameField = new TextField("Nombre");
        ComboBox<User> directorComboBox = new ComboBox<>("Director");
        directorComboBox.setItems(userService.findAllByRole(Role.MANAGER));
        directorComboBox.setItemLabelGenerator(User::getName);

        VerticalLayout dialogLayout = new VerticalLayout(nameField, directorComboBox);
        dialog.add(dialogLayout);

        Button saveButton = new Button("Guardar", e -> {
            if (nameField.isEmpty() || directorComboBox.isEmpty()) {
                Notification.show("Por favor rellene todos los campos");
                return;
            }

            Portfolio newPortfolio = new Portfolio();
            newPortfolio.setName(nameField.getValue());
            newPortfolio.setDirector(directorComboBox.getValue());

            portfolioService.createOrUpdate(newPortfolio);
            updateList();
            dialog.close();
            Notification.show("Portfolio creado exitosamente");
        });

        Button cancelButton = new Button("Cancelar", e -> dialog.close());

        dialog.getFooter().add(cancelButton);
        dialog.getFooter().add(saveButton);

        dialog.open();
    }

    private void updateList() {
        grid.setItems(portfolioService.getAll());
    }
}