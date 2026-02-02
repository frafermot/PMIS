package com.example.notification.ui;

import com.example.base.ui.MainLayout;
import com.example.notification.Notification;
import com.example.notification.NotificationService;
import com.example.security.SecurityService;
import com.example.user.User;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility.TextColor;
import jakarta.annotation.security.RolesAllowed;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "notifications", layout = MainLayout.class)
@PageTitle("Notificaciones")
@RolesAllowed({ "USER", "MANAGER", "ADMIN" })
public class NotificationsView extends VerticalLayout {

    private final NotificationService notificationService;
    private final SecurityService securityService;
    private Grid<Notification> grid = new Grid<>(Notification.class, false);

    public NotificationsView(NotificationService notificationService, SecurityService securityService) {
        this.notificationService = notificationService;
        this.securityService = securityService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new H2("Notificaciones"));

        configureGrid();
        add(grid);
        refreshGrid();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(Notification::getContent).setHeader("Mensaje").setAutoWidth(true);
        grid.addColumn(n -> n.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .setHeader("Fecha")
                .setWidth("180px").setFlexGrow(0);

        grid.addComponentColumn(notification -> {
            if (!notification.isRead()) {
                Button markReadBtn = new Button("Marcar como leída", e -> {
                    notificationService.markAsRead(notification.getId());
                    refreshGrid();
                });
                markReadBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
                return markReadBtn;
            } else {
                return new com.vaadin.flow.component.html.Span("Leída");
            }
        }).setHeader("Estado").setWidth("150px").setFlexGrow(0);

        grid.setPartNameGenerator(notification -> notification.isRead() ? "read-notification" : "unread-notification");

        grid.addItemClickListener(event -> {
            Notification notification = event.getItem();
            if (notification.getLink() != null && !notification.getLink().isEmpty()) {
                if (!notification.isRead()) {
                    notificationService.markAsRead(notification.getId());
                }
                getUI().ifPresent(ui -> ui.navigate(notification.getLink()));
            }
        });
    }

    private void refreshGrid() {
        User currentUser = securityService.getCurrentUser();
        List<Notification> notifications = notificationService.getUserNotifications(currentUser.getId());
        grid.setItems(notifications);
    }
}
