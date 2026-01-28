package com.example.examplefeature.ui;

import com.example.base.ui.MainLayout;
import com.example.program.Program;
import com.example.project.Project;
import com.example.project.ProjectService;
import com.example.security.SecurityService;
import com.example.user.User;
import com.example.user.UserService;
import com.example.user.UserRepository;
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

@Route(value = "project", layout = MainLayout.class)
@PageTitle("Detalle de Proyecto")
@RolesAllowed({ "ADMIN", "MANAGER", "USER" })
public class ProjectDetailView extends VerticalLayout implements HasUrlParameter<Long> {

    private final ProjectService projectService;
    private final UserService userService;
    private final SecurityService securityService;
    private final UserRepository userRepository;
    private Project currentProject;
    private final Grid<User> userGrid = new Grid<>(User.class, false);

    private TextField nameField;
    private Select<User> directorSelect;
    private Select<User> sponsorSelect;

    // Store original navigation data for breadcrumb
    private Long originalProgramId;
    private Long originalPortfolioId;
    private String originalPortfolioName;
    private String originalProgramName;

    public ProjectDetailView(ProjectService projectService, UserService userService,
            UserRepository userRepository, SecurityService securityService) {
        this.projectService = projectService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.securityService = securityService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }

    @Override
    public void setParameter(BeforeEvent event, Long projectId) {
        currentProject = projectService.get(projectId);

        if (currentProject == null) {
            Notification.show("Proyecto no encontrado");
            UI.getCurrent().navigate("proyectos");
            return;
        }

        // Store original navigation data for breadcrumb
        if (currentProject.getProgram() != null) {
            originalProgramId = currentProject.getProgram().getId();
            originalProgramName = currentProject.getProgram().getName();

            if (currentProject.getProgram().getPortfolio() != null) {
                originalPortfolioId = currentProject.getProgram().getPortfolio().getId();
                originalPortfolioName = currentProject.getProgram().getPortfolio().getName();
            }
        }

        removeAll();
        buildView();
    }

    private void buildView() {
        // Breadcrumb / Navigation
        HorizontalLayout breadcrumb = new HorizontalLayout();
        breadcrumb.setSpacing(false);
        breadcrumb.getStyle().set("font-size", "0.9em");

        Button backToPortfoliosButton = new Button("Portfolios", e -> {
            UI.getCurrent().navigate("portfolios");
        });
        backToPortfoliosButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        breadcrumb.add(backToPortfoliosButton);

        if (originalPortfolioId != null && originalPortfolioName != null) {
            breadcrumb.add(new com.vaadin.flow.component.html.Span(" > "));
            Button portfolioButton = new Button(originalPortfolioName, e -> {
                UI.getCurrent().navigate("portfolio/" + originalPortfolioId);
            });
            portfolioButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            breadcrumb.add(portfolioButton);
        }

        if (originalProgramId != null && originalProgramName != null) {
            breadcrumb.add(new com.vaadin.flow.component.html.Span(" > "));
            Button programButton = new Button(originalProgramName, e -> {
                UI.getCurrent().navigate("program/" + originalProgramId);
            });
            programButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            breadcrumb.add(programButton);
        }

        breadcrumb.add(new com.vaadin.flow.component.html.Span(" > "));
        breadcrumb.add(new com.vaadin.flow.component.html.Span(currentProject.getName()));

        add(breadcrumb);

        // Project Info Section
        add(new H2("Información del Proyecto"));

        nameField = new TextField("Nombre");
        nameField.setValue(currentProject.getName());
        nameField.setWidthFull();

        // Program as read-only text field (cannot be changed from this view)
        TextField programField = new TextField("Programa");
        programField
                .setValue(currentProject.getProgram() != null ? currentProject.getProgram().getName() : "Sin Programa");
        programField.setReadOnly(true);
        programField.setWidthFull();

        directorSelect = new Select<>();
        directorSelect.setLabel("Director");
        // Director debe ser solo rol USER
        directorSelect.setItems(userService.findAllByRoles(List.of(Role.USER)));
        directorSelect.setItemLabelGenerator(User::getName);
        directorSelect.setValue(currentProject.getDirector());
        directorSelect.setWidthFull();

        sponsorSelect = new Select<>();
        sponsorSelect.setLabel("Sponsor");
        // Sponsor debe ser ADMIN o MANAGER (GESTOR)
        sponsorSelect.setItems(userService.findAllByRoles(List.of(Role.MANAGER, Role.ADMIN)));
        sponsorSelect.setItemLabelGenerator(User::getName);
        sponsorSelect.setValue(currentProject.getSponsor());
        sponsorSelect.setWidthFull();

        Button saveButton = new Button("Guardar Cambios", e -> saveProject());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout formLayout = new HorizontalLayout(nameField, programField, directorSelect, sponsorSelect,
                saveButton);
        formLayout.setWidthFull();
        formLayout.setAlignItems(Alignment.END);
        add(formLayout);

        // Enforce strict editing: Only Program Director can edit Project Info
        boolean isProgramDirector = false;
        if (currentProject.getProgram() != null) {
            isProgramDirector = securityService.isProgramDirector(currentProject.getProgram().getId());
        }

        if (!isProgramDirector) {
            nameField.setReadOnly(true);
            directorSelect.setReadOnly(true);
            sponsorSelect.setReadOnly(true);
            saveButton.setVisible(false);
        }

        // Users Section
        add(new H3("Usuarios Asignados"));

        Button addUsersButton = new Button("Añadir Usuarios", e -> openAssignUsersDialog());
        addUsersButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add(addUsersButton);

        configureGrid();
        add(userGrid);
        updateUserList();
    }

    private void configureGrid() {
        userGrid.setSizeFull();
        userGrid.addColumn(User::getId).setHeader("ID").setWidth("100px");
        userGrid.addColumn(User::getName).setHeader("Nombre");
        userGrid.addColumn(User::getUvus).setHeader("UVUS");

        // Columna de Eliminar
        userGrid.addComponentColumn(user -> {
            Button removeButton = new Button("Eliminar", e -> unassignUser(user));
            removeButton.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_SMALL,
                    com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR);
            return removeButton;
        }).setHeader("").setWidth("100px").setFlexGrow(0);

        // Set fixed height to show at least 5 rows with scrolling
        userGrid.setHeight("350px");
        userGrid.setPageSize(10);
    }

    private void saveProject() {
        if (nameField.isEmpty() || directorSelect.isEmpty()
                || sponsorSelect.isEmpty()) {
            Notification.show("Por favor rellene todos los campos");
            return;
        }

        currentProject.setName(nameField.getValue());
        // Program cannot be changed from this view
        currentProject.setDirector(directorSelect.getValue());
        currentProject.setSponsor(sponsorSelect.getValue());

        projectService.createOrUpdate(currentProject);
        Notification.show("Proyecto actualizado exitosamente");
    }

    private void updateUserList() {
        List<User> assignedUsers = userService.findByProject(currentProject.getId());
        userGrid.setItems(assignedUsers);
    }

    private void openAssignUsersDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Asignar Usuarios");
        dialog.setWidth("600px");

        // Search field for UVUS
        TextField searchField = new TextField("Buscar por UVUS");
        searchField.setPlaceholder("Escribe UVUS para filtrar...");
        searchField.setWidthFull();

        // Grid with multi-selection
        Grid<User> availableUsersGrid = new Grid<>(User.class, false);
        availableUsersGrid.addColumn(User::getUvus).setHeader("UVUS").setWidth("150px");
        availableUsersGrid.addColumn(User::getName).setHeader("Nombre");
        availableUsersGrid.addColumn(user -> user.getRole() != null ? user.getRole().name() : "").setHeader("Rol")
                .setWidth("120px");

        availableUsersGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        availableUsersGrid.setHeight("400px");

        // Load available users
        List<User> availableUsers = userService.findAvailableForProject(currentProject.getId());
        availableUsersGrid.setItems(availableUsers);

        // Search filter
        searchField.addValueChangeListener(e -> {
            String filter = e.getValue().toLowerCase().trim();
            if (filter.isEmpty()) {
                availableUsersGrid.setItems(availableUsers);
            } else {
                List<User> filtered = availableUsers.stream()
                        .filter(user -> user.getUvus().toLowerCase().contains(filter))
                        .toList();
                availableUsersGrid.setItems(filtered);
            }
        });

        VerticalLayout dialogLayout = new VerticalLayout(searchField, availableUsersGrid);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);
        dialog.add(dialogLayout);

        Button saveButton = new Button("Asignar Seleccionados", e -> {
            var selectedUsers = availableUsersGrid.getSelectedItems();
            if (selectedUsers.isEmpty()) {
                Notification.show("Por favor seleccione al menos un usuario");
                return;
            }

            // Assign all selected users to this project
            for (User user : selectedUsers) {
                user.setProject(currentProject);
                userService.createOrUpdate(user);
            }

            updateUserList();
            dialog.close();
            Notification.show(selectedUsers.size() + " usuario(s) asignado(s) exitosamente");
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancelar", e -> dialog.close());

        dialog.getFooter().add(cancelButton);
        dialog.getFooter().add(saveButton);

        dialog.open();
    }

    private void unassignUser(User user) {
        user.setProject(null);
        userService.createOrUpdate(user);
        updateUserList();
        Notification.show("Usuario desasignado exitosamente");
    }
}
