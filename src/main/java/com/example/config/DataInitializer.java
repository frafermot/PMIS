package com.example.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.portfolio.Portfolio;
import com.example.pmo.PMO;
import com.example.program.Program;
import com.example.project.Project;
import com.example.user.Role;
import com.example.user.User;
import com.example.user.UserService;
import com.example.portfolio.PortfolioService;
import com.example.pmo.PMOService;
import com.example.program.ProgramService;
import com.example.project.ProjectService;
import com.example.resource.Resource;
import com.example.resource.ResourceService;

import com.example.document.DocumentService;

@Component
public class DataInitializer implements CommandLineRunner {

    private final PortfolioService portfolioService;
    private final PMOService pmoService;
    private final ProgramService programService;
    private final ProjectService projectService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final DocumentService documentService;
    private final ResourceService resourceService;

    public DataInitializer(
            PortfolioService portfolioService, PMOService pmoService,
            ProgramService programService, ProjectService projectService, UserService userService,
            PasswordEncoder passwordEncoder, DocumentService documentService,
            ResourceService resourceService) {
        this.portfolioService = portfolioService;
        this.pmoService = pmoService;
        this.programService = programService;
        this.projectService = projectService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.documentService = documentService;
        this.resourceService = resourceService;
    }

    @Override
    public void run(String... args) throws Exception {
        // Skip initialization if data already exists
        if (userService.findByUvus("jmcordero") != null) {
            return;
        }

        // Set System Admin context for initialization
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "system", "system",
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_SYSTEM_ADMIN"))));
        User coordinador = new User();
        coordinador.setName("Juan Manuel Cordero Valle");
        coordinador.setUvus("jmcordero");
        coordinador.setRole(Role.ADMIN); // Was isAdmin=true
        coordinador.setPassword(passwordEncoder.encode("password"));
        userService.createOrUpdate(coordinador);
    }
}
