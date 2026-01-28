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

        // Get current user to check if ADMIN
        User currentUser = authContext
                .getAuthenticatedUser(org.springframework.security.core.userdetails.UserDetails.class)
                .map(userDetails -> userService.findByUvusWithProject(userDetails.getUsername()))
                .orElse(null);

        boolean isAdmin = currentUser != null && currentUser.getRole() == Role.ADMIN;

        // Container for cards with CSS Grid layout
        Div cardsContainer = new Div();
        cardsContainer.addClassName("management-cards-container");
        cardsContainer.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(3, 1fr)")
                .set("gap", "20px")
                .set("margin-top", "20px")
                .set("width", "100%");

        // Add CSS to adjust grid based on viewport width
        // When drawer is open (narrower viewport), use 2 columns
        // When drawer is closed (wider viewport), use 3 columns
        getElement().executeJs(
                "const style = document.createElement('style');" +
                        "style.textContent = `" +
                        "  @media (max-width: 1400px) {" +
                        "    .management-cards-container {" +
                        "      grid-template-columns: repeat(2, 1fr) !important;" +
                        "    }" +
                        "  }" +
                        "  @media (min-width: 1401px) {" +
                        "    .management-cards-container {" +
                        "      grid-template-columns: repeat(3, 1fr) !important;" +
                        "    }" +
                        "  }" +
                        "`;" +
                        "document.head.appendChild(style);");

        // Portfolio card
        cardsContainer.add(createManagementCard(
                VaadinIcon.BRIEFCASE,
                "Portafolios",
                "Gestiona los portafolios, programas y proyectos de la organización",
                "Ver Portafolios",
                "portfolios"));

        // Users card
        cardsContainer.add(createManagementCard(
                VaadinIcon.USERS,
                "Usuarios",
                "Gestiona los usuarios del sistema",
                "Ver Usuarios",
                "usuarios"));

        // Managers card (only for ADMIN)
        if (isAdmin) {
            cardsContainer.add(createManagementCard(
                    VaadinIcon.USER_STAR,
                    "Gestores",
                    "Gestiona los gestores y directores de la organización",
                    "Ver Gestores",
                    "gestores"));
        }

        // PMO card
        cardsContainer.add(createManagementCard(
                VaadinIcon.OFFICE,
                "Oficina de Gestión",
                "Gestiona las oficinas de gestión de proyectos (PMO)",
                "Ver PMO",
                "pmo"));

        add(cardsContainer);
    }

    private Div createManagementCard(VaadinIcon iconType, String title, String description,
            String buttonText, String navigationTarget) {
        Div card = new Div();
        card.getStyle()
                .set("border", "1px solid #ddd")
                .set("border-radius", "8px")
                .set("padding", "20px")
                .set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "flex-start")
                .set("min-height", "200px");

        Icon icon = iconType.create();
        icon.setSize("48px");
        icon.getStyle().set("color", "var(--lumo-primary-color)");

        H2 cardTitle = new H2(title);
        cardTitle.getStyle()
                .set("margin-top", "10px")
                .set("margin-bottom", "5px")
                .set("font-size", "1.5rem");

        Paragraph cardDescription = new Paragraph(description);
        cardDescription.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("flex-grow", "1")
                .set("margin-bottom", "15px");

        Button goButton = new Button(buttonText, e -> {
            UI.getCurrent().navigate(navigationTarget);
        });
        goButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        goButton.getStyle().set("width", "100%");

        card.add(icon, cardTitle, cardDescription, goButton);
        return card;
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
