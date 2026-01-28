package com.example.examplefeature.ui;

import com.example.base.ui.MainLayout;
import com.example.project.Project;
import com.example.project.ProjectService;
import com.example.user.Role;
import com.example.user.User;
import com.example.user.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;

import java.util.List;

@Route(value = "", layout = MainLayout.class)
@RouteAlias(value = "inicio", layout = MainLayout.class)
@PageTitle("Inicio")
@Menu(order = 0, icon = "vaadin:home", title = "Inicio")
@PermitAll
public class HomeView extends VerticalLayout {

    private final AuthenticationContext authContext;
    private final UserService userService;
    private final ProjectService projectService;

    public HomeView(AuthenticationContext authContext, UserService userService, ProjectService projectService) {
        this.authContext = authContext;
        this.userService = userService;
        this.projectService = projectService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new H1("Bienvenido a PMIS"));

        authContext.getAuthenticatedUser(org.springframework.security.core.userdetails.UserDetails.class)
                .ifPresent(userDetails -> {
                    User currentUser = userService.findByUvusWithProject(userDetails.getUsername());
                    if (currentUser != null) {
                        if (currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.MANAGER) {
                            showManagerAdminView();
                        } else {
                            showUserView(currentUser);
                        }
                    }
                });
    }

    private void showManagerAdminView() {
        add(new H2("Panel de Gestión"));

        Div card = new Div();
        card.getStyle()
                .set("border", "1px solid #ddd")
                .set("border-radius", "8px")
                .set("padding", "20px")
                .set("max-width", "400px")
                .set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)");

        Icon icon = VaadinIcon.BRIEFCASE.create();
        icon.setSize("48px");
        icon.getStyle().set("color", "var(--lumo-primary-color)");

        H2 cardTitle = new H2("Portafolios");
        cardTitle.getStyle().set("margin-top", "10px");

        Paragraph cardDescription = new Paragraph(
                "Gestiona los portafolios, programas y proyectos de la organización");
        cardDescription.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Button goButton = new Button("Ver Portafolios", e -> {
            UI.getCurrent().navigate("portfolios");
        });
        goButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        card.add(icon, cardTitle, cardDescription, goButton);
        add(card);
    }

    private void showUserView(User currentUser) {
        add(new H2("Mis Proyectos"));

        if (currentUser.getProject() != null) {
            Grid<Project> projectGrid = new Grid<>(Project.class, false);
            projectGrid.addColumn(Project::getName).setHeader("Proyecto");
            projectGrid.addColumn(
                    project -> project.getProgram() != null ? project.getProgram().getName() : "Sin Programa")
                    .setHeader("Programa");
            projectGrid.addColumn(
                    project -> project.getDirector() != null ? project.getDirector().getName() : "Sin Director")
                    .setHeader("Director");

            projectGrid.setItems(List.of(currentUser.getProject()));
            projectGrid.setMaxHeight("300px");

            add(projectGrid);
        } else {
            Paragraph noProjects = new Paragraph("No tienes proyectos asignados actualmente.");
            noProjects.getStyle().set("color", "var(--lumo-secondary-text-color)");
            add(noProjects);
        }
    }
}
