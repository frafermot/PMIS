package com.example.examplefeature.ui;

import com.example.base.ui.MainLayout;
import com.example.project.Project;
import com.example.user.User;
import com.example.user.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

@Route(value = "mis-proyectos", layout = MainLayout.class)
@PageTitle("Mis Proyectos")
@Menu(order = 1, icon = "vaadin:briefcase", title = "Mis Proyectos")
@RolesAllowed({ "USER" })
public class MyProjectsView extends VerticalLayout {

    private final AuthenticationContext authContext;
    private final UserService userService;
    private final Grid<Project> projectGrid = new Grid<>(Project.class, false);

    public MyProjectsView(AuthenticationContext authContext, UserService userService) {
        this.authContext = authContext;
        this.userService = userService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new H2("Mis Proyectos"));

        configureGrid();
        add(projectGrid);
        updateProjectList();
    }

    private void configureGrid() {
        projectGrid.setSizeFull();
        projectGrid.addColumn(Project::getName).setHeader("Proyecto").setSortable(true);
        projectGrid.addColumn(project -> project.getProgram() != null ? project.getProgram().getName() : "Sin Programa")
                .setHeader("Programa")
                .setSortable(true);
        projectGrid
                .addColumn(project -> project.getDirector() != null ? project.getDirector().getName() : "Sin Director")
                .setHeader("Director")
                .setSortable(true);

        // Navigate to project detail view on row click
        projectGrid.addItemClickListener(event -> {
            if (event.getItem() != null) {
                UI.getCurrent().navigate("proyecto/" + event.getItem().getId());
            }
        });

        projectGrid.setMaxHeight("600px");
    }

    private void updateProjectList() {
        authContext.getAuthenticatedUser(org.springframework.security.core.userdetails.UserDetails.class)
                .ifPresent(userDetails -> {
                    User currentUser = userService.findByUvusWithProject(userDetails.getUsername());
                    if (currentUser != null && currentUser.getProject() != null) {
                        projectGrid.setItems(List.of(currentUser.getProject()));
                    } else {
                        projectGrid.setItems(List.of());
                        Paragraph noProjects = new Paragraph("No tienes proyectos asignados actualmente.");
                        noProjects.getStyle().set("color", "var(--lumo-secondary-text-color)");
                        add(noProjects);
                    }
                });
    }
}
