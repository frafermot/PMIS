package com.example.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import com.example.user.Role;
import com.example.user.User;
import com.example.user.UserService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
public class SecurityServiceTest {

    @Autowired
    private SecurityService securityService;
    @Autowired
    private UserService userService;

    private User admin;
    private User manager;
    private User user1;

    @BeforeEach
    public void setup() {
        authenticateAsSystem();

        admin = new User();
        admin.setName("Admin User");
        admin.setUvus("admin");
        admin.setRole(Role.ADMIN);
        admin = userService.createOrUpdate(admin);

        manager = new User();
        manager.setName("Manager User");
        manager.setUvus("manager");
        manager.setRole(Role.MANAGER);
        manager = userService.createOrUpdate(manager);

        user1 = new User();
        user1.setName("User One");
        user1.setUvus("user1");
        user1.setRole(Role.USER);
        user1 = userService.createOrUpdate(user1);

        SecurityContextHolder.clearContext();
    }

    private void authenticateAsUser(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getUvus(), "password",
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))));
    }

    private void authenticateAsSystem() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("system", "pass",
                        List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"))));
    }

    @Test
    public void testIsSystemAdmin() {
        authenticateAsSystem();
        assertTrue(securityService.isSystemAdmin());
        assertFalse(securityService.isAdmin());
    }

    @Test
    public void testIsAdmin() {
        authenticateAsUser(admin);
        assertTrue(securityService.isAdmin());
        assertTrue(securityService.isAdminOrManager());
        assertFalse(securityService.isSystemAdmin());
    }

    @Test
    public void testIsManager() {
        authenticateAsUser(manager);
        assertFalse(securityService.isAdmin());
        assertTrue(securityService.isManager());
        assertTrue(securityService.isAdminOrManager());
    }

    @Test
    public void testIsCurrentUser() {
        authenticateAsUser(user1);
        assertTrue(securityService.isCurrentUser(user1.getId()));
        assertFalse(securityService.isCurrentUser(manager.getId()));
    }

    @Test
    public void testCanDeleteUser() {
        authenticateAsSystem();
        assertTrue(securityService.canDeleteUser(admin.getId()));

        authenticateAsUser(admin);
        assertFalse(securityService.canDeleteUser(admin.getId()));
        assertTrue(securityService.canDeleteUser(manager.getId()));
        assertTrue(securityService.canDeleteUser(user1.getId()));

        authenticateAsUser(manager);
        assertFalse(securityService.canDeleteUser(manager.getId()));
        assertFalse(securityService.canDeleteUser(admin.getId()));
        assertFalse(securityService.canDeleteUser(manager.getId()));
        assertTrue(securityService.canDeleteUser(user1.getId()));

        authenticateAsUser(user1);
        assertFalse(securityService.canDeleteUser(user1.getId()));
        assertFalse(securityService.canDeleteUser(manager.getId()));
    }
}
