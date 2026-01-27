package com.example.examplefeature.ui;

import com.example.base.ui.MainLayout;

import com.example.program.Program;
import com.example.program.ProgramService;
import com.example.project.Project;
import com.example.project.ProjectService;
import com.example.user.User;
import com.example.user.UserService;
import com.example.user.Role;
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

@Route(value = "proyectos", layout = MainLayout.class)
@PageTitle("Proyectos")
@Menu(order = 5, icon = "vaadin:archive", title = "Proyectos")
@RolesAllowed({ "ADMIN", "MANAGER" })
public class ProjectView extends VerticalLayout {

    private final ProjectService projectService;
    private final UserService userService;
    private final ProgramService programService;

    private final Grid<Project> grid = new Grid<>(Project.class);

    public ProjectView(ProjectService projectService, UserService userService,
            ProgramService programService) {
        this.projectService = projectService;
        this.userService = userService;
        this.programService = programService;

        setSizeFull();
        configureGrid();

        add(new H2("Vista de Proyectos"), createToolbar(), grid);
        updateList();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();
        grid.addColumn(Project::getId).setHeader("ID");
        grid.addColumn(Project::getName).setHeader("Nombre");
        grid.addColumn(project -> project.getDirector() != null ? project.getDirector().getName() : "Sin Director")
                .setHeader("Director");
        grid.addColumn(project -> project.getProgram() != null ? project.getProgram().getName() : "Sin Programa")
                .setHeader("Programa");
        grid.addColumn(project -> project.getSponsor() != null ? project.getSponsor().getName() : "Sin Sponsor")
                .setHeader("Sponsor");
    }

    private HorizontalLayout createToolbar() {
        Button addProjectButton = new Button("Añadir Proyecto");
        addProjectButton.addClickListener(e -> openCreateProjectDialog());

        return new HorizontalLayout(addProjectButton);
    }

    private void openCreateProjectDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Nuevo Proyecto");

        TextField nameField = new TextField("Nombre");

        ComboBox<User> directorComboBox = new ComboBox<>("Director");
        directorComboBox.setItems(userService.getAll());
        directorComboBox.setItemLabelGenerator(User::getName);

        ComboBox<Program> programComboBox = new ComboBox<>("Programa");
        programComboBox.setItems(programService.getAll());
        programComboBox.setItemLabelGenerator(Program::getName);

        ComboBox<User> sponsorComboBox = new ComboBox<>("Sponsor");
        sponsorComboBox.setItems(userService.findAllByRole(Role.MANAGER));
        sponsorComboBox.setItemLabelGenerator(User::getName);

        VerticalLayout dialogLayout = new VerticalLayout(nameField, directorComboBox, programComboBox, sponsorComboBox);
        dialog.add(dialogLayout);

        Button saveButton = new Button("Guardar", e -> {
            if (nameField.isEmpty() || directorComboBox.isEmpty() || programComboBox.isEmpty()
                    || sponsorComboBox.isEmpty()) {
                Notification.show("Por favor rellene todos los campos");
                return;
            }

            Project newProject = new Project();
            newProject.setName(nameField.getValue());
            newProject.setDirector(directorComboBox.getValue());
            newProject.setProgram(programComboBox.getValue());
            newProject.setSponsor(sponsorComboBox.getValue());

            projectService.createOrUpdate(newProject);
            updateList();
            dialog.close();
            Notification.show("Proyecto creado exitosamente");
        });

        Button cancelButton = new Button("Cancelar", e -> dialog.close());

        dialog.getFooter().add(cancelButton);
        dialog.getFooter().add(saveButton);

        dialog.open();
    }

    private void updateList() {
        grid.setItems(projectService.getAll());
    }
}
