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
    private final com.example.portfolio.PortfolioRepository portfolioRepository;
    private final com.example.program.ProgramRepository programRepository;
    private final com.example.project.ProjectRepository projectRepository;
    private final com.example.pmo.PMORepository pmoRepository;

    public SecurityService(UserRepository userRepository,
            com.example.portfolio.PortfolioRepository portfolioRepository,
            com.example.program.ProgramRepository programRepository,
            com.example.project.ProjectRepository projectRepository,
            com.example.pmo.PMORepository pmoRepository) {
        this.userRepository = userRepository;
        this.portfolioRepository = portfolioRepository;
        this.programRepository = programRepository;
        this.projectRepository = projectRepository;
        this.pmoRepository = pmoRepository;
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null
                && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SYSTEM_ADMIN"))) {
            return true;
        }
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null
                && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SYSTEM_ADMIN"))) {
            return true;
        }

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

    public boolean isPmoDirector() {
        User currentUser = getCurrentUser();
        return currentUser != null && pmoRepository.countByDirectorId(currentUser.getId()) > 0;
    }

    public boolean isPortfolioDirector(Long portfolioId) {
        User currentUser = getCurrentUser();
        if (currentUser == null || portfolioId == null)
            return false;
        return portfolioRepository.findById(portfolioId)
                .map(p -> p.getDirector() != null && p.getDirector().getId().equals(currentUser.getId()))
                .orElse(false);
    }

    public boolean isProgramDirector(Long programId) {
        User currentUser = getCurrentUser();
        if (currentUser == null || programId == null)
            return false;
        return programRepository.findById(programId)
                .map(p -> p.getDirector() != null && p.getDirector().getId().equals(currentUser.getId()))
                .orElse(false);
    }

    public boolean isProjectDirector(Long projectId) {
        User currentUser = getCurrentUser();
        if (currentUser == null || projectId == null)
            return false;
        return projectRepository.findById(projectId)
                .map(p -> (p.getDirector() != null && p.getDirector().getId().equals(currentUser.getId())))
                .orElse(false);
    }
}
