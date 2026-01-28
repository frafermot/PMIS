package com.example.examplefeature.ui;

import com.example.base.ui.MainLayout;
import com.example.portfolio.Portfolio;
import com.example.portfolio.PortfolioService;
import com.example.program.Program;
import com.example.program.ProgramService;
import com.example.user.User;
import com.example.user.UserService;
import com.example.user.Role;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

@Route(value = "portfolio", layout = MainLayout.class)
@PageTitle("Detalle de Portfolio")
@RolesAllowed({ "ADMIN", "MANAGER" })
public class PortfolioDetailView extends VerticalLayout implements HasUrlParameter<Long> {

    private final PortfolioService portfolioService;
    private final ProgramService programService;
    private final UserService userService;
    private Portfolio currentPortfolio;
    private final Grid<Program> programGrid = new Grid<>(Program.class, false);

    private TextField nameField;
    private Select<User> directorSelect;

    public PortfolioDetailView(PortfolioService portfolioService, ProgramService programService,
            UserService userService) {
        this.portfolioService = portfolioService;
        this.programService = programService;
        this.userService = userService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }

    @Override
    public void setParameter(BeforeEvent event, Long portfolioId) {
        currentPortfolio = portfolioService.get(portfolioId);

        if (currentPortfolio == null) {
            Notification.show("Portfolio no encontrado");
            UI.getCurrent().navigate("portfolios");
            return;
        }

        removeAll();
        buildView();
    }

    private void buildView() {
        // Breadcrumb / Navigation
        Button backButton = new Button("← Volver a Portfolios", e -> {
            UI.getCurrent().navigate("portfolios");
        });
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        add(backButton);

        // Portfolio Info Section
        add(new H2("Información del Portfolio"));

        nameField = new TextField("Nombre");
        nameField.setValue(currentPortfolio.getName());
        nameField.setWidthFull();

        directorSelect = new Select<>();
        directorSelect.setLabel("Director");
        directorSelect.setItems(userService.findAllByRoles(List.of(Role.MANAGER, Role.ADMIN)));
        directorSelect.setItemLabelGenerator(User::getName);
        directorSelect.setValue(currentPortfolio.getDirector());
        directorSelect.setWidthFull();

        Button saveButton = new Button("Guardar Cambios", e -> savePortfolio());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout formLayout = new HorizontalLayout(nameField, directorSelect, saveButton);
        formLayout.setWidthFull();
        formLayout.setAlignItems(Alignment.END);
        add(formLayout);

        // Programs Section
        add(new H3("Programas de este Portfolio"));

        configureGrid();
        Button addProgramButton = new Button("Añadir Programa", e -> openCreateProgramDialog());
        addProgramButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(addProgramButton, programGrid);
        updateProgramList();
    }

    private void configureGrid() {
        programGrid.setSizeFull();
        programGrid.addColumn(Program::getId).setHeader("ID").setWidth("100px");
        programGrid.addColumn(Program::getName).setHeader("Nombre");
        programGrid
                .addColumn(program -> program.getDirector() != null ? program.getDirector().getName() : "Sin Director")
                .setHeader("Director");

        // Columna de Editar
        programGrid.addComponentColumn(program -> {
            Button editButton = new Button("Editar", e -> openEditProgramDialog(program));
            editButton.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_SMALL,
                    com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);
            return editButton;
        }).setHeader("").setWidth("80px").setFlexGrow(0);

        // Columna de Borrar
        programGrid.addComponentColumn(program -> {
            Button deleteButton = new Button("Borrar", e -> deleteProgram(program));
            deleteButton.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_SMALL,
                    com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR);
            return deleteButton;
        }).setHeader("").setWidth("80px").setFlexGrow(0);

        programGrid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                UI.getCurrent().navigate("program/" + event.getValue().getId());
            }
        });
    }

    private void savePortfolio() {
        if (nameField.isEmpty() || directorSelect.isEmpty()) {
            Notification.show("Por favor rellene todos los campos");
            return;
        }

        try {
            currentPortfolio.setName(nameField.getValue());
            currentPortfolio.setDirector(directorSelect.getValue());
            portfolioService.createOrUpdate(currentPortfolio);
            Notification.show("Portfolio actualizado exitosamente");
        } catch (SecurityException ex) {
            Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private void openCreateProgramDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Nuevo Programa");

        TextField programNameField = new TextField("Nombre");
        Select<User> programDirectorSelect = new Select<>();
        programDirectorSelect.setLabel("Director");
        programDirectorSelect.setItems(userService.findAllByRoles(List.of(Role.MANAGER, Role.ADMIN)));
        programDirectorSelect.setItemLabelGenerator(User::getName);

        VerticalLayout dialogLayout = new VerticalLayout(programNameField, programDirectorSelect);
        dialog.add(dialogLayout);

        Button saveButton = new Button("Guardar", e -> {
            if (programNameField.isEmpty() || programDirectorSelect.isEmpty()) {
                Notification.show("Por favor rellene todos los campos");
                return;
            }

            Program newProgram = new Program();
            newProgram.setName(programNameField.getValue());
            newProgram.setDirector(programDirectorSelect.getValue());
            newProgram.setPortfolio(currentPortfolio);

            programService.createOrUpdate(newProgram);
            updateProgramList();
            dialog.close();
            Notification.show("Programa creado exitosamente");
        });

        Button cancelButton = new Button("Cancelar", e -> dialog.close());

        dialog.getFooter().add(cancelButton);
        dialog.getFooter().add(saveButton);

        dialog.open();
    }

    private void updateProgramList() {
        programGrid.setItems(programService.getByPortfolioId(currentPortfolio.getId()));
    }

    private void openEditProgramDialog(Program program) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Editar Programa");

        TextField programNameField = new TextField("Nombre");
        programNameField.setValue(program.getName());

        Select<User> programDirectorSelect = new Select<>();
        programDirectorSelect.setLabel("Director");
        programDirectorSelect.setItems(userService.findAllByRoles(List.of(Role.MANAGER, Role.ADMIN)));
        programDirectorSelect.setItemLabelGenerator(User::getName);
        programDirectorSelect.setValue(program.getDirector());

        VerticalLayout dialogLayout = new VerticalLayout(programNameField, programDirectorSelect);
        dialog.add(dialogLayout);

        Button saveButton = new Button("Guardar", e -> {
            if (programNameField.isEmpty() || programDirectorSelect.isEmpty()) {
                Notification.show("Por favor rellene todos los campos");
                return;
            }

            program.setName(programNameField.getValue());
            program.setDirector(programDirectorSelect.getValue());

            programService.createOrUpdate(program);
            updateProgramList();
            dialog.close();
            Notification.show("Programa actualizado exitosamente");
        });

        Button cancelButton = new Button("Cancelar", e -> dialog.close());

        dialog.getFooter().add(cancelButton);
        dialog.getFooter().add(saveButton);

        dialog.open();
    }

    private void deleteProgram(Program program) {
        // Check if program has projects
        if (programService.hasProjects(program.getId())) {
            Notification.show("No se puede borrar el programa porque tiene proyectos asignados", 5000,
                    Notification.Position.MIDDLE);
            return;
        }

        programService.delete(program.getId());
        updateProgramList();
        Notification.show("Programa eliminado exitosamente");
    }
}
