package com.example.task;

import com.example.base.ui.MainLayout;
import com.example.project.Project;
import com.example.project.ProjectService;
import com.example.security.SecurityService;
import com.example.user.User;
import com.example.user.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupVariant;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Route(value = "proyecto/:projectId/gantt", layout = MainLayout.class)
@PageTitle("Cronograma")
@RolesAllowed({ "ADMIN", "MANAGER", "USER" })
public class ProjectGanttView extends VerticalLayout implements BeforeEnterObserver {

    private final TaskService taskService;
    private final UserService userService;
    private final ProjectService projectService;
    private final SecurityService securityService;
    private Project currentProject;

    private TreeGrid<Task> grid = new TreeGrid<>(Task.class);
    private List<Task> tasks;
    private boolean showBaseline = false;
    private boolean isReadOnlyView = false;
    private ProjectBaseline selectedBaseline;
    private java.util.Map<Long, TaskBaseline> taskBaselineMap = new java.util.HashMap<>();
    private ComboBox<ProjectBaseline> baselineSelector;
    private HorizontalLayout baselineRow;

    private Div timelineContainer;

    private LocalDate viewMinDate;
    private LocalDate viewMaxDate;
    private long totalViewDays;

    public ProjectGanttView(TaskService taskService, UserService userService, ProjectService projectService,
            SecurityService securityService) {
        this.taskService = taskService;
        this.userService = userService;
        this.projectService = projectService;
        this.securityService = securityService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        grid.addItemDoubleClickListener(e -> openTaskDialog(e.getItem()));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Long projectId = event.getRouteParameters().getLong("projectId").orElse(null);
        if (projectId == null) {
            UI.getCurrent().navigate("proyectos");
            return;
        }
        currentProject = projectService.get(projectId);
        if (currentProject == null) {
            Notification.show("Proyecto no encontrado");
            UI.getCurrent().navigate("proyectos");
            return;
        }

        User currentUser = securityService.getCurrentUser();
        boolean isDirector = securityService.isProjectDirector(projectId);
        boolean isMember = currentUser != null && currentUser.getProject() != null
                && currentUser.getProject().getId().equals(projectId);
        boolean isAdmin = securityService.isAdmin() || securityService.isSystemAdmin();

        boolean isSponsor = securityService.isProjectSponsor(projectId);

        if (!isDirector && !isMember && !isAdmin && !isSponsor) {
            Notification.show("No tienes acceso al cronograma de este proyecto");
            UI.getCurrent().navigate("proyecto/" + projectId);
            return;
        }

        this.isReadOnlyView = isSponsor && !isDirector && !isMember && !isAdmin;

        // Collapse drawer automatically
        UI.getCurrent().getElement().executeJs("document.querySelector('vaadin-app-layout').drawerOpened = false");

        removeAll();
        buildView();
        refreshData();
    }

    private void buildView() {
        // Header
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);

        HorizontalLayout titleGroup = new HorizontalLayout();
        titleGroup.setAlignItems(Alignment.CENTER);
        Button backBtn = new Button("Volver", e -> UI.getCurrent().navigate("proyecto/" + currentProject.getId()));
        titleGroup.add(backBtn, new H2("Cronograma: " + currentProject.getName()));

        // Toolbar
        MenuBar menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.LUMO_CONTRAST);

        // Acciones principales
        Button addTaskBtn = new Button("Tarea", VaadinIcon.PLUS.create(), e -> openTaskDialog(new Task()));
        addTaskBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
        addTaskBtn.setVisible(!isReadOnlyView);

        // Menú de Planificación
        MenuItem planning = menuBar.addItem(VaadinIcon.CHART_TIMELINE.create());
        planning.add(" Planificación");
        planning.setVisible(!isReadOnlyView);
        planning.getSubMenu().addItem("Asignar EDT", e -> {
            taskService.assignWBS(currentProject);
            refreshData();
        });
        planning.getSubMenu().addItem("Calcular Ruta Crítica", e -> {
            taskService.calculateCriticalPath(currentProject);
            refreshData();
        });
        planning.getSubMenu().addItem("Agrupar Selección", e -> openGroupDialog());

        // Menú de Línea Base
        MenuItem baseline = menuBar.addItem(VaadinIcon.FLAG.create());
        baseline.add(" Línea Base");
        
        baselineSelector = new ComboBox<>();
        baselineSelector.setPlaceholder("Seleccionar LB...");
        baselineSelector.setItemLabelGenerator(ProjectBaseline::getName);
        baselineSelector.setWidth("200px");
        baselineSelector.addValueChangeListener(e -> {
            selectedBaseline = e.getValue();
            refreshData();
        });
        updateBaselineSelector(baselineSelector);
        
        baseline.getSubMenu().addItem("Fijar Nueva Línea Base", e -> openNewBaselineDialog(baselineSelector)).setVisible(!isReadOnlyView);
        
        // Add the selector to the second row
        baselineRow = new HorizontalLayout(new Span("Ver LB:"), baselineSelector);
        baselineRow.setAlignItems(Alignment.CENTER);
        baselineRow.setSpacing(true);
        baselineRow.setVisible(false); // Default to hidden
        updateBaselineSelector(baselineSelector);

        // Menú de Configuración
        MenuItem config = menuBar.addItem(VaadinIcon.COG.create());
        config.add(" Configuración");
        config.setVisible(!isReadOnlyView);
        config.getSubMenu().addItem("Calendario Laboral", e -> openCalendarDialog());
        config.getSubMenu().addItem("Visibilidad de Columnas", e -> openColumnConfigDialog());
        
        String unitLabel = currentProject.getDurationUnit().equals("DAYS") ? "Cambiar a Horas" : "Cambiar a Días";
        config.getSubMenu().addItem(unitLabel, e -> {
            String newUnit = currentProject.getDurationUnit().equals("DAYS") ? "HOURS" : "DAYS";
            currentProject.setDurationUnit(newUnit);
            projectService.createOrUpdate(currentProject);
            refreshData();
        });

        // Header and Actions in two rows for more space
        VerticalLayout topLayout = new VerticalLayout();
        topLayout.setPadding(false);
        topLayout.setSpacing(true);
        topLayout.setWidthFull();

        HorizontalLayout firstRow = new HorizontalLayout(titleGroup);
        firstRow.setWidthFull();
        firstRow.setJustifyContentMode(JustifyContentMode.BETWEEN);

        HorizontalLayout secondRow = new HorizontalLayout(addTaskBtn, menuBar, baselineRow);
        secondRow.setWidthFull();
        secondRow.setJustifyContentMode(JustifyContentMode.START);
        secondRow.setSpacing(true);

        topLayout.add(firstRow, secondRow);
        add(topLayout);

        // Split Layout
        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(30);

        // Left Panel (Grid)
        VerticalLayout leftPanel = new VerticalLayout();
        leftPanel.setSizeFull();
        leftPanel.setPadding(false);
        configureGrid();
        leftPanel.add(grid);

        // Right Panel (Timeline)
        timelineContainer = new Div();
        timelineContainer.setSizeFull();
        timelineContainer.getStyle()
                .set("overflow-x", "auto")
                .set("overflow-y", "hidden")
                .set("background-color", "#fafafa")
                .set("position", "relative")
                .set("border", "1px solid #e0e0e0")
                .set("border-radius", "4px");

        splitLayout.addToPrimary(leftPanel);
        splitLayout.addToSecondary(timelineContainer);

        add(splitLayout);
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.setSelectionMode(SelectionMode.NONE);
        grid.removeAllColumns();
        grid.setSelectionMode(SelectionMode.MULTI);
        grid.setColumnReorderingAllowed(true);
        grid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_COLUMN_BORDERS, com.vaadin.flow.component.grid.GridVariant.LUMO_ROW_STRIPES);

        String visible = currentProject.getVisibleColumns();
        if (visible == null || visible.trim().isEmpty()) {
            visible = "EDT,Nombre,Pred.,Responsable,Inicio,Fin,Duración,Coste";
        }
        
        this.showBaseline = visible.contains("LB");

        if (visible.contains("EDT"))
            grid.addColumn(Task::getWbsCode).setHeader("EDT").setWidth("60px").setFlexGrow(0).setResizable(true);

        if (visible.contains("Nombre"))
            grid.addComponentHierarchyColumn(task -> {
                String prefix = task.isMilestone() ? "◆ " : "";
                Span nameSpan = new Span(prefix + task.getName());
                if (task.isGroup()) nameSpan.getStyle().set("font-weight", "bold");
                if (task.isCritical()) nameSpan.getStyle().set("color", "#d32f2f");
                return nameSpan;
            }).setHeader("Nombre").setFlexGrow(3).setResizable(true);

        if (visible.contains("Pred."))
            grid.addComponentColumn(task -> {
                TextField predField = new TextField();
                predField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
                predField.setPlaceholder("ID");
                predField.setWidthFull();
                predField.setReadOnly(isReadOnlyView);
                if (task.getPredecessor() != null) predField.setValue(task.getPredecessor().getId().toString());
                predField.addValueChangeListener(e -> {
                    if (e.isFromClient()) {
                        String val = e.getValue();
                        if (val == null || val.trim().isEmpty()) {
                            task.setPredecessor(null);
                            task.setDependencyType(TaskDependencyType.NONE);
                            taskService.saveTask(task);
                            refreshData();
                        } else {
                            try {
                                Long id = Long.parseLong(val.trim());
                                Task pred = taskService.getTaskById(id);
                                if (pred != null && !pred.equals(task)) {
                                    if (pred.isGroup()) {
                                        Task firstChild = taskService.getFirstChildOfGroup(pred);
                                        task.setPredecessor(firstChild != null ? firstChild : pred);
                                    } else {
                                        task.setPredecessor(pred);
                                    }
                                    if (task.getDependencyType() == TaskDependencyType.NONE || task.getDependencyType() == null) {
                                        task.setDependencyType(TaskDependencyType.FINISH_TO_START);
                                    }
                                    try {
                                        taskService.saveTask(task);
                                        refreshData();
                                    } catch (IllegalStateException ex) {
                                        Notification.show(ex.getMessage(), 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                                        task.setPredecessor(null);
                                        predField.setValue(e.getOldValue() != null ? e.getOldValue() : "");
                                    }
                                } else {
                                    Notification.show("Predecesora inválida");
                                    predField.setValue(e.getOldValue() != null ? e.getOldValue() : "");
                                }
                            } catch (NumberFormatException ex) {
                                Notification.show("ID inválido");
                                predField.setValue(e.getOldValue() != null ? e.getOldValue() : "");
                            }
                        }
                    }
                });
                return predField;
            }).setHeader("Pred.").setFlexGrow(1).setResizable(true);

        if (visible.contains("Responsable"))
            grid.addColumn(t -> t.getAssignee() != null ? t.getAssignee().getName() : "-")
                .setHeader("Responsable").setFlexGrow(1).setResizable(true);

        if (visible.contains("Inicio"))
            grid.addColumn(t -> t.getStartDate().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .setHeader("Inicio").setFlexGrow(1).setResizable(true);
        
        if (visible.contains("Fin"))
            grid.addColumn(t -> t.getEndDate().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .setHeader("Fin").setFlexGrow(1).setResizable(true);
        
        if (visible.contains("Duración"))
            grid.addColumn(task -> {
                if (task.isMilestone()) return "0";
                if (currentProject.getDurationUnit().equals("HOURS")) {
                    return (task.getDuration() != null ? task.getDuration() : 0) + "h";
                }
                return (task.getDuration() != null ? task.getDuration() : (ChronoUnit.DAYS.between(task.getStartDate(), task.getEndDate()) + 1)) + "d";
            }).setHeader("Duración").setFlexGrow(1).setResizable(true);

        if (visible.contains("Coste"))
            grid.addColumn(task -> String.format("%.2f €", calculateTaskCost(task)))
                    .setHeader("Coste").setFlexGrow(1).setResizable(true);

        if (showBaseline) {
            if (visible.contains("Inicio LB"))
                grid.addColumn(task -> {
                    TaskBaseline tb = taskBaselineMap.get(task.getId());
                    return tb != null ? tb.getStartDate().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "-";
                }).setHeader("Inicio LB").setFlexGrow(1).setResizable(true);
            
            if (visible.contains("Fin LB"))
                grid.addColumn(task -> {
                    TaskBaseline tb = taskBaselineMap.get(task.getId());
                    return tb != null ? tb.getEndDate().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "-";
                }).setHeader("Fin LB").setFlexGrow(1).setResizable(true);
            
            if (visible.contains("Duración LB"))
                grid.addColumn(task -> {
                    TaskBaseline tb = taskBaselineMap.get(task.getId());
                    if (tb == null) return "-";
                    if (currentProject.getDurationUnit().equals("HOURS")) {
                        return (tb.getDuration() != null ? tb.getDuration() : 0) + "h";
                    }
                    return (tb.getDuration() != null ? tb.getDuration() : (ChronoUnit.DAYS.between(tb.getStartDate(), tb.getEndDate()) + 1)) + "d";
                }).setHeader("Duración LB").setFlexGrow(1).setResizable(true);
            
            if (visible.contains("Coste LB"))
                grid.addColumn(task -> String.format("%.2f €", calculateBaselineTaskCost(task)))
                        .setHeader("Coste LB").setFlexGrow(1).setResizable(true);

            if (visible.contains("Variación Coste"))
                grid.addComponentColumn(task -> {
                    double actual = calculateTaskCost(task);
                    double baseline = calculateBaselineTaskCost(task);
                    double diff = actual - baseline;
                    Span span = new Span(String.format("%+.2f €", diff));
                    if (diff > 0) span.getStyle().set("color", "red");
                    else if (diff < 0) span.getStyle().set("color", "green");
                    return span;
                }).setHeader("Variación Coste").setFlexGrow(1).setResizable(true);
        }
    }

    private void openColumnConfigDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Configurar Columnas");
        
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        
        CheckboxGroup<String> group = new CheckboxGroup<>();
        group.setItems("EDT", "Nombre", "Pred.", "Responsable", "Inicio", "Fin", "Duración", "Coste", "Inicio LB", "Fin LB", "Duración LB", "Coste LB", "Variación Coste");
        group.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);
        
        String visible = currentProject.getVisibleColumns();
        if (visible != null) {
            Set<String> initial = Stream.of(visible.split(",")).collect(Collectors.toSet());
            group.setValue(initial);
        }
        
        content.add(new Span("Selecciona las columnas que deseas visualizar:"), group);
        dialog.add(content);
        
        Button saveBtn = new Button("Guardar", e -> {
            Set<String> selected = group.getValue();
            if (selected.isEmpty()) {
                Notification.show("Debes seleccionar al menos una columna");
                return;
            }
            currentProject.setVisibleColumns(String.join(",", selected));
            projectService.createOrUpdate(currentProject);
            refreshData();
            dialog.close();
            Notification.show("Configuración de columnas guardada");
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        dialog.getFooter().add(new Button("Cancelar", ev -> dialog.close()), saveBtn);
        dialog.open();
    }

    private void openGroupDialog() {
        var selected = grid.getSelectedItems();
        if (selected.isEmpty()) {
            Notification.show("Selecciona tareas para agrupar");
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Nuevo Grupo");
        TextField nameField = new TextField("Nombre del Grupo");
        dialog.add(nameField);

        Button saveBtn = new Button("Crear", e -> {
            if (!nameField.isEmpty()) {
                taskService.createTaskGroup(currentProject, nameField.getValue(), new java.util.ArrayList<>(selected));
                refreshData();
                dialog.close();
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(new Button("Cancelar", e -> dialog.close()), saveBtn);
        dialog.open();
    }

    private void refreshData() {
        tasks = taskService.getTasksByProject(currentProject);
        
        // Load baseline data if selected
        if (selectedBaseline != null) {
            taskBaselineMap = taskService.getTaskBaselineMap(selectedBaseline);
        } else {
            taskBaselineMap.clear();
        }
        
        configureGrid();

        Task projectRoot = new Task();
        projectRoot.setId(-1L);
        projectRoot.setName(currentProject.getName());
        projectRoot.setStartDate(currentProject.getStartDate() != null ? currentProject.getStartDate().atTime(currentProject.getWorkStartHour()) : LocalDateTime.now());
        projectRoot.setEndDate(currentProject.getEndDate() != null ? currentProject.getEndDate().atTime(currentProject.getWorkEndHour()) : LocalDateTime.now().plusDays(1));
        projectRoot.setGroup(true);
        projectRoot.setWbsCode("1.");

        grid.setItems(
            java.util.List.of(projectRoot),
            task -> {
                if (task.getId() != null && task.getId().equals(-1L)) {
                    return tasks.stream().filter(t -> t.getParentGroup() == null).toList();
                }
                return tasks.stream().filter(t -> task.equals(t.getParentGroup())).toList();
            }
        );
        grid.expand(projectRoot);
        grid.expand(tasks.stream().filter(Task::isGroup).toList());

        calculateDateRange();
        renderTimeline();
    }

    private void calculateDateRange() {
        LocalDate tasksMin = tasks.stream().map(t -> t.getStartDate().toLocalDate()).min(LocalDate::compareTo).orElse(null);
        LocalDate tasksMax = tasks.stream().map(t -> t.getEndDate().toLocalDate()).max(LocalDate::compareTo).orElse(null);

        LocalDate projStart = currentProject.getStartDate();
        LocalDate projEnd = currentProject.getEndDate();

        viewMinDate = LocalDate.now();
        viewMaxDate = LocalDate.now().plusDays(30);

        if (projStart != null)
            viewMinDate = projStart;
        if (tasksMin != null && tasksMin.isBefore(viewMinDate))
            viewMinDate = tasksMin;

        if (projEnd != null)
            viewMaxDate = projEnd;
        if (tasksMax != null && tasksMax.isAfter(viewMaxDate))
            viewMaxDate = tasksMax;

        // Add some padding
        viewMinDate = viewMinDate.minusDays(3);
        viewMaxDate = viewMaxDate.plusDays(3);

        totalViewDays = ChronoUnit.DAYS.between(viewMinDate, viewMaxDate) + 1;
    }

    private List<Task> getHierarchicalTasks() {
        List<Task> result = new java.util.ArrayList<>();
        
        Task projectRoot = new Task();
        projectRoot.setId(-1L);
        projectRoot.setName(currentProject.getName());
        projectRoot.setStartDate(currentProject.getStartDate() != null ? currentProject.getStartDate().atTime(currentProject.getWorkStartHour()) : LocalDateTime.now());
        projectRoot.setEndDate(currentProject.getEndDate() != null ? currentProject.getEndDate().atTime(currentProject.getWorkEndHour()) : LocalDateTime.now().plusDays(1));
        projectRoot.setGroup(true);
        projectRoot.setWbsCode("1.");
        
        result.add(projectRoot);

        List<Task> roots = tasks.stream().filter(t -> t.getParentGroup() == null).toList();
        for (Task root : roots) {
            result.add(root);
            result.addAll(tasks.stream().filter(t -> root.equals(t.getParentGroup())).toList());
        }
        return result;
    }

    private void renderTimeline() {
        timelineContainer.removeAll();

        // Canvas for the timeline content
        Div canvas = new Div();
        // Width is proportional to total days to allow horizontal scroll (e.g., 20px
        // per day)
        int minWidthPerDay = 30;
        long canvasWidth = totalViewDays * minWidthPerDay;

        canvas.getStyle()
                .set("width", canvasWidth + "px")
                .set("height", "100%")
                .set("position", "relative")
                .set("min-width", "100%");

        // Render Header (Months/Days)
        Div header = new Div();
        header.getStyle()
                .set("height", "56px")
                .set("border-bottom", "1px solid #eaeeee")
                .set("position", "absolute")
                .set("top", "0")
                .set("left", "0")
                .set("width", "100%")
                .set("background-color", "#f8f9fa");

        Div monthLayer = new Div();
        monthLayer.getStyle()
                .set("height", "28px")
                .set("width", "100%")
                .set("position", "relative")
                .set("border-bottom", "1px solid #eaeeee");

        Div dayLayer = new Div();
        dayLayer.getStyle()
                .set("height", "28px")
                .set("width", "100%")
                .set("position", "relative");

        int currentMonth = -1;
        int currentYear = -1;
        int monthStartDayIndex = 0;

        for (int i = 0; i < totalViewDays; i++) {
            LocalDate date = viewMinDate.plusDays(i);

            // Vertical day line
            Div verticalLine = new Div();
            verticalLine.getStyle()
                    .set("position", "absolute")
                    .set("left", ((double) i / totalViewDays * 100) + "%")
                    .set("top", "0")
                    .set("height", "100%")
                    .set("width", "1px")
                    .set("background-color", "#f0f0f0")
                    .set("z-index", "0");
            
            if (!isWorkingDay(date)) {
                Div holidayShade = new Div();
                holidayShade.getStyle()
                        .set("position", "absolute")
                        .set("left", ((double) i / totalViewDays * 100) + "%")
                        .set("top", "56px")
                        .set("height", "calc(100% - 56px)")
                        .set("width", (100.0 / totalViewDays) + "%")
                        .set("background-color", "#f0f0f0")
                        .set("opacity", "0.5")
                        .set("z-index", "0");
                canvas.add(holidayShade);
            }
            
            canvas.add(verticalLine);

            // Day marker
            Div dayMarker = new Div();
            dayMarker.setText(String.valueOf(date.getDayOfMonth()));
            dayMarker.getStyle()
                    .set("position", "absolute")
                    .set("left", ((double) i / totalViewDays * 100) + "%")
                    .set("width", (100.0 / totalViewDays) + "%")
                    .set("text-align", "center")
                    .set("font-size", "11px")
                    .set("font-weight", "500")
                    .set("color", "#444")
                    .set("border-left", "1px solid #eaeeee")
                    .set("line-height", "28px");
            dayLayer.add(dayMarker);

            // Month/Year grouping
            if (date.getMonthValue() != currentMonth || date.getYear() != currentYear) {
                if (currentMonth != -1) {
                    addMonthBlock(monthLayer, monthStartDayIndex, i, currentMonth, currentYear);
                }
                currentMonth = date.getMonthValue();
                currentYear = date.getYear();
                monthStartDayIndex = i;
            }
        }
        // Last month block
        if (currentMonth != -1) {
            addMonthBlock(monthLayer, monthStartDayIndex, (int) totalViewDays, currentMonth, currentYear);
        }

        header.add(monthLayer, dayLayer);
        canvas.add(header);

        int headerHeight = 56;
        int rowHeight = 44; // Standard Vaadin Lumo Grid row height

        List<Task> orderedTasks = getHierarchicalTasks();
        java.util.Map<Long, Integer> taskIndexMap = new java.util.HashMap<>();
        for (int i = 0; i < orderedTasks.size(); i++) {
            if (orderedTasks.get(i).getId() != null) {
                taskIndexMap.put(orderedTasks.get(i).getId(), i);
            }
        }

        // Render Background Rows to match Grid
        int bgTopOffset = headerHeight;
        for (Task task : orderedTasks) {
            Div rowBg = new Div();
            rowBg.getStyle()
                    .set("position", "absolute")
                    .set("left", "0")
                    .set("width", "100%")
                    .set("top", bgTopOffset + "px")
                    .set("height", rowHeight + "px")
                    .set("border-bottom", "1px solid #eaeeee")
                    .set("box-sizing", "border-box");
            canvas.add(rowBg);
            bgTopOffset += rowHeight;
        }

        // Render Task Bars
        int topOffset = headerHeight;
        for (Task task : orderedTasks) {
            long startMinutes = ChronoUnit.MINUTES.between(viewMinDate.atStartOfDay(), task.getStartDate());
            long durationMinutes = ChronoUnit.MINUTES.between(task.getStartDate(), task.getEndDate());
            
            double left = (double) startMinutes / (totalViewDays * 24 * 60.0) * 100.0;
            double width = (double) durationMinutes / (totalViewDays * 24 * 60.0) * 100.0;
            if (width < 0.5) width = 0.5; // Min width to be visible

            Div bar = new Div();
            bar.setText(task.getName());
            // Vertically center the bar inside the 44px row
            int barHeight = task.isGroup() ? 16 : 24;
            int barMarginTop = (rowHeight - barHeight) / 2;
            String bgColor = task.isGroup() ? "#424242" : (task.isCritical() ? "#d32f2f" : "#1976d2");
            String borderRadius = task.isGroup() ? "0px" : "4px";

            bar.getStyle()
                    .set("position", "absolute")
                    .set("left", left + "%")
                    .set("width", width + "%")
                    .set("top", (topOffset + barMarginTop) + "px")
                    .set("background-color", bgColor)
                    .set("color", "white")
                    .set("border-radius", borderRadius)
                    .set("font-size", "11px")
                    .set("line-height", barHeight + "px")
                    .set("padding-left", "8px")
                    .set("white-space", "nowrap")
                    .set("overflow", "hidden")
                    .set("text-overflow", "ellipsis")
                    .set("box-shadow", "0 1px 3px rgba(0,0,0,0.2)")
                    .set("z-index", "2")
                    .set("cursor", "pointer");

            if (task.isMilestone()) {
                bar.setText("");
                bar.getStyle()
                        .set("width", "12px")
                        .set("height", "12px")
                        .set("border-radius", "50%")
                        .set("top", (topOffset + (rowHeight - 12) / 2) + "px")
                        .set("margin-left", "-6px"); // center the point on the date
            } else {
                bar.getStyle()
                        .set("height", barHeight + "px");
            }

            bar.addClickListener(e -> openTaskDialog(task));
            canvas.add(bar);

            // Render Baseline if enabled
            if (showBaseline && taskBaselineMap.containsKey(task.getId())) {
                TaskBaseline tb = taskBaselineMap.get(task.getId());
                long blStartMinutes = ChronoUnit.MINUTES.between(viewMinDate.atStartOfDay(), tb.getStartDate());
                long blDurationMinutes = ChronoUnit.MINUTES.between(tb.getStartDate(), tb.getEndDate());
                
                double blLeft = (double) blStartMinutes / (totalViewDays * 24 * 60.0) * 100.0;
                double blWidth = (double) blDurationMinutes / (totalViewDays * 24 * 60.0) * 100.0;
                if (blWidth < 0.5) blWidth = 0.5;

                Div bBar = new Div();
                bBar.getStyle()
                        .set("position", "absolute")
                        .set("left", blLeft + "%")
                        .set("width", blWidth + "%")
                        .set("top", (topOffset + rowHeight - 6) + "px")
                        .set("height", "3px")
                        .set("background-color", "#616161")
                        .set("border-radius", "1px")
                        .set("opacity", "0.5")
                        .set("z-index", "1");
                canvas.add(bBar);
            }

            topOffset += rowHeight;
        }

        // Render Project Markers (Start and End)
        renderProjectMarker(canvas, currentProject.getStartDate().atTime(currentProject.getWorkStartHour()), "#4caf50", "Inicio Proyecto");
        renderProjectMarker(canvas, currentProject.getEndDate() != null ? currentProject.getEndDate().atTime(currentProject.getWorkEndHour()) : null, "#f44336", "Fin Proyecto");

        // Render Dependency Arrows using Native Vaadin Divs
        for (Task task : orderedTasks) {
            if (task.getPredecessor() != null && task.getPredecessor().getId() != null && taskIndexMap.containsKey(task.getPredecessor().getId())) {
                Task pred = orderedTasks.get(taskIndexMap.get(task.getPredecessor().getId()));

                long predEndMin = ChronoUnit.MINUTES.between(viewMinDate.atStartOfDay(), pred.getEndDate());
                long taskStartMin = ChronoUnit.MINUTES.between(viewMinDate.atStartOfDay(), task.getStartDate());
                
                double x1 = (double) predEndMin / (totalViewDays * 24 * 60.0) * 100.0;
                double x2 = (double) taskStartMin / (totalViewDays * 24 * 60.0) * 100.0;
                double y1 = headerHeight + taskIndexMap.get(pred.getId()) * rowHeight + (rowHeight / 2.0);
                double y2 = headerHeight + taskIndexMap.get(task.getId()) * rowHeight + (rowHeight / 2.0);

                Div arrowLine = new Div();
                arrowLine.getStyle()
                        .set("position", "absolute")
                        .set("left", Math.min(x1, x2) + "%")
                        .set("width", Math.max(0.1, Math.abs(x2 - x1)) + "%")
                        .set("top", Math.min(y1, y2) + "px")
                        .set("height", Math.max(1.0, Math.abs(y2 - y1)) + "px")
                        .set("border-left", x2 > x1 ? "1.5px solid #9e9e9e" : "none")
                        .set("border-right", x1 > x2 ? "1.5px solid #9e9e9e" : "none")
                        .set("border-top", y2 > y1 ? "1.5px solid #9e9e9e" : "none")
                        .set("border-bottom", y1 > y2 ? "1.5px solid #9e9e9e" : "none")
                        .set("z-index", "1")
                        .set("pointer-events", "none");
                canvas.add(arrowLine);
            }
        }

        timelineContainer.add(canvas);
    }

    private void renderProjectMarker(Div canvas, LocalDateTime dateTime, String color, String title) {
        if (dateTime == null) return;
        long minutesFromStart = ChronoUnit.MINUTES.between(viewMinDate.atStartOfDay(), dateTime);
        double left = (double) minutesFromStart / (totalViewDays * 24 * 60.0) * 100.0;

        Div line = new Div();
        line.getStyle()
                .set("position", "absolute")
                .set("left", left + "%")
                .set("top", "0")
                .set("bottom", "0")
                .set("width", "2px")
                .set("background-color", color)
                .set("z-index", "10");

        Span label = new Span(title);
        label.getStyle()
                .set("position", "absolute")
                .set("left", "4px")
                .set("top", "4px")
                .set("color", color)
                .set("font-size", "10px")
                .set("font-weight", "bold")
                .set("white-space", "nowrap");
        line.add(label);

        canvas.add(line);
    }

    private void addMonthBlock(Div layer, int startIndex, int endIndex, int month, int year) {
        double leftPercent = ((double) startIndex / totalViewDays) * 100;
        double widthPercent = ((double) (endIndex - startIndex) / totalViewDays) * 100;

        String monthName = java.time.Month.of(month).getDisplayName(
                java.time.format.TextStyle.SHORT,
                new java.util.Locale("es", "ES"));
        String text = monthName.toUpperCase() + " " + year;

        Div mDiv = new Div();
        mDiv.setText(text);
        mDiv.getStyle()
                .set("position", "absolute")
                .set("left", leftPercent + "%")
                .set("width", widthPercent + "%")
                .set("text-align", "center")
                .set("font-size", "11px")
                .set("font-weight", "bold")
                .set("color", "#333")
                .set("border-left", "1px solid #eaeeee")
                .set("line-height", "28px")
                .set("overflow", "hidden");
        layer.add(mDiv);
    }

    private void openTaskDialog(Task task) {
        if (isReadOnlyView) return;
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(task.getId() == null ? "Nueva Tarea" : "Editar Tarea");

        FormLayout form = new FormLayout();

        TextField nameField = new TextField("Nombre");
        TextArea descField = new TextArea("Descripción");
        DateTimePicker startDateField = new DateTimePicker("Fecha/Hora Inicio");
        IntegerField durationField = new IntegerField("Duración (" + (currentProject.getDurationUnit().equals("DAYS") ? "Días" : "Horas") + ")");
        durationField.setMin(1);
        DateTimePicker endDateField = new DateTimePicker("Fecha/Hora Fin");

        // Reactivity for Duration
        durationField.addValueChangeListener(e -> {
            if (e.isFromClient() && e.getValue() != null && startDateField.getValue() != null) {
                int val = e.getValue();
                if (currentProject.getDurationUnit().equals("HOURS")) {
                    endDateField.setValue(taskService.addWorkingHours(startDateField.getValue(), val, currentProject));
                } else {
                    long hoursPerDay = ChronoUnit.HOURS.between(currentProject.getWorkStartHour(), currentProject.getWorkEndHour());
                    endDateField.setValue(taskService.addWorkingHours(startDateField.getValue(), (int)(val * hoursPerDay), currentProject));
                }
            }
        });

        startDateField.addValueChangeListener(e -> {
            if (e.isFromClient() && e.getValue() != null && durationField.getValue() != null) {
                LocalDateTime correctedStart = taskService.ensureWorkingTime(e.getValue(), currentProject);
                if (!correctedStart.equals(e.getValue())) {
                    startDateField.setValue(correctedStart);
                    return; // Re-triggering with corrected value
                }

                int val = durationField.getValue();
                if (currentProject.getDurationUnit().equals("HOURS")) {
                    endDateField.setValue(taskService.addWorkingHours(correctedStart, val, currentProject));
                } else {
                    long hoursPerDay = ChronoUnit.HOURS.between(currentProject.getWorkStartHour(), currentProject.getWorkEndHour());
                    endDateField.setValue(taskService.addWorkingHours(correctedStart, (int)(val * hoursPerDay), currentProject));
                }
            }
        });

        endDateField.addValueChangeListener(e -> {
            if (e.isFromClient() && e.getValue() != null && startDateField.getValue() != null) {
                // If the user changes the end date manually, we recalculate duration in hours first
                long totalHours = ChronoUnit.HOURS.between(startDateField.getValue(), e.getValue());
                // But we must account for non-working hours and days... this is complex for a simple listener.
                // For now, let's just use the hour difference as a rough estimate or trust the user.
                if (currentProject.getDurationUnit().equals("HOURS")) {
                    durationField.setValue((int)totalHours);
                } else {
                    long hoursPerDay = ChronoUnit.HOURS.between(currentProject.getWorkStartHour(), currentProject.getWorkEndHour());
                    durationField.setValue((int)Math.ceil((double)totalHours / hoursPerDay));
                }
            }
        });

        ComboBox<User> assigneeBox = new ComboBox<>("Asignado");
        assigneeBox.setItems(userService.findByProject(currentProject.getId()));
        assigneeBox.setItemLabelGenerator(User::getName);

        ComboBox<Task> predecessorBox = new ComboBox<>("Predecesora");
        predecessorBox.setItems(tasks.stream().filter(t -> !t.equals(task)).toList());
        predecessorBox.setItemLabelGenerator(Task::getName);

        ComboBox<TaskDependencyType> depTypeBox = new ComboBox<>("Tipo Dependencia");
        depTypeBox.setItems(TaskDependencyType.values());

        // Reactive logic for predecessors
        com.vaadin.flow.component.HasValue.ValueChangeListener<com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent<ComboBox<Task>, Task>> predListener = e -> {
            Task pred = predecessorBox.getValue();
            TaskDependencyType depType = depTypeBox.getValue();
            
            if (e.isFromClient() && pred != null) {
                if (depType == null || depType == TaskDependencyType.NONE) {
                    depTypeBox.setValue(TaskDependencyType.FINISH_TO_START);
                    depType = TaskDependencyType.FINISH_TO_START;
                }
                
                int duration = durationField.getValue() != null ? durationField.getValue() : 1;
                int durationHours = currentProject.getDurationUnit().equals("HOURS") ? duration : (int)(duration * ChronoUnit.HOURS.between(currentProject.getWorkStartHour(), currentProject.getWorkEndHour()));
                
                if (depType == TaskDependencyType.FINISH_TO_START) {
                    startDateField.setValue(taskService.ensureWorkingTime(pred.getEndDate(), currentProject));
                    endDateField.setValue(taskService.addWorkingHours(startDateField.getValue(), durationHours, currentProject));
                } else if (depType == TaskDependencyType.START_TO_START) {
                    startDateField.setValue(pred.getStartDate());
                    endDateField.setValue(taskService.addWorkingHours(startDateField.getValue(), durationHours, currentProject));
                } else if (depType == TaskDependencyType.FINISH_TO_FINISH) {
                    endDateField.setValue(taskService.ensureWorkingTime(pred.getEndDate(), currentProject));
                    startDateField.setValue(taskService.subtractWorkingHours(endDateField.getValue(), durationHours, currentProject));
                } else if (depType == TaskDependencyType.START_TO_FINISH) {
                    endDateField.setValue(taskService.ensureWorkingTime(pred.getStartDate(), currentProject));
                    startDateField.setValue(taskService.subtractWorkingHours(endDateField.getValue(), durationHours, currentProject));
                }
            }
        };

        com.vaadin.flow.component.HasValue.ValueChangeListener<com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent<ComboBox<TaskDependencyType>, TaskDependencyType>> depTypeListener = e -> {
            Task pred = predecessorBox.getValue();
            TaskDependencyType depType = e.getValue();
            
            if (e.isFromClient() && pred != null && depType != null && depType != TaskDependencyType.NONE) {
                int duration = durationField.getValue() != null ? durationField.getValue() : 1;
                int durationHours = currentProject.getDurationUnit().equals("HOURS") ? duration : (int)(duration * ChronoUnit.HOURS.between(currentProject.getWorkStartHour(), currentProject.getWorkEndHour()));
                
                if (depType == TaskDependencyType.FINISH_TO_START) {
                    startDateField.setValue(taskService.ensureWorkingTime(pred.getEndDate(), currentProject));
                    endDateField.setValue(taskService.addWorkingHours(startDateField.getValue(), durationHours, currentProject));
                } else if (depType == TaskDependencyType.START_TO_START) {
                    startDateField.setValue(pred.getStartDate());
                    endDateField.setValue(taskService.addWorkingHours(startDateField.getValue(), durationHours, currentProject));
                } else if (depType == TaskDependencyType.FINISH_TO_FINISH) {
                    endDateField.setValue(taskService.ensureWorkingTime(pred.getEndDate(), currentProject));
                    startDateField.setValue(taskService.subtractWorkingHours(endDateField.getValue(), durationHours, currentProject));
                } else if (depType == TaskDependencyType.START_TO_FINISH) {
                    endDateField.setValue(taskService.ensureWorkingTime(pred.getStartDate(), currentProject));
                    startDateField.setValue(taskService.subtractWorkingHours(endDateField.getValue(), durationHours, currentProject));
                }
            }
        };

        predecessorBox.addValueChangeListener(predListener);
        depTypeBox.addValueChangeListener(depTypeListener);

        Binder<Task> binder = new Binder<>(Task.class);
        binder.forField(nameField).asRequired("Requerido").bind(Task::getName, Task::setName);
        binder.forField(descField).bind(Task::getDescription, Task::setDescription);
        binder.forField(startDateField).asRequired("Requerido").bind(Task::getStartDate, Task::setStartDate);
        binder.forField(endDateField).asRequired("Requerido").bind(Task::getEndDate, Task::setEndDate);
        binder.forField(durationField).asRequired("Requerido").bind(Task::getDuration, Task::setDuration);
        binder.forField(assigneeBox).bind(Task::getAssignee, Task::setAssignee);
        binder.forField(predecessorBox).bind(Task::getPredecessor, Task::setPredecessor);
        binder.forField(depTypeBox).bind(Task::getDependencyType, Task::setDependencyType);

        com.vaadin.flow.component.checkbox.Checkbox milestoneBox = new com.vaadin.flow.component.checkbox.Checkbox("Hito (Milestone)");
        binder.forField(milestoneBox).bind(Task::isMilestone, Task::setMilestone);

        milestoneBox.addValueChangeListener(e -> {
            boolean isMilestone = e.getValue();
            durationField.setEnabled(!isMilestone);
            endDateField.setEnabled(!isMilestone);
            if (isMilestone) {
                durationField.setValue(0);
                if (startDateField.getValue() != null) {
                    endDateField.setValue(startDateField.getValue());
                }
            } else if (durationField.getValue() == 0) {
                durationField.setValue(1);
                if (startDateField.getValue() != null) {
                    endDateField.setValue(startDateField.getValue());
                }
            }
        });

        if (task.getId() == null) {
            task.setProject(currentProject);
            task.setDependencyType(TaskDependencyType.NONE);
            task.setStartDate(LocalDate.now().atTime(currentProject.getWorkStartHour()));
            task.setEndDate(LocalDate.now().atTime(currentProject.getWorkEndHour()));
            long hoursPerDay = ChronoUnit.HOURS.between(currentProject.getWorkStartHour(), currentProject.getWorkEndHour());
            task.setDuration(currentProject.getDurationUnit().equals("HOURS") ? (int)hoursPerDay : 1);
        }
        
        // Initialize fields from task entity
        binder.readBean(task);
        
        // Ensure duration is set if it was null (for legacy tasks)
        if (task.getDuration() == null) {
            long days = ChronoUnit.DAYS.between(task.getStartDate(), task.getEndDate()) + 1;
            if (currentProject.getDurationUnit().equals("HOURS")) {
                long hoursPerDay = ChronoUnit.HOURS.between(currentProject.getWorkStartHour(), currentProject.getWorkEndHour());
                task.setDuration((int)(days * hoursPerDay));
            } else {
                task.setDuration((int)days);
            }
            durationField.setValue(task.getDuration());
        }
        binder.readBean(task);

        form.add(nameField, descField, startDateField, durationField, endDateField, assigneeBox, predecessorBox,
                depTypeBox, milestoneBox);
        dialog.add(form);

        Button saveBtn = new Button("Guardar", e -> {
            try {
                if (binder.writeBeanIfValid(task)) {
                    if (task.getId() != null && task.getId().equals(-1L)) {
                        currentProject.setName(task.getName());
                        currentProject.setStartDate(task.getStartDate().toLocalDate());
                        currentProject.setEndDate(task.getEndDate().toLocalDate());
                        projectService.createOrUpdate(currentProject);
                    } else {
                        taskService.saveTask(task);
                    }
                    dialog.close();
                    refreshData();
                    Notification.show("Guardado correctamente", 3000, Notification.Position.BOTTOM_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    Notification.show("Por favor, revise los campos marcados como requeridos", 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_WARNING);
                }
            } catch (IllegalStateException ex) {
                Notification error = new Notification(ex.getMessage(), 5000, Notification.Position.MIDDLE);
                error.addThemeVariants(NotificationVariant.LUMO_ERROR);
                error.open();
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage());
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancelar", e -> dialog.close());

        if (task.getId() != null && !task.getId().equals(-1L)) {
            Button deleteBtn = new Button("Eliminar", e -> {
                ConfirmDialog confirm = new ConfirmDialog();
                confirm.setHeader("Eliminar Tarea");
                confirm.setText("¿Estás seguro de eliminar '" + task.getName() + "'?");
                confirm.setCancelable(true);
                confirm.setConfirmText("Eliminar");
                confirm.setConfirmButtonTheme("error primary");
                confirm.addConfirmListener(ev -> {
                    taskService.deleteTask(task);
                    refreshData();
                    dialog.close();
                });
                confirm.open();
            });
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            deleteBtn.getStyle().set("margin-right", "auto"); // push other buttons to the right
            dialog.getFooter().add(deleteBtn);
        }

        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void renderHorizontalLine(Div canvas, double x1, double x2, double y, String color) {
        double left = Math.min(x1, x2);
        double width = Math.abs(x2 - x1);
        Div line = new Div();
        line.getStyle()
                .set("position", "absolute")
                .set("left", left + "px")
                .set("top", (y - 1) + "px")
                .set("width", width + "px")
                .set("height", "2px")
                .set("background-color", color)
                .set("z-index", "4")
                .set("pointer-events", "none");
        canvas.add(line);
    }

    private void renderVerticalLine(Div canvas, double x, double y1, double y2, String color) {
        double top = Math.min(y1, y2);
        double height = Math.abs(y2 - y1) + 2; 
        Div line = new Div();
        line.getStyle()
                .set("position", "absolute")
                .set("left", (x - 1) + "px")
                .set("top", (top - 1) + "px")
                .set("width", "2px")
                .set("height", height + "px")
                .set("background-color", color)
                .set("z-index", "4")
                .set("pointer-events", "none");
        canvas.add(line);
    }

    private void renderArrowHead(Div canvas, double x, double y, String color) {
        Div arrow = new Div();
        arrow.getStyle()
                .set("position", "absolute")
                .set("left", (x - 4) + "px")
                .set("top", (y - 4) + "px")
                .set("width", "0")
                .set("height", "0")
                .set("border-top", "4px solid transparent")
                .set("border-bottom", "4px solid transparent")
                .set("border-left", "5px solid " + color)
                .set("z-index", "4")
                .set("pointer-events", "none");
        canvas.add(arrow);
    }

    private double calculateTaskCost(Task task) {
        if (task.isGroup()) {
            // Recursive sum for groups
            List<Task> children = tasks.stream()
                    .filter(t -> task.equals(t.getParentGroup()))
                    .toList();
            double sum = 0;
            for (Task child : children) {
                sum += calculateTaskCost(child);
            }
            // If it's the project root (id -1), sum all top-level tasks
            if (task.getId() != null && task.getId().equals(-1L)) {
                sum = tasks.stream()
                        .filter(t -> t.getParentGroup() == null)
                        .mapToDouble(this::calculateTaskCost)
                        .sum();
            }
            return sum;
        }

        if (task.isMilestone() || task.getAssignee() == null || task.getAssignee().getResource() == null) {
            return 0.0;
        }

        Double costPerHour = task.getAssignee().getResource().getCostPerHour();
        if (costPerHour == null) return 0.0;

        if (currentProject.getDurationUnit().equals("HOURS")) {
            return (task.getDuration() != null ? task.getDuration() : 0) * costPerHour;
        }
        
        long days = ChronoUnit.DAYS.between(task.getStartDate(), task.getEndDate()) + 1;
        long hoursPerDay = ChronoUnit.HOURS.between(currentProject.getWorkStartHour(), currentProject.getWorkEndHour());
        
        return days * hoursPerDay * costPerHour;
    }

    private double calculateBaselineTaskCost(Task task) {
        TaskBaseline tb = taskBaselineMap.get(task.getId());
        if (tb == null) return 0.0;
        
        if (task.isGroup()) {
            List<Task> children = tasks.stream()
                    .filter(t -> task.equals(t.getParentGroup()))
                    .toList();
            double sum = 0;
            for (Task child : children) {
                sum += calculateBaselineTaskCost(child);
            }
            if (task.getId() != null && task.getId().equals(-1L)) {
                sum = tasks.stream()
                        .filter(t -> t.getParentGroup() == null)
                        .mapToDouble(this::calculateBaselineTaskCost)
                        .sum();
            }
            return sum;
        }

        if (task.isMilestone() || task.getAssignee() == null || task.getAssignee().getResource() == null) {
            return 0.0;
        }

        Double costPerHour = task.getAssignee().getResource().getCostPerHour();
        if (costPerHour == null) return 0.0;

        if (currentProject.getDurationUnit().equals("HOURS")) {
            return (tb.getDuration() != null ? tb.getDuration() : 0) * costPerHour;
        }

        long days = ChronoUnit.DAYS.between(tb.getStartDate(), tb.getEndDate()) + 1;
        long hoursPerDay = ChronoUnit.HOURS.between(currentProject.getWorkStartHour(), currentProject.getWorkEndHour());
        
        return days * hoursPerDay * costPerHour;
    }

    private boolean isWorkingDay(LocalDate date) {
        String workingDaysStr = currentProject.getWorkingDays();
        if (workingDaysStr == null || workingDaysStr.isEmpty()) return true;
        java.util.Set<java.time.DayOfWeek> workingDays = java.util.Arrays.stream(workingDaysStr.split(","))
                .map(java.time.DayOfWeek::valueOf)
                .collect(java.util.stream.Collectors.toSet());
        return workingDays.contains(date.getDayOfWeek());
    }

    private void openCalendarDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Configuración de Calendario Laboral");
        dialog.setWidth("600px");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);

        CheckboxGroup<DayOfWeek> workingDaysGroup = new CheckboxGroup<>();
        workingDaysGroup.setLabel("Días Laborables");
        workingDaysGroup.setItems(DayOfWeek.values());
        workingDaysGroup.setItemLabelGenerator(day -> day.getDisplayName(TextStyle.FULL, new Locale("es", "ES")));
        workingDaysGroup.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);
        
        String[] savedDays = currentProject.getWorkingDays().split(",");
        Set<DayOfWeek> initialDays = Stream.of(savedDays)
                .filter(s -> !s.isEmpty())
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toSet());
        workingDaysGroup.setValue(initialDays);

        TimePicker startTime = new TimePicker("Hora Inicio Jornada");
        startTime.setValue(currentProject.getWorkStartHour());
        
        TimePicker endTime = new TimePicker("Hora Fin Jornada");
        endTime.setValue(currentProject.getWorkEndHour());

        HorizontalLayout timesLayout = new HorizontalLayout(startTime, endTime);
        timesLayout.setWidthFull();

        content.add(workingDaysGroup, timesLayout);
        dialog.add(content);

        Button saveBtn = new Button("Guardar", e -> {
            Set<DayOfWeek> selectedDays = workingDaysGroup.getValue();
            if (selectedDays.isEmpty()) {
                Notification.show("Selecciona al menos un día laborable");
                return;
            }
            String daysStr = selectedDays.stream()
                    .map(DayOfWeek::name)
                    .collect(Collectors.joining(","));
            currentProject.setWorkingDays(daysStr);
            currentProject.setWorkStartHour(startTime.getValue());
            currentProject.setWorkEndHour(endTime.getValue());
            
            projectService.createOrUpdate(currentProject);
            Notification.show("Calendario actualizado. Recalculando cronograma...");
            
            dialog.close();
            refreshData();
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancelar", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, saveBtn);

        dialog.open();
    }

    private void openNewBaselineDialog(ComboBox<ProjectBaseline> selector) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Fijar Nueva Línea Base");
        TextField nameField = new TextField("Nombre de la Línea Base");
        nameField.setPlaceholder("Ej: Plan inicial, Revisión Q1...");
        nameField.setWidthFull();
        dialog.add(nameField);

        Button saveBtn = new Button("Fijar", e -> {
            if (!nameField.isEmpty()) {
                ProjectBaseline pb = taskService.setBaselineForProject(currentProject, nameField.getValue());
                updateBaselineSelector(selector);
                selector.setValue(pb);
                dialog.close();
                Notification.show("Línea base '" + pb.getName() + "' establecida");
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(new Button("Cancelar", ev -> dialog.close()), saveBtn);
        dialog.open();
    }

    private void updateBaselineSelector(ComboBox<ProjectBaseline> selector) {
        List<ProjectBaseline> baselines = taskService.getBaselinesByProject(currentProject);
        selector.setItems(baselines);
        if (baselineRow != null) {
            baselineRow.setVisible(!baselines.isEmpty());
        }
    }
}
