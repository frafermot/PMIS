package com.example.examplefeature.ui;

import com.example.base.ui.MainLayout;
import com.example.communication.Ccc;
import com.example.communication.CccService;
import com.example.document.Document;
import com.example.document.DocumentService;
import com.example.document.DocumentStatus;
import com.example.document.DocumentType;
import com.example.project.Project;
import com.example.project.ProjectService;
import com.example.security.SecurityService;
import com.example.user.Role;
import com.example.user.User;
import com.example.user.UserRepository;
import com.example.user.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Route(value = "proyecto", layout = MainLayout.class)
@PageTitle("Detalle de Proyecto")
@RolesAllowed({ "ADMIN", "MANAGER", "USER" })
public class ProjectDetailView extends VerticalLayout implements HasUrlParameter<Long> {

    private final ProjectService projectService;
    private final UserService userService;
    private final SecurityService securityService;
    private final UserRepository userRepository;
    private final CccService cccService;
    private final DocumentService documentService;
    private Project currentProject;
    private final Grid<User> userGrid = new Grid<>(User.class, false);

    private TextField nameField;
    private Select<User> directorSelect;
    private Select<User> sponsorSelect;

    // Breadcrumb data
    private Long originalProgramId;
    private Long originalPortfolioId;
    private String originalPortfolioName;
    private String originalProgramName;

    public ProjectDetailView(ProjectService projectService, UserService userService,
            UserRepository userRepository, SecurityService securityService, CccService cccService,
            DocumentService documentService) {
        this.projectService = projectService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.securityService = securityService;
        this.cccService = cccService;
        this.documentService = documentService;

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
        // ── Breadcrumb ────────────────────────────────────────────────────────
        HorizontalLayout breadcrumb = new HorizontalLayout();
        breadcrumb.setSpacing(false);
        breadcrumb.getStyle().set("font-size", "0.9em");

        Button backToPortfoliosButton = new Button("Portfolios",
                e -> UI.getCurrent().navigate("portfolios"));
        backToPortfoliosButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        breadcrumb.add(backToPortfoliosButton);

        if (originalPortfolioId != null && originalPortfolioName != null) {
            breadcrumb.add(new Span(" > "));
            Button portfolioButton = new Button(originalPortfolioName,
                    e -> UI.getCurrent().navigate("portfolio/" + originalPortfolioId));
            portfolioButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            breadcrumb.add(portfolioButton);
        }
        if (originalProgramId != null && originalProgramName != null) {
            breadcrumb.add(new Span(" > "));
            Button programButton = new Button(originalProgramName,
                    e -> UI.getCurrent().navigate("program/" + originalProgramId));
            programButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            breadcrumb.add(programButton);
        }
        breadcrumb.add(new Span(" > "));
        breadcrumb.add(new Span(currentProject.getName()));
        add(breadcrumb);

        // ── Project info ──────────────────────────────────────────────────────
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setAlignItems(Alignment.CENTER);
        headerLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);

        H2 title = new H2("Información del Proyecto");
        headerLayout.add(title);

        if (securityService.isProjectDirectorOrSponsor(currentProject.getId())) {
            Button cccButton = new Button("Control de Comité de Cambios", e -> {
                Optional<Ccc> cccOptional = cccService.getCccByProject(currentProject.getId());
                Ccc ccc = cccOptional.orElseGet(() -> cccService.createCccForProject(currentProject));
                UI.getCurrent().navigate("ccc/" + ccc.getId());
            });
            cccButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            headerLayout.add(cccButton);
        }
        add(headerLayout);

        nameField = new TextField("Nombre");
        nameField.setValue(currentProject.getName());
        nameField.setWidthFull();

        TextField programField = new TextField("Programa");
        programField.setValue(
                currentProject.getProgram() != null ? currentProject.getProgram().getName() : "Sin Programa");
        programField.setReadOnly(true);
        programField.setWidthFull();

        directorSelect = new Select<>();
        directorSelect.setLabel("Director");
        directorSelect.setItems(userService.findAllByRoles(List.of(Role.USER)));
        directorSelect.setItemLabelGenerator(User::getName);
        directorSelect.setValue(currentProject.getDirector());
        directorSelect.setWidthFull();

        sponsorSelect = new Select<>();
        sponsorSelect.setLabel("Sponsor");
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

        // ── Permissions ───────────────────────────────────────────────────────
        boolean isProgramDirector = currentProject.getProgram() != null
                && securityService.isProgramDirector(currentProject.getProgram().getId());

        User currentUser = securityService.getCurrentUser();
        boolean isSponsor = currentProject.getSponsor() != null && currentUser != null
                && currentProject.getSponsor().getId().equals(currentUser.getId());
        boolean isProjectDirector = currentProject.getDirector() != null && currentUser != null
                && currentProject.getDirector().getId().equals(currentUser.getId());
        boolean isSystemAdmin = securityService.isAdmin();
        boolean canEditProjectInfo = isSystemAdmin || isProgramDirector;
        boolean canAssignDirector = canEditProjectInfo || isSponsor;

        nameField.setReadOnly(!canEditProjectInfo);
        sponsorSelect.setReadOnly(!canEditProjectInfo);

        if (canAssignDirector) {
            directorSelect.setReadOnly(false);
            List<User> projectUsers = userService.findByProject(currentProject.getId());
            if (currentProject.getDirector() != null && !projectUsers.contains(currentProject.getDirector())) {
                projectUsers.add(currentProject.getDirector());
            }
            directorSelect.setItems(projectUsers);
            directorSelect.setValue(currentProject.getDirector());
        } else {
            directorSelect.setReadOnly(true);
        }

        if (!canEditProjectInfo && !canAssignDirector) {
            saveButton.setVisible(false);
        }

        // ── Users Section (collapsible, starts CLOSED) ────────────────────────
        boolean canManageUsers = isSystemAdmin || isSponsor || isProjectDirector;
        VerticalLayout usersContent = buildUsersContent(canManageUsers);

        Details usersDetails = new Details("Usuarios Asignados", usersContent);
        usersDetails.setOpened(false); // starts closed
        usersDetails.setWidthFull();
        add(usersDetails);

        // ── Documents Section ─────────────────────────────────────────────────
        add(new H3("Documentos del Proyecto"));

        // Seed documents if not yet done (idempotent)
        documentService.initDocumentsForProject(currentProject);

        Map<DocumentType, Document> docMap = documentService.getProjectDocumentMap(currentProject.getId());

        // Group by phase in fixed order
        String[] phases = { "inicio", "planificacion", "ejecucion", "cierre" };
        for (String phase : phases) {
            List<DocumentType> typesInPhase = Arrays.stream(DocumentType.values())
                    .filter(t -> t.getPhase().equals(phase))
                    .collect(Collectors.toList());

            VerticalLayout phaseContent = buildPhaseGrid(typesInPhase, docMap, isProgramDirector, isSponsor);

            // Use the label of the first type's phase (they all share the same phase label)
            String phaseLabel = typesInPhase.get(0).getPhaseLabel();
            Details phaseDetails = new Details(phaseLabel, phaseContent);
            phaseDetails.setOpened(true); // phase groups start open
            phaseDetails.setWidthFull();
            add(phaseDetails);
        }
    }

    // ── Users content ─────────────────────────────────────────────────────────

    private VerticalLayout buildUsersContent(boolean canManageUsers) {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        if (canManageUsers) {
            Button addUsersButton = new Button("Añadir Usuarios", e -> openAssignUsersDialog());
            addUsersButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            layout.add(addUsersButton);
        }

        configureUserGrid(canManageUsers);
        layout.add(userGrid);
        updateUserList();
        return layout;
    }

    private void configureUserGrid(boolean canManageUsers) {
        userGrid.setSizeFull();
        userGrid.addColumn(User::getId).setHeader("ID").setWidth("60px").setFlexGrow(0);
        userGrid.addColumn(User::getName).setHeader("Nombre").setFlexGrow(2);
        userGrid.addColumn(User::getUvus).setHeader("UVUS").setFlexGrow(1);

        if (canManageUsers) {
            userGrid.addComponentColumn(user -> {
                Button removeButton = new Button("Eliminar", e -> unassignUser(user));
                removeButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
                return removeButton;
            }).setHeader("").setWidth("100px").setFlexGrow(0);
        }
        userGrid.setHeight("250px");
        userGrid.setPageSize(10);
    }

    // ── Documents grid per phase ──────────────────────────────────────────────

    /**
     * Data record used to drive the document grid rows.
     */
    private record DocRow(DocumentType type, Document document) {
    }

    private VerticalLayout buildPhaseGrid(List<DocumentType> types, Map<DocumentType, Document> docMap,
            boolean isProgramDirector, boolean isSponsor) {

        List<DocRow> rows = types.stream()
                .map(t -> new DocRow(t, docMap.get(t)))
                .collect(Collectors.toList());

        Grid<DocRow> grid = new Grid<>();
        grid.setWidthFull();
        grid.setAllRowsVisible(true);

        // Documento column
        grid.addColumn(r -> r.type().getLabel()).setHeader("Documento").setFlexGrow(2);

        // Estado column with badge-like styling
        grid.addComponentColumn(r -> {
            DocumentStatus status = r.document() != null ? r.document().getStatus() : DocumentStatus.POR_CREAR;
            Span badge = new Span(status.getLabel());
            badge.getStyle()
                    .set("font-size", "0.8em")
                    .set("padding", "2px 8px")
                    .set("border-radius", "12px")
                    .set("font-weight", "600")
                    .set("background-color", statusColor(status))
                    .set("color", "white");
            return badge;
        }).setHeader("Estado").setFlexGrow(1);

        // Actions column
        grid.addComponentColumn(r -> buildActionsButton(r, isProgramDirector, isSponsor))
                .setHeader("Acciones").setWidth("130px").setFlexGrow(0);

        grid.setItems(rows);

        VerticalLayout layout = new VerticalLayout(grid);
        layout.setPadding(false);
        layout.setSpacing(false);
        return layout;
    }

    private Button buildActionsButton(DocRow row, boolean isProgramDirector, boolean isSponsor) {
        DocumentStatus status = row.document() != null ? row.document().getStatus() : DocumentStatus.POR_CREAR;
        boolean isPorCrear = status == DocumentStatus.POR_CREAR;

        Button actionsBtn = new Button("Acciones ▾");
        actionsBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);

        ContextMenu menu = new ContextMenu(actionsBtn);
        menu.setOpenOnClick(true);

        // ── Crear ──
        var crearItem = menu.addItem("Crear", e -> handleCrear(row.type()));
        crearItem.setEnabled(isPorCrear && !isProgramDirector && !isSponsor);

        // ── Modificar ──
        var modificarItem = menu.addItem("Modificar", e -> handleModificar(row.document()));
        modificarItem.setEnabled(!isPorCrear && row.document() != null);

        // ── Enviar (no-op) ──
        var enviarItem = menu.addItem("Enviar", e -> Notification.show("Funcionalidad de envío no disponible aún"));
        enviarItem.setEnabled(false);

        // ── Firmar (no-op) ──
        var firmarItem = menu.addItem("Firmar", e -> Notification.show("Funcionalidad de firma no disponible aún"));
        firmarItem.setEnabled(false);

        // ── Imprimir (no-op) ──
        var imprimirItem = menu.addItem("Imprimir", e -> Notification.show("Descarga PDF no disponible aún"));
        imprimirItem.setEnabled(false);

        return actionsBtn;
    }

    private void handleCrear(DocumentType type) {
        try {
            Document created = documentService.createDocument(currentProject.getId(), type);
            UI.getCurrent().navigate("document/" + created.getId());
        } catch (Exception ex) {
            Notification.show("Error al crear el documento: " + ex.getMessage(), 5000,
                    Notification.Position.MIDDLE);
        }
    }

    private void handleModificar(Document document) {
        if (document != null) {
            UI.getCurrent().navigate("document/" + document.getId());
        }
    }

    private String statusColor(DocumentStatus status) {
        return switch (status) {
            case POR_CREAR -> "#9e9e9e";
            case EN_PROCESO -> "#1976d2";
            case FIRMADO -> "#388e3c";
            case ENVIADO -> "#f57c00";
            case VALORADO -> "#7b1fa2";
        };
    }

    // ── Project save ──────────────────────────────────────────────────────────

    private void saveProject() {
        if (nameField.isEmpty() || sponsorSelect.isEmpty()) {
            Notification.show("Por favor rellene Nombre y Sponsor");
            return;
        }
        currentProject.setName(nameField.getValue());
        currentProject.setDirector(directorSelect.getValue());
        currentProject.setSponsor(sponsorSelect.getValue());
        projectService.createOrUpdate(currentProject);
        Notification.show("Proyecto actualizado exitosamente");
    }

    // ── User management ───────────────────────────────────────────────────────

    private void updateUserList() {
        List<User> assignedUsers = userService.findByProject(currentProject.getId());
        userGrid.setItems(assignedUsers);
    }

    private void updateDirectorList() {
        List<User> projectUsers = userService.findByProject(currentProject.getId());
        if (currentProject.getDirector() != null && !projectUsers.contains(currentProject.getDirector())) {
            projectUsers.add(currentProject.getDirector());
        }
        User currentSelection = directorSelect.getValue();
        directorSelect.setItems(projectUsers);
        if (currentSelection != null && projectUsers.contains(currentSelection)) {
            directorSelect.setValue(currentSelection);
        }
    }

    private void openAssignUsersDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Asignar Usuarios");
        dialog.setWidth("600px");

        TextField searchField = new TextField("Buscar por UVUS");
        searchField.setPlaceholder("Escribe UVUS para filtrar...");
        searchField.setWidthFull();

        Grid<User> availableUsersGrid = new Grid<>(User.class, false);
        availableUsersGrid.addColumn(User::getUvus).setHeader("UVUS").setWidth("150px");
        availableUsersGrid.addColumn(User::getName).setHeader("Nombre");
        availableUsersGrid.addColumn(user -> user.getRole() != null ? user.getRole().name() : "")
                .setHeader("Rol").setWidth("120px");
        availableUsersGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        availableUsersGrid.setHeight("400px");

        List<User> availableUsers = userService.findAvailableForProject(currentProject.getId());
        availableUsersGrid.setItems(availableUsers);

        searchField.addValueChangeListener(e -> {
            String filter = e.getValue().toLowerCase().trim();
            if (filter.isEmpty()) {
                availableUsersGrid.setItems(availableUsers);
            } else {
                availableUsersGrid.setItems(
                        availableUsers.stream()
                                .filter(user -> user.getUvus().toLowerCase().contains(filter))
                                .toList());
            }
        });

        VerticalLayout dialogLayout = new VerticalLayout(searchField, availableUsersGrid);
        dialogLayout.setPadding(false);
        dialog.add(dialogLayout);

        Button saveAssignBtn = new Button("Asignar Seleccionados", e -> {
            var selectedUsers = availableUsersGrid.getSelectedItems();
            if (selectedUsers.isEmpty()) {
                Notification.show("Por favor seleccione al menos un usuario");
                return;
            }
            for (User user : selectedUsers) {
                user.setProject(currentProject);
                userService.createOrUpdate(user);
            }
            updateUserList();
            updateDirectorList();
            dialog.close();
            Notification.show(selectedUsers.size() + " usuario(s) asignado(s) exitosamente");
        });
        saveAssignBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancelar", e -> dialog.close());
        dialog.getFooter().add(cancelButton);
        dialog.getFooter().add(saveAssignBtn);
        dialog.open();
    }

    private void unassignUser(User user) {
        user.setProject(null);
        userService.createOrUpdate(user);
        updateUserList();
        updateDirectorList();
        Notification.show("Usuario desasignado exitosamente");
    }
}
