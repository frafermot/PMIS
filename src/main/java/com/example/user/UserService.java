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
        return userRepository.save(user);
    }

    public User get(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    public void delete(Long userId) {
        // Validate deletion permissions
        if (!securityService.canDeleteUser(userId)) {
            throw new SecurityException("No tiene permisos para eliminar este usuario");
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

    public User findByUvus(String uvus) {
        return userRepository.findByUvus(uvus);
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
        if (!securityService.canDeleteUser(id)) {
            throw new SecurityException("No tiene permisos para eliminar este usuario");
        }

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
