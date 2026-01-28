package com.example.user;

import java.util.List;

import com.example.portfolio.PortfolioRepository;
import com.example.program.ProgramRepository;
import com.example.project.ProjectRepository;
import com.example.pmo.PMORepository;
import com.example.security.SecurityService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final ProgramRepository programRepository;
    private final ProjectRepository projectRepository;
    private final PMORepository pmoRepository;
    private final SecurityService securityService;

    public UserService(UserRepository userRepository, PortfolioRepository portfolioRepository,
            ProgramRepository programRepository, ProjectRepository projectRepository, PMORepository pmoRepository,
            SecurityService securityService) {
        this.userRepository = userRepository;
        this.portfolioRepository = portfolioRepository;
        this.programRepository = programRepository;
        this.projectRepository = projectRepository;
        this.pmoRepository = pmoRepository;
        this.securityService = securityService;
    }

    public User createOrUpdate(User user) {
        if (securityService.isSystemAdmin()) {
            return userRepository.save(user);
        }

        // Admin can manage Managers and Admins
        if ((user.getRole() == Role.MANAGER || user.getRole() == Role.ADMIN) && securityService.isAdmin()) {
            return userRepository.save(user);
        }

        // PMO Directors can manage Users (create/update)
        if (user.getRole() == Role.USER && securityService.isPmoDirector()) {
            // Basic validation or just allow
            return userRepository.save(user);
        }

        // Project Directors can assign users to their project
        // This effectively means updating a User to set their project
        if (user.getId() != null && user.getProject() != null) {
            if (securityService.isProjectDirector(user.getProject().getId())) {
                // Must be USER
                User existing = userRepository.findById(user.getId()).orElse(null);
                if (existing != null && existing.getRole() == Role.USER) {
                    // Allow only if role remains USER (implicit in object passed, but we should be
                    // careful)
                    if (user.getRole() != Role.USER) {
                        throw new SecurityException("No puedes cambiar el rol del usuario");
                    }
                    return userRepository.save(user);
                }
            }
        }

        throw new SecurityException("No tienes permisos para realizar esta acción");
    }

    public User get(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    public void delete(Long userId) {
        // Validate deletion permissions
        if (!securityService.canDeleteUser(userId)) {
            // Check if PMO Director deleted a USER
            if (securityService.isPmoDirector()) {
                User target = userRepository.findById(userId).orElse(null);
                if (target != null && target.getRole() == Role.USER) {
                    // Allow PMO Director to delete USER
                } else {
                    throw new SecurityException("No tiene permisos para eliminar este usuario");
                }
            } else {
                throw new SecurityException("No tiene permisos para eliminar este usuario");
            }
        }

        User userToDelete = userRepository.findById(userId).orElse(null);
        if (userToDelete != null && userToDelete.getRole() == Role.ADMIN) {
            throw new SecurityException("No se puede eliminar un usuario con rol ADMIN");
        }

        if (securityService.isCurrentUser(userId)) {
            throw new SecurityException("No puede eliminarse a sí mismo");
        }

        userRepository.deleteById(userId);
    }

    public List<User> getAll() {
        List<User> users = userRepository.findAllWithProject();
        return users;
    }

    public List<User> findAllByRole(Role role) {
        return userRepository.findAllByRole(role);
    }

    public List<User> findAllByRoles(List<Role> roles) {
        return userRepository.findAllByRoleIn(roles);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<User> findAvailableForProject(Long projectId) {
        return userRepository.findAllWithProject().stream()
                .filter(user -> user.getProject() == null ||
                        (projectId != null && !user.getProject().getId().equals(projectId)))
                .toList();
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<User> findByProject(Long projectId) {
        if (projectId == null) {
            return List.of();
        }
        return userRepository.findAllWithProject().stream()
                .filter(user -> user.getProject() != null && user.getProject().getId().equals(projectId))
                .toList();
    }

    public User findByUvus(String uvus) {
        return userRepository.findByUvus(uvus);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public User findByUvusWithProject(String uvus) {
        return userRepository.findByUvusWithProject(uvus);
    }

    public boolean hasAssignedEntities(Long id) {
        return portfolioRepository.countByDirectorId(id) > 0
                || programRepository.countByDirectorId(id) > 0
                || projectRepository.countByDirectorId(id) > 0
                || projectRepository.countBySponsorId(id) > 0
                || pmoRepository.countByDirectorId(id) > 0;
    }

    public void deleteSafe(Long id) {
        // Validate deletion permissions
        // Re-using logic from delete() for permission check
        boolean customPermission = false;
        if (securityService.isPmoDirector()) {
            User target = userRepository.findById(id).orElse(null);
            if (target != null && target.getRole() == Role.USER) {
                customPermission = true;
            }
        }

        if (!customPermission && !securityService.canDeleteUser(id)) {
            throw new SecurityException("No tiene permisos para eliminar este usuario");
        }

        // ... (rest of logic same as original but we need careful merge)
        User userToDelete = userRepository.findById(id).orElse(null);
        if (userToDelete != null && userToDelete.getRole() == Role.ADMIN) {
            throw new SecurityException("No se puede eliminar un usuario con rol ADMIN");
        }

        if (securityService.isCurrentUser(id)) {
            throw new SecurityException("No puede eliminarse a sí mismo");
        }

        portfolioRepository.unassignDirector(id);
        programRepository.unassignDirector(id);
        projectRepository.unassignDirector(id);
        projectRepository.unassignSponsor(id);
        pmoRepository.unassignDirector(id);
        userRepository.deleteById(id);
    }
}
