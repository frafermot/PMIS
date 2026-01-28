package com.example.examplefeature.ui;

import java.util.List;

import com.example.base.ui.MainLayout;

import com.example.security.SecurityService;
import com.example.program.Program;
import com.example.program.ProgramRepository;
import com.example.project.Project;
import com.example.project.ProjectService;
import com.example.user.User;
import com.example.user.UserService;
import com.example.user.Role;
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

@Route(value = "proyectos", layout = MainLayout.class)
@PageTitle("Proyectos")
@RolesAllowed({ "ADMIN", "MANAGER" })
public class ProjectView extends VerticalLayout {

    private final ProjectService projectService;
    private final SecurityService securityService;
    private final UserService userService;
    private final ProgramRepository programRepository;

    private final Grid<Project> grid = new Grid<>(Project.class);

    public ProjectView(ProjectService projectService,
            SecurityService securityService, UserService userService,
            ProgramRepository programRepository) {
        this.projectService = projectService;
        this.securityService = securityService;
        this.userService = userService;
        this.programRepository = programRepository;

        setSizeFull();
        configureGrid();

        add(new H2("Vista de Proyectos"), createToolbar(), grid);
        updateList();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();
        grid.addColumn(Project::getId).setHeader("ID").setWidth("100px");
        grid.addColumn(Project::getName).setHeader("Nombre");
        grid.addColumn(project -> project.getDirector() != null ? project.getDirector().getName() : "Sin Director")
                .setHeader("Director");
        grid.addColumn(project -> project.getProgram() != null ? project.getProgram().getName() : "Sin Programa")
                .setHeader("Programa");
        grid.addColumn(project -> project.getSponsor() != null ? project.getSponsor().getName() : "Sin Sponsor")
                .setHeader("Sponsor");

        // Columna de Editar
        grid.addComponentColumn(project -> {
            boolean canEdit = false;
            if (project.getProgram() != null) {
                if (securityService.isProgramDirector(project.getProgram().getId())) {
                    canEdit = true;
                }
            }

            if (canEdit) {
                Button editButton = new Button("Editar", e -> openProjectDialog(project));
                editButton.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_SMALL,
                        com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);
                return editButton;
            } else {
                return new com.vaadin.flow.component.html.Div(); // Componente vacío
            }
        }).setHeader("").setWidth("80px").setFlexGrow(0);

        // Columna de Borrar
        grid.addComponentColumn(project -> {
            boolean canDelete = false;
            if (project.getProgram() != null) {
                if (securityService.isProgramDirector(project.getProgram().getId())) {
                    canDelete = true;
                }
            }

            if (canDelete) {
                Button deleteButton = new Button("Borrar", e -> deleteProject(project));
                deleteButton.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_SMALL,
                        com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR);
                return deleteButton;
            } else {
                return new com.vaadin.flow.component.html.Div(); // Componente vacío
            }
        }).setHeader("").setWidth("80px").setFlexGrow(0);

        // Navigate to detail view on row click
        grid.addItemClickListener(event -> {
            if (event.getItem() != null) {
                com.vaadin.flow.component.UI.getCurrent().navigate("project/" + event.getItem().getId());
            }
        });
    }

    private HorizontalLayout createToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();

        // Program directors can add new projects
        boolean isProgramDirector = securityService.getCurrentUser() != null &&
                !programRepository.findAllByDirectorIdWithRelations(securityService.getCurrentUser().getId()).isEmpty();

        if (isProgramDirector) {
            Button addProjectButton = new Button("Añadir Proyecto");
            addProjectButton.addClickListener(e -> openProjectDialog(null));
            toolbar.add(addProjectButton);
        }

        return toolbar;
    }

    private void deleteProject(Project project) {
        if (projectService.hasAssignedUsers(project.getId())) {
            Dialog confirmDialog = new Dialog();
            confirmDialog.setHeaderTitle("Eliminar Proyecto");
            confirmDialog.add(
                    "Este proyecto tiene usuarios asignados. ¿Desea desasignar los usuarios y eliminar el proyecto?");

            Button confirmDeleteButton = new Button("Eliminar", event -> {
                projectService.deleteSafe(project.getId());
                updateList();
                confirmDialog.close();
                Notification.show("Proyecto eliminado exitosamente y usuarios desasignados");
            });
            confirmDeleteButton.getStyle().set("color", "red");

            Button cancelDeleteButton = new Button("Cancelar", event -> confirmDialog.close());

            confirmDialog.getFooter().add(cancelDeleteButton);
            confirmDialog.getFooter().add(confirmDeleteButton);
            confirmDialog.open();
        } else {
            projectService.delete(project.getId());
            updateList();
            Notification.show("Proyecto eliminado exitosamente");
        }
    }

    private void openProjectDialog(Project project) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(project == null ? "Nuevo Proyecto" : "Editar Proyecto");

        TextField nameField = new TextField("Nombre");

        Select<User> directorSelect = new Select<>();
        directorSelect.setLabel("Director");
        directorSelect.setItems(userService.getAll());
        directorSelect.setItemLabelGenerator(User::getName);

        Select<Program> programSelect = new Select<>();
        programSelect.setLabel("Programa");

        // Filter Programs (Only those managed by current user)
        java.util.Set<Program> programs = new java.util.HashSet<>();
        User currentUser = securityService.getCurrentUser();
        if (currentUser != null) {
            // As Program Director
            programs.addAll(programRepository.findAllByDirectorIdWithRelations(currentUser.getId()));
        }
        programSelect.setItems(programs);

        programSelect.setItemLabelGenerator(Program::getName);

        Select<User> sponsorSelect = new Select<>();
        sponsorSelect.setLabel("Sponsor");
        sponsorSelect.setItems(userService.findAllByRoles(List.of(Role.MANAGER, Role.ADMIN)));
        sponsorSelect.setItemLabelGenerator(User::getName);

        if (project != null) {
            nameField.setValue(project.getName());
            directorSelect.setValue(project.getDirector());
            programSelect.setValue(project.getProgram());
            sponsorSelect.setValue(project.getSponsor());
        }

        VerticalLayout dialogLayout = new VerticalLayout(nameField, directorSelect, programSelect, sponsorSelect);
        dialog.add(dialogLayout);

        Button saveButton = new Button("Guardar", e -> {
            if (nameField.isEmpty() || directorSelect.isEmpty() || programSelect.isEmpty()
                    || sponsorSelect.isEmpty()) {
                Notification.show("Por favor rellene todos los campos");
                return;
            }

            Project projectToSave = project == null ? new Project() : project;
            projectToSave.setName(nameField.getValue());
            projectToSave.setDirector(directorSelect.getValue());
            projectToSave.setProgram(programSelect.getValue());
            projectToSave.setSponsor(sponsorSelect.getValue());

            projectService.createOrUpdate(projectToSave);
            updateList();
            dialog.close();
            Notification.show("Proyecto guardado exitosamente");
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
