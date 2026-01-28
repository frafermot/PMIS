package com.example.examplefeature.ui;

import com.example.base.ui.MainLayout;
import com.example.program.Program;
import com.example.program.ProgramService;
import com.example.project.Project;
import com.example.project.ProjectService;
import com.example.user.User;
import com.example.user.UserService;
import com.example.user.Role;
import com.example.security.SecurityService;
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

@Route(value = "program", layout = MainLayout.class)
@PageTitle("Detalle de Programa")
@RolesAllowed({ "ADMIN", "MANAGER" })
public class ProgramDetailView extends VerticalLayout implements HasUrlParameter<Long> {

    private final ProgramService programService;
    private final ProjectService projectService;
    private final UserService userService;
    private final SecurityService securityService;
    private Program currentProgram;
    private final Grid<Project> projectGrid = new Grid<>(Project.class, false);

    private TextField nameField;
    private Select<User> directorSelect;

    public ProgramDetailView(ProgramService programService,
            ProjectService projectService, UserService userService,
            SecurityService securityService) {
        this.programService = programService;
        this.projectService = projectService;
        this.userService = userService;
        this.securityService = securityService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }

    @Override
    public void setParameter(BeforeEvent event, Long programId) {
        currentProgram = programService.get(programId);

        if (currentProgram == null) {
            Notification.show("Programa no encontrado");
            UI.getCurrent().navigate("programas");
            return;
        }

        removeAll();
        buildView();
    }

    private void buildView() {
        // Breadcrumb / Navigation
        HorizontalLayout breadcrumb = new HorizontalLayout();
        Button backToPortfoliosButton = new Button("Portfolios", e -> {
            UI.getCurrent().navigate("portfolios");
        });
        backToPortfoliosButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

        Button backToPortfolioButton = new Button(
                currentProgram.getPortfolio() != null ? currentProgram.getPortfolio().getName() : "Portfolio", e -> {
                    if (currentProgram.getPortfolio() != null) {
                        UI.getCurrent().navigate("portfolio/" + currentProgram.getPortfolio().getId());
                    }
                });
        backToPortfolioButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

        breadcrumb.add(backToPortfoliosButton, new com.vaadin.flow.component.html.Span(" > "), backToPortfolioButton);
        add(breadcrumb);

        // Program Info Section
        add(new H2("Información del Programa"));

        nameField = new TextField("Nombre");
        nameField.setValue(currentProgram.getName());
        nameField.setWidthFull();

        // Portfolio as read-only text field (cannot be changed from this view)
        TextField portfolioField = new TextField("Portfolio");
        portfolioField.setValue(
                currentProgram.getPortfolio() != null ? currentProgram.getPortfolio().getName() : "Sin portfolio");
        portfolioField.setReadOnly(true);
        portfolioField.setWidthFull();

        directorSelect = new Select<>();
        directorSelect.setLabel("Director");
        directorSelect.setItems(userService.findAllByRoles(List.of(Role.MANAGER, Role.ADMIN)));
        directorSelect.setItemLabelGenerator(User::getName);
        directorSelect.setValue(currentProgram.getDirector());
        directorSelect.setWidthFull();

        Button saveButton = new Button("Guardar Cambios", e -> saveProgram());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout formLayout = new HorizontalLayout(nameField, portfolioField, directorSelect, saveButton);
        formLayout.setWidthFull();
        formLayout.setAlignItems(Alignment.END);
        add(formLayout);

        // Enforce strict editing: Only Portfolio Director can edit Program Info
        boolean isPortfolioDirector = false;
        if (currentProgram.getPortfolio() != null) {
            // Check if current user is director of the portfolio
            User currentUser = securityService.getCurrentUser();
            isPortfolioDirector = currentUser != null && currentProgram.getPortfolio().getDirector() != null &&
                    currentProgram.getPortfolio().getDirector().getId().equals(currentUser.getId());
        }

        if (!isPortfolioDirector) {
            nameField.setReadOnly(true);
            directorSelect.setReadOnly(true);
            saveButton.setVisible(false);
        }

        // Projects Section
        add(new H3("Proyectos de este Programa"));

        configureGrid();

        // Only show Add Project button if current user is the Program Director
        User currentUser = securityService.getCurrentUser();
        boolean isProgramDirector = currentUser != null && currentProgram.getDirector() != null &&
                currentProgram.getDirector().getId().equals(currentUser.getId());

        if (isProgramDirector) {
            Button addProjectButton = new Button("Añadir Proyecto", e -> openCreateProjectDialog());
            addProjectButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            add(addProjectButton);
        }

        add(projectGrid);
        updateProjectList();
    }

    private void configureGrid() {
        projectGrid.setSizeFull();
        projectGrid.addColumn(Project::getId).setHeader("ID").setWidth("100px");
        projectGrid.addColumn(Project::getName).setHeader("Nombre");
        projectGrid
                .addColumn(project -> project.getDirector() != null ? project.getDirector().getName() : "Sin Director")
                .setHeader("Director");
        projectGrid.addColumn(project -> project.getSponsor() != null ? project.getSponsor().getName() : "Sin Sponsor")
                .setHeader("Sponsor");

        // Columna de Editar
        projectGrid.addComponentColumn(project -> {
            Button editButton = new Button("Editar", e -> openEditProjectDialog(project));
            editButton.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_SMALL,
                    com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);

            // Solo mostrar botón si es el director del programa
            User currentUser = securityService.getCurrentUser();
            boolean canEdit = currentUser != null && currentProgram.getDirector() != null &&
                    currentProgram.getDirector().getId().equals(currentUser.getId());
            editButton.setVisible(canEdit);

            return editButton;
        }).setHeader("").setWidth("80px").setFlexGrow(0);

        // Columna de Borrar
        projectGrid.addComponentColumn(project -> {
            Button deleteButton = new Button("Borrar", e -> deleteProject(project));
            deleteButton.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_SMALL,
                    com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR);

            // Solo mostrar botón si es el director del programa
            User currentUser = securityService.getCurrentUser();
            boolean canDelete = currentUser != null && currentProgram.getDirector() != null &&
                    currentProgram.getDirector().getId().equals(currentUser.getId());
            deleteButton.setVisible(canDelete);

            return deleteButton;
        }).setHeader("").setWidth("80px").setFlexGrow(0);

        projectGrid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                UI.getCurrent().navigate("project/" + event.getValue().getId());
            }
        });
    }

    private void saveProgram() {
        if (nameField.isEmpty() || directorSelect.isEmpty()) {
            Notification.show("Por favor rellene todos los campos");
            return;
        }

        currentProgram.setName(nameField.getValue());
        // Portfolio cannot be changed from this view
        currentProgram.setDirector(directorSelect.getValue());

        programService.createOrUpdate(currentProgram);
        Notification.show("Programa actualizado exitosamente");
    }

    private void openCreateProjectDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Nuevo Proyecto");

        TextField projectNameField = new TextField("Nombre");
        Select<User> projectSponsorSelect = new Select<>();
        projectSponsorSelect.setLabel("Sponsor");
        projectSponsorSelect.setItems(userService.findAllByRoles(List.of(Role.MANAGER, Role.ADMIN)));
        projectSponsorSelect.setItemLabelGenerator(User::getName);

        VerticalLayout dialogLayout = new VerticalLayout(projectNameField, projectSponsorSelect);
        dialog.add(dialogLayout);

        Button saveButton = new Button("Guardar", e -> {
            if (projectNameField.isEmpty() || projectSponsorSelect.isEmpty()) {
                Notification.show("Por favor rellene todos los campos");
                return;
            }

            Project newProject = new Project();
            newProject.setName(projectNameField.getValue());
            // Director not set on creation
            newProject.setSponsor(projectSponsorSelect.getValue());
            newProject.setProgram(currentProgram);

            projectService.createOrUpdate(newProject);
            updateProjectList();
            dialog.close();
            Notification.show("Proyecto creado exitosamente");
        });

        Button cancelButton = new Button("Cancelar", e -> dialog.close());

        dialog.getFooter().add(cancelButton);
        dialog.getFooter().add(saveButton);

        dialog.open();
    }

    private void updateProjectList() {
        projectGrid.setItems(projectService.getByProgramId(currentProgram.getId()));
    }

    private void openEditProjectDialog(Project project) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Editar Proyecto");

        TextField projectNameField = new TextField("Nombre");
        projectNameField.setValue(project.getName());

        Select<User> projectDirectorSelect = new Select<>();
        projectDirectorSelect.setLabel("Director");
        // Director debe ser solo rol USER
        projectDirectorSelect.setItems(userService.findAllByRoles(List.of(Role.USER)));
        projectDirectorSelect.setItemLabelGenerator(User::getName);
        projectDirectorSelect.setValue(project.getDirector());

        Select<User> projectSponsorSelect = new Select<>();
        projectSponsorSelect.setLabel("Sponsor");
        // Sponsor debe ser ADMIN o MANAGER (GESTOR)
        projectSponsorSelect.setItems(userService.findAllByRoles(List.of(Role.MANAGER, Role.ADMIN)));
        projectSponsorSelect.setItemLabelGenerator(User::getName);
        projectSponsorSelect.setValue(project.getSponsor());

        VerticalLayout dialogLayout = new VerticalLayout(projectNameField, projectDirectorSelect, projectSponsorSelect);
        dialog.add(dialogLayout);

        Button saveButton = new Button("Guardar", e -> {
            if (projectNameField.isEmpty() || projectDirectorSelect.isEmpty() || projectSponsorSelect.isEmpty()) {
                Notification.show("Por favor rellene todos los campos");
                return;
            }

            project.setName(projectNameField.getValue());
            project.setDirector(projectDirectorSelect.getValue());
            project.setSponsor(projectSponsorSelect.getValue());

            projectService.createOrUpdate(project);
            updateProjectList();
            dialog.close();
            Notification.show("Proyecto actualizado exitosamente");
        });

        Button cancelButton = new Button("Cancelar", e -> dialog.close());

        dialog.getFooter().add(cancelButton);
        dialog.getFooter().add(saveButton);

        dialog.open();
    }

    private void deleteProject(Project project) {
        projectService.delete(project.getId());
        updateProjectList();
        Notification.show("Proyecto eliminado exitosamente");
    }
}
