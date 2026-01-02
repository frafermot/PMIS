package com.example.examplefeature.ui;

import com.example.base.ui.MainLayout;
import com.example.manager.Manager;
import com.example.manager.ManagerService;
import com.example.portfolio.Portfolio;
import com.example.portfolio.PortfolioService;
import com.example.pmo.PMO;
import com.example.pmo.PMOService;
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

@Route(value = "pmo", layout = MainLayout.class)
@PageTitle("Oficina de Proyectos")
@Menu(order = 2, icon = "vaadin:chart-timeline", title = "Oficina de Proyectos")
public class PmoView extends VerticalLayout {

    private final PMOService pmoService;
    private final PortfolioService portfolioService;
    private final ManagerService managerService;
    private final Grid<PMO> grid = new Grid<>(PMO.class);

    public PmoView(PMOService pmoService, PortfolioService portfolioService, ManagerService managerService) {
        this.pmoService = pmoService;
        this.portfolioService = portfolioService;
        this.managerService = managerService;

        setSizeFull();
        configureGrid();

        add(new H2("Vista de Oficina de Dirección de Proyectos (PMO)"), createToolbar(), grid);
        updateList();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();
        grid.addColumn(PMO::getId).setHeader("ID");
        grid.addColumn(PMO::getName).setHeader("Nombre");
        grid.addColumn(pmo -> pmo.getPortfolio() != null ? pmo.getPortfolio().getName() : "Sin Portafolio")
                .setHeader("Portafolio");
        grid.addColumn(pmo -> pmo.getDirector() != null ? pmo.getDirector().getName() : "Sin Director")
                .setHeader("Director");
    }

    private HorizontalLayout createToolbar() {
        Button addPmoButton = new Button("Añadir PMO");
        addPmoButton.addClickListener(e -> openCreatePmoDialog());

        return new HorizontalLayout(addPmoButton);
    }

    private void openCreatePmoDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Nueva PMO");

        TextField nameField = new TextField("Nombre");

        ComboBox<Portfolio> portfolioComboBox = new ComboBox<>("Portafolio");
        portfolioComboBox.setItems(portfolioService.findAll());
        portfolioComboBox.setItemLabelGenerator(Portfolio::getName);

        ComboBox<Manager> directorComboBox = new ComboBox<>("Director");
        directorComboBox.setItems(managerService.findAll());
        directorComboBox.setItemLabelGenerator(Manager::getName);

        VerticalLayout dialogLayout = new VerticalLayout(nameField, portfolioComboBox, directorComboBox);
        dialog.add(dialogLayout);

        Button saveButton = new Button("Guardar", e -> {
            if (nameField.isEmpty() || portfolioComboBox.isEmpty() || directorComboBox.isEmpty()) {
                Notification.show("Por favor rellene todos los campos");
                return;
            }

            PMO newPmo = new PMO();
            newPmo.setName(nameField.getValue());
            newPmo.setPortfolio(portfolioComboBox.getValue());
            newPmo.setDirector(directorComboBox.getValue());

            pmoService.createOrUpdatePMO(newPmo);
            updateList();
            dialog.close();
            Notification.show("PMO creada exitosamente");
        });

        Button cancelButton = new Button("Cancelar", e -> dialog.close());

        dialog.getFooter().add(cancelButton);
        dialog.getFooter().add(saveButton);

        dialog.open();
    }

    private void updateList() {
        grid.setItems(pmoService.findAll());
    }
}