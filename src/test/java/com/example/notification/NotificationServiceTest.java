package com.example.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.user.User;
import com.example.user.UserService;
import com.example.user.Role;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class NotificationServiceTest {

    @Autowired
    NotificationService notificationService;

    @Autowired
    UserService userService;

    @BeforeEach
    public void setup() {
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("system", "pass",
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_SYSTEM_ADMIN"))));
    }

    @Test
    public void testCreateAndRetrieveNotification() {
        User user = new User();
        user.setName("Notify User");
        user.setUvus("notify_user");
        user.setRole(Role.USER);
        userService.createOrUpdate(user);

        notificationService.notify(user, "Test Notification", "link/to/somewhere");

        List<Notification> notifications = notificationService.getUserNotifications(user.getId());
        assertEquals(1, notifications.size());
        Notification n = notifications.get(0);
        assertEquals("Test Notification", n.getContent());
        assertEquals("link/to/somewhere", n.getLink());
        assertFalse(n.isRead());
    }

    @Test
    public void testUnreadCountAndMarkAsRead() {
        User user = new User();
        user.setName("Count User");
        user.setUvus("count_user");
        user.setRole(Role.USER);
        userService.createOrUpdate(user);

        notificationService.notify(user, "Msg 1");
        notificationService.notify(user, "Msg 2");
        notificationService.notify(user, "Msg 3");

        long count = notificationService.getUnreadCount(user.getId());
        assertEquals(3, count);

        List<Notification> notifications = notificationService.getUserNotifications(user.getId());
        Notification n1 = notifications.get(0);

        notificationService.markAsRead(n1.getId());

        count = notificationService.getUnreadCount(user.getId());
        assertEquals(2, count);

        // Verify n1 is read
        assertTrue(notificationService.getUserNotifications(user.getId()).stream()
                .filter(n -> n.getId().equals(n1.getId()))
                .findFirst().get().isRead());
    }

    @Test
    public void testNotifyNullUser_ShouldNotFail() {
        // Should gracefully handle null
        notificationService.notify(null, "Test");
    }
}
