package com.example.communication;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import com.example.user.User;
import com.example.user.UserService;
import com.example.user.Role;
import com.example.program.Program;
import com.example.program.ProgramService;
import com.example.portfolio.Portfolio;
import com.example.portfolio.PortfolioService;
import com.example.project.Project;
import com.example.project.ProjectService;
import com.example.communication.Communication;
import com.example.communication.CommunicationService;
import com.example.communication.CommunicationType;
import com.example.communication.CommunicationStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class CommunicationServiceTest {

    @Autowired
    CommunicationService communicationService;
    @Autowired
    ProjectService projectService;
    @Autowired
    ProgramService programService;
    @Autowired
    PortfolioService portfolioService;
    @Autowired
    UserService userService;

    private User director;
    private User sponsor;
    private User otherUser;
    private Project project;
    private Long cccId;

    @BeforeEach
    public void setup() {
        // Setup System Admin Context for Creating Initial Data
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("system", "pass",
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_SYSTEM_ADMIN"))));

        // Create Users
        User manager = new User();
        manager.setName("Manager");
        manager.setUvus("manager");
        manager.setRole(Role.MANAGER);
        userService.createOrUpdate(manager);

        director = new User();
        director.setName("Project Director");
        director.setUvus("director");
        director.setRole(Role.MANAGER); // Directors usually Managers? Or Users? Project Director can be any user in
                                        // some systems, let's assume Manager for broader rights or User. Based on
                                        // ProjectServiceTest, it can be User.
        // Assuming Project Director needs to be MANAGER or USER. Let's make him MANAGER
        // to avoid other issues.
        userService.createOrUpdate(director);

        sponsor = new User();
        sponsor.setName("Project Sponsor");
        sponsor.setUvus("sponsor");
        sponsor.setRole(Role.MANAGER);
        userService.createOrUpdate(sponsor);

        otherUser = new User();
        otherUser.setName("Other User");
        otherUser.setUvus("other");
        otherUser.setRole(Role.USER);
        userService.createOrUpdate(otherUser);

        // Create Project Structure
        Portfolio portfolio = new Portfolio();
        portfolio.setName("Test Portfolio");
        portfolio.setDirector(manager);
        portfolioService.createOrUpdate(portfolio);

        Program program = new Program();
        program.setName("Test Program");
        program.setPortfolio(portfolio);
        program.setDirector(manager);
        programService.createOrUpdate(program);

        Project proj = new Project();
        proj.setName("Test Project");
        proj.setProgram(program);
        proj.setSponsor(sponsor);
        proj.setDirector(director);
        this.project = projectService.createOrUpdate(proj);

        this.project = projectService.createOrUpdate(proj);

        // Manually create CCC
        com.example.communication.Ccc ccc = new com.example.communication.Ccc();
        ccc.setProject(project);
        cccRepository.save(ccc);
        this.cccId = ccc.getId();
    }

    // Need to inject CccRepository to get the ID if Project doesn't expose it.
    @Autowired
    com.example.communication.CccRepository cccRepository;

    private Long getCccId() {
        return this.cccId;
    }

    @Test
    public void testCreateCommunicationAsProjectDirector() {
        // Setup Security Context
        setupSecurityContext(director);

        Long cccId = getCccId();
        Communication comm = communicationService.createCommunication(cccId, "Test Subject", CommunicationType.INCIDENT,
                director);

        assertNotNull(comm.getId());
        assertEquals("Test Subject", comm.getSubject());
        assertEquals(CommunicationType.INCIDENT, comm.getType());
        assertEquals(director, comm.getCreatedBy());
    }

    @Test
    public void testCreateCommunicationAsProjectSponsor_ShouldFail_OnlyDirectorCanCreate() {
        // CommunicationService.createCommunication checks isProjectDirector.
        // Sponsor should NOT be able to create? Line 29 says:
        // !securityService.isProjectDirector -> AccessDenied.

        setupSecurityContext(sponsor);

        Long cccId = getCccId();
        assertThrows(AccessDeniedException.class, () -> {
            communicationService.createCommunication(cccId, "Sponsor Subject", CommunicationType.CHANGE_REQUEST,
                    sponsor);
        });
    }

    @Test
    public void testCreateCommunicationAsUnauthorizedUser_ShouldFail() {
        setupSecurityContext(otherUser);

        Long cccId = getCccId();
        assertThrows(AccessDeniedException.class, () -> {
            communicationService.createCommunication(cccId, "Unauthorized", CommunicationType.CHANGE_REQUEST,
                    otherUser);
        });
    }

    @Test
    public void testUpdateStatusAsSponsor() {
        // 1. Create communication as Director
        setupSecurityContext(director);
        Long cccId = getCccId();
        Communication comm = communicationService.createCommunication(cccId, "Status Test", CommunicationType.INCIDENT,
                director);

        // 2. Switch to Sponsor
        setupSecurityContext(sponsor);

        // 3. Update Status
        Communication updated = communicationService.updateStatus(comm.getId(), CommunicationStatus.RESOLVED);

        assertEquals(CommunicationStatus.RESOLVED, updated.getStatus());
    }

    @Test
    public void testUpdateStatusAsDirector_ShouldFail() {
        // 1. Create communication as Director
        setupSecurityContext(director);
        Long cccId = getCccId();
        Communication comm = communicationService.createCommunication(cccId, "Status Test 2",
                CommunicationType.INCIDENT,
                director);

        // 2. Try to update status as Director (should fail as per recent change)
        assertThrows(AccessDeniedException.class, () -> {
            communicationService.updateStatus(comm.getId(), CommunicationStatus.RESOLVED);
        });
    }

    private void setupSecurityContext(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        user.getUvus(), "password",
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_" + user.getRole().name()))));
    }
}
