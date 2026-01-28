package com.example.base.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle; // Import para el botón de menú
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.RouterLayout; // Necesario para el Footer
import com.vaadin.flow.server.menu.MenuConfiguration; // Import opcional
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.theme.lumo.LumoUtility.AlignItems;
import com.vaadin.flow.theme.lumo.LumoUtility.Display; // Importa tu tema
import com.vaadin.flow.theme.lumo.LumoUtility.Flex;
import com.vaadin.flow.theme.lumo.LumoUtility.FlexDirection;
import com.vaadin.flow.theme.lumo.LumoUtility.FontSize;
import com.vaadin.flow.theme.lumo.LumoUtility.FontWeight;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;
import com.vaadin.flow.theme.lumo.LumoUtility.IconSize;
import com.vaadin.flow.theme.lumo.LumoUtility.JustifyContent;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;
import com.vaadin.flow.theme.lumo.LumoUtility.TextColor;

public class MainLayout extends AppLayout implements RouterLayout { // Implementamos RouterLayout

    // 1. ÁREA DE CONTENIDO
    // Este Div será el contenedor de tus vistas (PortfolioView, etc.)
    private final Div contentArea = new Div();

    // Constructor del Esqueleto
    private final com.vaadin.flow.spring.security.AuthenticationContext authContext;
    private final com.example.user.UserService userService;

    // Constructor del Esqueleto
    public MainLayout(com.vaadin.flow.spring.security.AuthenticationContext authContext,
            com.example.user.UserService userService) {
        this.authContext = authContext;
        this.userService = userService;

        setPrimarySection(Section.DRAWER);
        setDrawerOpened(true); // Ensure drawer is open by default on login

        // 2. HEADER (Navbar - Barra Superior)
        // Creamos el botón "hamburguesa" y la cabecera (logo/título)
        var appHeader = createHeader();
        addToNavbar(new DrawerToggle(), appHeader);

        // 3. SIDEBAR (Drawer - Menú Lateral)
        // Usamos un Flex Layout vertical para el Drawer
        Div drawerContent = new Div();
        drawerContent.addClassNames(Display.FLEX, FlexDirection.COLUMN, "h-full");

        // Scroller para el menú (ocupa el espacio disponible)
        Scroller scroller = new Scroller(createSideNav());
        scroller.addClassNames(Flex.GROW);

        drawerContent.add(scroller);

        // Sección de Usuario (Footer del Drawer)
        if (authContext.isAuthenticated()) {
            drawerContent.add(createDrawerFooter());
        }

        addToDrawer(drawerContent);

        // 4. FOOTER y CONTENIDO
        // Creamos el footer
        var appFooter = createFooter();

        // Creamos un wrapper para el contenido y el footer
        // Esto nos permite tener un "sticky footer"
        Div mainContentWrapper = new Div(contentArea, appFooter);

        // Estilos para que el footer se quede abajo
        mainContentWrapper.addClassNames(Display.FLEX, FlexDirection.COLUMN, "h-full");
        contentArea.addClassNames(Flex.GROW); // El contenido crece para empujar el footer

        // Asignamos este wrapper como el contenido principal del AppLayout
        setContent(mainContentWrapper);
    }

    private Div createDrawerFooter() {
        Div footer = new Div();
        footer.addClassNames(Display.FLEX, FlexDirection.COLUMN, Padding.MEDIUM, Gap.SMALL, "border-t");

        authContext.getAuthenticatedUser(org.springframework.security.core.userdetails.UserDetails.class)
                .ifPresent(userDetails -> {
                    com.example.user.User user = userService.findByUvus(userDetails.getUsername());
                    if (user != null) {
                        Span name = new Span(user.getName());
                        name.addClassNames(FontWeight.BOLD, FontSize.SMALL);

                        Span uvus = new Span("@" + user.getUvus());
                        uvus.addClassNames(TextColor.SECONDARY, FontSize.XSMALL);

                        footer.add(name, uvus);
                    }
                });

        com.vaadin.flow.component.button.Button logoutButton = new com.vaadin.flow.component.button.Button(
                "Cerrar Sesión", VaadinIcon.SIGN_OUT.create());
        logoutButton.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY);
        logoutButton.addClickListener(e -> authContext.logout());

        footer.add(logoutButton);
        return footer;
    }

    // Cabecera (Logo y Nombre) - Ahora es horizontal
    private Div createHeader() {
        var appLogo = VaadinIcon.CUBES.create();
        appLogo.addClassNames(TextColor.PRIMARY, IconSize.LARGE);

        var appName = new Span("PMIS");
        appName.addClassNames(FontWeight.SEMIBOLD, FontSize.LARGE);

        var header = new Div(appLogo, appName);
        header.addClassNames(Display.FLEX, Padding.SMALL, Gap.MEDIUM, AlignItems.CENTER);
        return header;
    }

    // Menú Lateral (Sidebar) - Filtrado por rol
    private SideNav createSideNav() {
        var nav = new SideNav();
        nav.addClassNames(Margin.Horizontal.MEDIUM);

        // Get current user role
        authContext.getAuthenticatedUser(org.springframework.security.core.userdetails.UserDetails.class)
                .ifPresent(userDetails -> {
                    com.example.user.User currentUser = userService.findByUvus(userDetails.getUsername());
                    if (currentUser != null) {
                        com.example.user.Role userRole = currentUser.getRole();

                        // Add menu entries based on role
                        MenuConfiguration.getMenuEntries().forEach(entry -> {
                            String title = entry.title();

                            // Filter menu items based on role
                            boolean shouldShow = true;

                            if (title.equals("Registro de Gestores")) {
                                // Only ADMIN can see Gestores
                                shouldShow = userRole == com.example.user.Role.ADMIN;
                            } else if (title.equals("Usuarios")) {
                                // ADMIN and MANAGER can see Usuarios
                                shouldShow = userRole == com.example.user.Role.ADMIN
                                        || userRole == com.example.user.Role.MANAGER;
                            }

                            if (shouldShow) {
                                nav.addItem(createSideNavItem(entry));
                            }
                        });
                    }
                });

        return nav;
    }

    // Ítems del Menú - Sin cambios
    private SideNavItem createSideNavItem(MenuEntry menuEntry) {
        if (menuEntry.icon() != null) {
            return new SideNavItem(menuEntry.title(), menuEntry.path(), new Icon(menuEntry.icon()));
        } else {
            return new SideNavItem(menuEntry.title(), menuEntry.path());
        }
    }

    // Implementación del Footer
    private Div createFooter() {
        var footer = new Div(new Span("© 2024 Nombre de tu Empresa | Todos los derechos reservados"));

        // Estilos para el footer
        footer.addClassNames(
                Display.FLEX,
                AlignItems.CENTER,
                JustifyContent.CENTER,
                Padding.MEDIUM,
                TextColor.SECONDARY,
                "text-xs" // Texto pequeño
        );
        return footer;
    }

    // Este método es requerido por RouterLayout.
    // Se asegura de que las vistas se carguen en nuestro 'contentArea'
    @Override
    public void showRouterLayoutContent(com.vaadin.flow.component.HasElement content) {
        if (content != null) {
            contentArea.getElement().appendChild(content.getElement());
        }
    }
}