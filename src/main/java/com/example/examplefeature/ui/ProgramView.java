package com.example.examplefeature.ui;

import java.util.List;

import com.example.base.ui.MainLayout;

import com.example.security.SecurityService;
import com.example.user.User;
import com.example.user.UserService;
import com.example.user.Role;
import com.example.portfolio.Portfolio;
import com.example.portfolio.PortfolioService;
import com.example.portfolio.PortfolioRepository;
import com.example.program.Program;
import com.example.program.ProgramService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "programas", layout = MainLayout.class)
@PageTitle("Programas")
@RolesAllowed({ "ADMIN", "MANAGER" })
public class ProgramView extends VerticalLayout {

    private final ProgramService programService;
    private final PortfolioService portfolioService;
    private final PortfolioRepository portfolioRepository;
    private final UserService userService;
    private final SecurityService securityService;
    private final Grid<Program> grid = new Grid<>(Program.class);

    public ProgramView(ProgramService programService, PortfolioService portfolioService,
            UserService userService, SecurityService securityService, PortfolioRepository portfolioRepository) {
        this.programService = programService;
        this.portfolioService = portfolioService;
        this.portfolioRepository = portfolioRepository;
        this.userService = userService;
        this.securityService = securityService;

        setSizeFull();
        configureGrid();

        add(new H2("Vista de Programas"), createToolbar(), grid);
        updateList();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();
        grid.addColumn(Program::getId).setHeader("ID").setWidth("100px");
        grid.addColumn(Program::getName).setHeader("Nombre");
        grid.addColumn(program -> program.getPortfolio() != null ? program.getPortfolio().getName() : "Sin Portafolio")
                .setHeader("Portafolio");
        grid.addColumn(program -> program.getDirector() != null ? program.getDirector().getName() : "Sin Director")
                .setHeader("Director");

        // Columna de Editar
        grid.addComponentColumn(program -> {
            Button editButton = new Button("Editar", e -> openProgramDialog(program));
            editButton.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_SMALL,
                    com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);
            return editButton;
        }).setHeader("").setWidth("80px").setFlexGrow(0);

        // Columna de Borrar
        grid.addComponentColumn(program -> {
            Button deleteButton = new Button("Borrar", e -> deleteProgram(program));
            deleteButton.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_SMALL,
                    com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR);
            return deleteButton;
        }).setHeader("").setWidth("80px").setFlexGrow(0);

        // Navigate to detail view on row click
        grid.addItemClickListener(event -> {
            if (event.getItem() != null) {
                com.vaadin.flow.component.UI.getCurrent().navigate("program/" + event.getItem().getId());
            }
        });
    }

    private HorizontalLayout createToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();

        // Admins and portfolio directors can add new programs
        boolean isPortfolioDirector = securityService.getCurrentUser() != null &&
                !portfolioRepository.findAllByDirectorIdWithDirector(securityService.getCurrentUser().getId())
                        .isEmpty();

        if (securityService.isAdmin() || isPortfolioDirector) {
            Button addProgramButton = new Button("Añadir Programa");
            addProgramButton.addClickListener(e -> openProgramDialog(null));
            toolbar.add(addProgramButton);
        }

        return toolbar;
    }

    private void deleteProgram(Program program) {
        if (programService.hasProjects(program.getId())) {
            Dialog confirmDialog = new Dialog();
            confirmDialog.setHeaderTitle("Eliminar Programa");
            confirmDialog.add(
                    "Este programa tiene proyectos asociados. ¿Desea eliminarlo junto con todos sus proyectos?");

            Button confirmDeleteButton = new Button("Eliminar Todo", event -> {
                programService.deleteWithCascade(program.getId());
                updateList();
                confirmDialog.close();
                Notification.show("Programa y proyectos eliminados exitosamente");
            });
            confirmDeleteButton.getStyle().set("color", "red");

            Button cancelDeleteButton = new Button("Cancelar", event -> confirmDialog.close());

            confirmDialog.getFooter().add(cancelDeleteButton);
            confirmDialog.getFooter().add(confirmDeleteButton);
            confirmDialog.open();
        } else {
            programService.delete(program.getId());
            updateList();
            Notification.show("Programa eliminado exitosamente");
        }
    }

    private void openProgramDialog(Program program) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(program == null ? "Nuevo Programa" : "Editar Programa");

        TextField nameField = new TextField("Nombre");

        Select<Portfolio> portfolioSelect = new Select<>();
        portfolioSelect.setLabel("Portafolio");
        portfolioSelect.setItems(portfolioService.getAll());
        portfolioSelect.setItemLabelGenerator(Portfolio::getName);

        Select<User> directorSelect = new Select<>();
        directorSelect.setLabel("Director");
        directorSelect.setItems(userService.findAllByRoles(List.of(Role.MANAGER, Role.ADMIN)));
        directorSelect.setItemLabelGenerator(User::getName);

        if (program != null) {
            nameField.setValue(program.getName());
            portfolioSelect.setValue(program.getPortfolio());
            directorSelect.setValue(program.getDirector());
        }

        VerticalLayout dialogLayout = new VerticalLayout(nameField, portfolioSelect, directorSelect);
        dialog.add(dialogLayout);

        Button saveButton = new Button("Guardar", e -> {
            if (nameField.isEmpty() || portfolioSelect.isEmpty() || directorSelect.isEmpty()) {
                Notification.show("Por favor rellene todos los campos");
                return;
            }

            Program programToSave = program == null ? new Program() : program;
            programToSave.setName(nameField.getValue());
            programToSave.setPortfolio(portfolioSelect.getValue());
            programToSave.setDirector(directorSelect.getValue());

            programService.createOrUpdate(programToSave);
            updateList();
            dialog.close();
            Notification.show("Programa guardado exitosamente");
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
