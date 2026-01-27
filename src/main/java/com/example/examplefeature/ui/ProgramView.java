package com.example.examplefeature.ui;

import com.example.base.ui.MainLayout;

import com.example.user.User;
import com.example.user.UserService;
import com.example.user.Role;
import com.example.portfolio.Portfolio;
import com.example.portfolio.PortfolioService;
import com.example.program.Program;
import com.example.program.ProgramService;
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

import jakarta.annotation.security.RolesAllowed;

@Route(value = "programas", layout = MainLayout.class)
@PageTitle("Programas")
@Menu(order = 4, icon = "vaadin:archives", title = "Programas")
@RolesAllowed({ "ADMIN", "MANAGER" })
public class ProgramView extends VerticalLayout {

    private final ProgramService programService;
    private final PortfolioService portfolioService;
    private final UserService userService;
    private final Grid<Program> grid = new Grid<>(Program.class);

    public ProgramView(ProgramService programService, PortfolioService portfolioService,
            UserService userService) {
        this.programService = programService;
        this.portfolioService = portfolioService;
        this.userService = userService;

        setSizeFull();
        configureGrid();

        add(new H2("Vista de Programas"), createToolbar(), grid);
        updateList();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();
        grid.addColumn(Program::getId).setHeader("ID");
        grid.addColumn(Program::getName).setHeader("Nombre");
        grid.addColumn(program -> program.getPortfolio() != null ? program.getPortfolio().getName() : "Sin Portafolio")
                .setHeader("Portafolio");
        grid.addColumn(program -> program.getDirector() != null ? program.getDirector().getName() : "Sin Director")
                .setHeader("Director");
    }

    private HorizontalLayout createToolbar() {
        Button addProgramButton = new Button("Añadir Programa");
        addProgramButton.addClickListener(e -> openCreateProgramDialog());

        return new HorizontalLayout(addProgramButton);
    }

    private void openCreateProgramDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Nuevo Programa");

        TextField nameField = new TextField("Nombre");

        ComboBox<Portfolio> portfolioComboBox = new ComboBox<>("Portafolio");
        portfolioComboBox.setItems(portfolioService.getAll());
        portfolioComboBox.setItemLabelGenerator(Portfolio::getName);

        ComboBox<User> directorComboBox = new ComboBox<>("Director");
        directorComboBox.setItems(userService.findAllByRole(Role.MANAGER));
        directorComboBox.setItemLabelGenerator(User::getName);

        VerticalLayout dialogLayout = new VerticalLayout(nameField, portfolioComboBox, directorComboBox);
        dialog.add(dialogLayout);

        Button saveButton = new Button("Guardar", e -> {
            if (nameField.isEmpty() || portfolioComboBox.isEmpty() || directorComboBox.isEmpty()) {
                Notification.show("Por favor rellene todos los campos");
                return;
            }

            Program newProgram = new Program();
            newProgram.setName(nameField.getValue());
            newProgram.setPortfolio(portfolioComboBox.getValue());
            newProgram.setDirector(directorComboBox.getValue());

            programService.createOrUpdate(newProgram);
            updateList();
            dialog.close();
            Notification.show("Programa creado exitosamente");
        });

        Button cancelButton = new Button("Cancelar", e -> dialog.close());

        dialog.getFooter().add(cancelButton);
        dialog.getFooter().add(saveButton);

        dialog.open();
    }

    private void updateList() {
        grid.setItems(programService.getAll());
    }
}
