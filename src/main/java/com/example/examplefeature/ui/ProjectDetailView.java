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
    private final CccService cccService;
    private final DocumentService documentService;
    private Project currentProject;
    private final Grid<User> userGrid = new Grid<>(User.class, false);

    // Permissions computed once in buildView
    private boolean isProgramDirector;
    private boolean isSponsor;

    private TextField nameField;
    private Select<User> directorSelect;
    private Select<User> sponsorSelect;

    // Document view: the container that holds whichever mode is active
    private VerticalLayout docViewContainer;
    // Track active mode: true = by-phase, false = by-document
    private boolean viewByPhase = true;
    // Buttons for toggling (kept as fields to update their styles)
    private Button btnByPhase;
    private Button btnByDocument;

    // Breadcrumb data
    private Long originalProgramId;
    private Long originalPortfolioId;
    private String originalPortfolioName;
    private String originalProgramName;

    public ProjectDetailView(ProjectService projectService, UserService userService,
            SecurityService securityService, CccService cccService,
            DocumentService documentService) {
        this.projectService = projectService;
        this.userService = userService;
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

    // ─── Main build ───────────────────────────────────────────────────────────

    private void buildView() {
        buildBreadcrumb();
        buildProjectInfoSection();
        buildUsersSection();
        buildDocumentsSection();
    }

    // ─── Breadcrumb ───────────────────────────────────────────────────────────

    private void buildBreadcrumb() {
        HorizontalLayout breadcrumb = new HorizontalLayout();
        breadcrumb.setSpacing(false);
        breadcrumb.getStyle().set("font-size", "0.9em");

        Button backBtn = new Button("Portfolios", e -> UI.getCurrent().navigate("portfolios"));
        backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        breadcrumb.add(backBtn);

        if (originalPortfolioId != null && originalPortfolioName != null) {
            breadcrumb.add(new Span(" > "));
            Button b = new Button(originalPortfolioName,
                    e -> UI.getCurrent().navigate("portfolio/" + originalPortfolioId));
            b.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            breadcrumb.add(b);
        }
        if (originalProgramId != null && originalProgramName != null) {
            breadcrumb.add(new Span(" > "));
            Button b = new Button(originalProgramName,
                    e -> UI.getCurrent().navigate("program/" + originalProgramId));
            b.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            breadcrumb.add(b);
        }
        breadcrumb.add(new Span(" > " + currentProject.getName()));
        add(breadcrumb);
    }

    // ─── Project info ────────────────────────────────────────────────────────

    private void buildProjectInfoSection() {
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setAlignItems(Alignment.CENTER);
        headerLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        headerLayout.add(new H2("Información del Proyecto"));

        if (securityService.isProjectDirectorOrSponsor(currentProject.getId())) {
            Button cccButton = new Button("Control de Comité de Cambios", e -> {
                Optional<Ccc> cccOpt = cccService.getCccByProject(currentProject.getId());
                Ccc ccc = cccOpt.orElseGet(() -> cccService.createCccForProject(currentProject));
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

        // Compute permissions
        isProgramDirector = currentProject.getProgram() != null
                && securityService.isProgramDirector(currentProject.getProgram().getId());
        User cu = securityService.getCurrentUser();
        isSponsor = currentProject.getSponsor() != null && cu != null
                && currentProject.getSponsor().getId().equals(cu.getId());
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
    }

    // ─── Users section (collapsible, closed by default) ───────────────────────

    private void buildUsersSection() {
        User cu = securityService.getCurrentUser();
        boolean isSystemAdmin = securityService.isAdmin(); // Re-declared here for scope, or assume it's a field/passed.
                                                           // Assuming it's a local variable for this method.
        boolean isProjectDirector = currentProject.getDirector() != null && cu != null
                && currentProject.getDirector().getId().equals(cu.getId());
        boolean canManageUsers = isSystemAdmin || isSponsor || isProjectDirector;

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        if (canManageUsers) {
            Button addBtn = new Button("Añadir Usuarios", e -> openAssignUsersDialog());
            addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            content.add(addBtn);
        }
        configureUserGrid(canManageUsers);
        updateUserList();
        content.add(userGrid);

        Details details = new Details("Usuarios Asignados", content);
        details.setOpened(false); // starts closed
        details.setWidthFull();
        add(details);
    }

    // ─── Documents section ───────────────────────────────────────────────────

    private void buildDocumentsSection() {
        // Seed documents (idempotent)
        documentService.initDocumentsForProject(currentProject);

        // ── Section header ──
        H3 heading = new H3("Documentos del Proyecto");
        add(heading);

        // ── Toggle buttons ──────────────────────────────────────────────────
        btnByPhase = new Button("Por Fases", e -> switchDocView(true));
        btnByDocument = new Button("Por Documentos", e -> switchDocView(false));

        styleToggleButton(btnByPhase, true); // active by default
        styleToggleButton(btnByDocument, false);

        HorizontalLayout toggleBar = new HorizontalLayout(btnByPhase, btnByDocument);
        toggleBar.setSpacing(true);
        toggleBar.setAlignItems(Alignment.CENTER);
        add(toggleBar);

        // ── Content container ────────────────────────────────────────────────
        docViewContainer = new VerticalLayout();
        docViewContainer.setPadding(false);
        docViewContainer.setSpacing(true);
        docViewContainer.setWidthFull();
        add(docViewContainer);

        // Render initial view
        renderDocView();
    }

    private void switchDocView(boolean byPhase) {
        if (viewByPhase == byPhase)
            return;
        viewByPhase = byPhase;
        styleToggleButton(btnByPhase, byPhase);
        styleToggleButton(btnByDocument, !byPhase);
        renderDocView();
    }

    private void styleToggleButton(Button btn, boolean active) {
        btn.getStyle().remove("background-color");
        btn.getStyle().remove("color");
        btn.getStyle().remove("border");
        btn.getStyle().remove("font-weight");
        if (active) {
            btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            btn.removeThemeVariants(ButtonVariant.LUMO_TERTIARY);
        } else {
            btn.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        }
    }

    private void renderDocView() {
        docViewContainer.removeAll();
        Map<DocumentType, Document> docMap = documentService.getProjectDocumentMap(currentProject.getId());

        if (viewByPhase) {
            renderByPhase(docMap);
        } else {
            renderByDocument(docMap);
        }
    }

    // ── VIEW: By Phase ────────────────────────────────────────────────────────

    private void renderByPhase(Map<DocumentType, Document> docMap) {
        String[] phases = { "inicio", "planificacion", "ejecucion", "cierre" };
        for (String phase : phases) {
            List<DocumentType> typesInPhase = Arrays.stream(DocumentType.values())
                    .filter(t -> t.getPhase().equals(phase))
                    .collect(Collectors.toList());

            String phaseLabel = typesInPhase.get(0).getPhaseLabel();
            VerticalLayout phaseContent = buildDocGrid(typesInPhase, docMap);

            Details phaseDetails = new Details(phaseLabel, phaseContent);
            phaseDetails.setOpened(true);
            phaseDetails.setWidthFull();
            docViewContainer.add(phaseDetails);
        }
    }

    // ── VIEW: By Document (flat grid ordered alphabetically) ──────────────────

    private void renderByDocument(Map<DocumentType, Document> docMap) {
        List<DocumentType> allTypes = Arrays.stream(DocumentType.values())
                .sorted((a, b) -> a.getLabel().compareToIgnoreCase(b.getLabel()))
                .collect(Collectors.toList());

        VerticalLayout grid = buildDocGrid(allTypes, docMap);
        docViewContainer.add(grid);
    }

    // ── Shared grid builder ───────────────────────────────────────────────────

    private record DocRow(DocumentType type, Document document) {
    }

    private VerticalLayout buildDocGrid(List<DocumentType> types, Map<DocumentType, Document> docMap) {
        List<DocRow> rows = types.stream()
                .map(t -> new DocRow(t, docMap.get(t)))
                .collect(Collectors.toList());

        Grid<DocRow> grid = new Grid<>();
        grid.setWidthFull();
        grid.setAllRowsVisible(true);

        // Document name column
        grid.addColumn(r -> r.type().getLabel())
                .setHeader("Documento")
                .setFlexGrow(2);

        // Phase column (useful in the "by document" flat view)
        if (!viewByPhase) {
            grid.addColumn(r -> r.type().getPhaseLabel())
                    .setHeader("Fase")
                    .setWidth("130px")
                    .setFlexGrow(0);
        }

        // Status badge
        grid.addComponentColumn(r -> {
            DocumentStatus status = r.document() != null ? r.document().getStatus() : DocumentStatus.POR_CREAR;
            Span badge = new Span(status.getLabel());
            badge.getStyle()
                    .set("font-size", "0.8em")
                    .set("padding", "2px 10px")
                    .set("border-radius", "12px")
                    .set("font-weight", "600")
                    .set("background-color", statusColor(status))
                    .set("color", "white");
            return badge;
        }).setHeader("Estado").setWidth("140px").setFlexGrow(0);

        // Valoración column — visible to all, shows dash when not rated
        grid.addComponentColumn(r -> {
            Double rating = r.document() != null ? r.document().getRating() : null;
            if (rating == null) {
                Span dash = new Span("—");
                dash.getStyle().set("color", "#9e9e9e");
                return dash;
            }
            // Format: ★ 8.5 / 10
            String text = String.format("★ %.1f / 10", rating);
            Span ratingSpan = new Span(text);
            ratingSpan.getStyle()
                    .set("font-weight", "600")
                    .set("color", ratingColor(rating));
            return ratingSpan;
        }).setHeader("Valoración").setWidth("130px").setFlexGrow(0);

        // Actions context menu
        grid.addComponentColumn(r -> buildActionsButton(r))
                .setHeader("Acciones").setWidth("130px").setFlexGrow(0);

        grid.setItems(rows);

        VerticalLayout wrapper = new VerticalLayout(grid);
        wrapper.setPadding(false);
        wrapper.setSpacing(false);
        return wrapper;
    }

    // ─── Actions button ───────────────────────────────────────────────────────

    private Button buildActionsButton(DocRow row) {
        DocumentStatus status = row.document() != null ? row.document().getStatus() : DocumentStatus.POR_CREAR;
        boolean isPorCrear = status == DocumentStatus.POR_CREAR;

        Button actionsBtn = new Button("Acciones ▾");
        actionsBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);

        ContextMenu menu = new ContextMenu(actionsBtn);
        menu.setOpenOnClick(true);

        // Crear — only when POR_CREAR and user is not just a viewer
        // (sponsor/programDirector can't create)
        var crearItem = menu.addItem("Crear", e -> handleCrear(row.type()));
        crearItem.setEnabled(isPorCrear && !isProgramDirector && !isSponsor);

        // Modificar — only when document exists
        var modificarItem = menu.addItem("Modificar", e -> handleModificar(row.document()));
        modificarItem.setEnabled(!isPorCrear && row.document() != null);

        // No-op actions
        menu.addItem("Enviar", e -> Notification.show("Funcionalidad de envío no disponible aún"))
                .setEnabled(false);
        menu.addItem("Firmar", e -> Notification.show("Funcionalidad de firma no disponible aún"))
                .setEnabled(false);
        menu.addItem("Imprimir", e -> Notification.show("Descarga PDF no disponible aún"))
                .setEnabled(false);

        return actionsBtn;
    }

    private void handleCrear(DocumentType type) {
        try {
            Document created = documentService.createDocument(currentProject.getId(), type);
            UI.getCurrent().navigate("document/" + created.getId());
        } catch (Exception ex) {
            Notification.show("Error al crear el documento: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
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

    /** Colour-codes the star rating: green ≥ 7, orange ≥ 4, red below 4. */
    private String ratingColor(Double rating) {
        if (rating == null)
            return "#9e9e9e";
        if (rating >= 7)
            return "#388e3c";
        if (rating >= 4)
            return "#f57c00";
        return "#d32f2f";
    }

    // ─── Project save ─────────────────────────────────────────────────────────

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

    // ─── User grid ────────────────────────────────────────────────────────────

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

    private void updateUserList() {
        userGrid.setItems(userService.findByProject(currentProject.getId()));
    }

    private void updateDirectorList() {
        List<User> projectUsers = userService.findByProject(currentProject.getId());
        if (currentProject.getDirector() != null && !projectUsers.contains(currentProject.getDirector())) {
            projectUsers.add(currentProject.getDirector());
        }
        User current = directorSelect.getValue();
        directorSelect.setItems(projectUsers);
        if (current != null && projectUsers.contains(current)) {
            directorSelect.setValue(current);
        }
    }

    // ─── Assign users dialog ──────────────────────────────────────────────────

    private void openAssignUsersDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Asignar Usuarios");
        dialog.setWidth("600px");

        TextField searchField = new TextField("Buscar por UVUS");
        searchField.setPlaceholder("Escribe UVUS para filtrar...");
        searchField.setWidthFull();

        Grid<User> availableGrid = new Grid<>(User.class, false);
        availableGrid.addColumn(User::getUvus).setHeader("UVUS").setWidth("150px");
        availableGrid.addColumn(User::getName).setHeader("Nombre");
        availableGrid.addColumn(u -> u.getRole() != null ? u.getRole().name() : "").setHeader("Rol").setWidth("120px");
        availableGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        availableGrid.setHeight("400px");

        List<User> available = userService.findAvailableForProject(currentProject.getId());
        availableGrid.setItems(available);

        searchField.addValueChangeListener(e -> {
            String f = e.getValue().toLowerCase().trim();
            availableGrid.setItems(f.isEmpty() ? available
                    : available.stream().filter(u -> u.getUvus().toLowerCase().contains(f)).toList());
        });

        dialog.add(new VerticalLayout(searchField, availableGrid));

        Button assignBtn = new Button("Asignar Seleccionados", e -> {
            var selected = availableGrid.getSelectedItems();
            if (selected.isEmpty()) {
                Notification.show("Por favor seleccione al menos un usuario");
                return;
            }
            for (User u : selected) {
                u.setProject(currentProject);
                userService.createOrUpdate(u);
            }
            updateUserList();
            updateDirectorList();
            dialog.close();
            Notification.show(selected.size() + " usuario(s) asignado(s) exitosamente");
        });
        assignBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.getFooter().add(new Button("Cancelar", e -> dialog.close()));
        dialog.getFooter().add(assignBtn);
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
