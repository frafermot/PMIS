package com.example.security;

import com.example.user.Role;
import com.example.user.User;
import com.example.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {

    private final UserRepository userRepository;

    public SecurityService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Gets the currently authenticated user
     * 
     * @return Current user or null if not authenticated
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        String uvus = authentication.getName();
        return userRepository.findByUvus(uvus);
    }

    /**
     * Checks if the current user has ADMIN role
     */
    public boolean isAdmin() {
        User currentUser = getCurrentUser();
        return currentUser != null && currentUser.getRole() == Role.ADMIN;
    }

    /**
     * Checks if the current user has MANAGER role
     */
    public boolean isManager() {
        User currentUser = getCurrentUser();
        return currentUser != null && currentUser.getRole() == Role.MANAGER;
    }

    /**
     * Checks if the current user has ADMIN or MANAGER role
     */
    public boolean isAdminOrManager() {
        User currentUser = getCurrentUser();
        return currentUser != null &&
                (currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.MANAGER);
    }

    /**
     * Checks if the current user is the same as the given user ID
     */
    public boolean isCurrentUser(Long userId) {
        User currentUser = getCurrentUser();
        return currentUser != null && currentUser.getId().equals(userId);
    }

    /**
     * Checks if deletion is allowed for the given user
     * - Cannot delete yourself
     * - Only admins can delete managers or admins
     * - Cannot delete any admin
     */
    public boolean canDeleteUser(Long userId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        // Cannot delete yourself
        if (currentUser.getId().equals(userId)) {
            return false;
        }

        User targetUser = userRepository.findById(userId).orElse(null);
        if (targetUser == null) {
            return false;
        }

        // Cannot delete an admin under any circumstances
        if (targetUser.getRole() == Role.ADMIN) {
            return false;
        }

        // Only admins can delete managers
        if (targetUser.getRole() == Role.MANAGER) {
            return currentUser.getRole() == Role.ADMIN;
        }

        // Admins and Managers can delete regular users
        return currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.MANAGER;
    }

    /**
     * Checks if the current user can modify (create/edit) managers or admins
     * Only admins can do this
     */
    public boolean canModifyManagerOrAdmin() {
        return isAdmin();
    }
}
