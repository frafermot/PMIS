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
    private final com.example.notification.NotificationService notificationService;

    // Constructor del Esqueleto
    public MainLayout(com.vaadin.flow.spring.security.AuthenticationContext authContext,
            com.example.user.UserService userService,
            com.example.notification.NotificationService notificationService) {
        this.authContext = authContext;
        this.userService = userService;
        this.notificationService = notificationService;

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

        // Asignamos contentArea como el contenido principal del AppLayout
        setContent(contentArea);
        contentArea.addClassNames("h-full");
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

                        com.vaadin.flow.component.button.Button editNameBtn = new com.vaadin.flow.component.button.Button(VaadinIcon.PENCIL.create());
                        editNameBtn.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY, com.vaadin.flow.component.button.ButtonVariant.LUMO_SMALL);
                        editNameBtn.getStyle().set("padding", "0");
                        editNameBtn.getStyle().set("margin-left", "auto");
                        
                        editNameBtn.addClickListener(e -> {
                            com.vaadin.flow.component.dialog.Dialog dialog = new com.vaadin.flow.component.dialog.Dialog();
                            dialog.setHeaderTitle("Modificar Nombre");
                            com.vaadin.flow.component.textfield.TextField nameField = new com.vaadin.flow.component.textfield.TextField("Nombre");
                            nameField.setValue(user.getName());
                            nameField.setWidthFull();
                            
                            com.vaadin.flow.component.button.Button saveBtn = new com.vaadin.flow.component.button.Button("Guardar", ev -> {
                                if(!nameField.isEmpty()) {
                                    try {
                                        userService.updateName(user.getId(), nameField.getValue());
                                        user.setName(nameField.getValue());
                                        name.setText(nameField.getValue());
                                        dialog.close();
                                        com.vaadin.flow.component.notification.Notification.show("Nombre actualizado", 3000, com.vaadin.flow.component.notification.Notification.Position.BOTTOM_CENTER);
                                    } catch (Exception ex) {
                                        com.vaadin.flow.component.notification.Notification.show("Error: " + ex.getMessage(), 5000, com.vaadin.flow.component.notification.Notification.Position.MIDDLE);
                                    }
                                }
                            });
                            saveBtn.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);
                            com.vaadin.flow.component.button.Button cancelBtn = new com.vaadin.flow.component.button.Button("Cancelar", ev -> dialog.close());
                            
                            dialog.add(nameField);
                            dialog.getFooter().add(cancelBtn, saveBtn);
                            dialog.open();
                        });

                        com.vaadin.flow.component.orderedlayout.HorizontalLayout nameLayout = new com.vaadin.flow.component.orderedlayout.HorizontalLayout(name, editNameBtn);
                        nameLayout.setWidthFull();
                        nameLayout.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);

                        Span uvus = new Span("@" + user.getUvus());
                        uvus.addClassNames(TextColor.SECONDARY, FontSize.XSMALL);

                        footer.add(nameLayout, uvus);
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

    private SideNavItem notificationsItem;

    public void updateNotificationBadge() {
        if (notificationsItem == null)
            return;

        authContext.getAuthenticatedUser(org.springframework.security.core.userdetails.UserDetails.class)
                .ifPresent(userDetails -> {
                    com.example.user.User currentUser = userService.findByUvus(userDetails.getUsername());
                    if (currentUser != null) {
                        long unreadCount = notificationService.getUnreadCount(currentUser.getId());
                        if (unreadCount > 0) {
                            Span badge = new Span(String.valueOf(unreadCount));
                            badge.getElement().getThemeList().add("badge error pill small");
                            badge.getStyle().set("margin-left", "auto");
                            notificationsItem.setSuffixComponent(badge);
                        } else {
                            notificationsItem.setSuffixComponent(null);
                        }
                    }
                });
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

                        // Add Notifications Link for everyone
                        notificationsItem = new SideNavItem("Notificaciones", "notifications",
                                VaadinIcon.BELL.create());
                        updateNotificationBadge(); // Initial population

                        nav.addItem(notificationsItem);
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


    // Este método es requerido por RouterLayout.
    // Se asegura de que las vistas se carguen en nuestro 'contentArea'
    @Override
    public void showRouterLayoutContent(com.vaadin.flow.component.HasElement content) {
        if (content != null) {
            contentArea.getElement().appendChild(content.getElement());
        }
    }
}