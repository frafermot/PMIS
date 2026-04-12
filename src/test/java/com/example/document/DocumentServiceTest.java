package com.example.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import com.example.portfolio.Portfolio;
import com.example.portfolio.PortfolioService;
import com.example.program.Program;
import com.example.program.ProgramService;
import com.example.project.Project;
import com.example.project.ProjectService;
import com.example.user.Role;
import com.example.user.User;
import com.example.user.UserService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class DocumentServiceTest {

    @Autowired
    private DocumentService documentService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private ProgramService programService;
    @Autowired
    private PortfolioService portfolioService;
    @Autowired
    private UserService userService;

    private User director;
    private User otherUser;
    private User progDirector;
    private Project project;

    @BeforeEach
    public void setup() {
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("system", "pass",
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_SYSTEM_ADMIN"))));

        progDirector = new User();
        progDirector.setName("Program Director");
        progDirector.setUvus("progdirector");
        progDirector.setRole(Role.MANAGER);
        progDirector = userService.createOrUpdate(progDirector);

        director = new User();
        director.setName("Project Director");
        director.setUvus("director");
        director.setRole(Role.MANAGER);
        director = userService.createOrUpdate(director);

        otherUser = new User();
        otherUser.setName("Other User");
        otherUser.setUvus("other");
        otherUser.setRole(Role.USER);
        otherUser = userService.createOrUpdate(otherUser);

        Portfolio portfolio = new Portfolio();
        portfolio.setName("Test Portfolio");
        portfolio = portfolioService.createOrUpdate(portfolio);

        Program program = new Program();
        program.setName("Test Program");
        program.setPortfolio(portfolio);
        program.setDirector(progDirector);
        program = programService.createOrUpdate(program);

        Project proj = new Project();
        proj.setName("Test Project");
        proj.setProgram(program);
        proj.setDirector(director);
        this.project = projectService.createOrUpdate(proj);
        
        director.setProject(project);
        userService.createOrUpdate(director);

        SecurityContextHolder.clearContext();
    }

    private void setupSecurityContext(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        user.getUvus(), "password",
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_" + user.getRole().name()))));
    }

    @Test
    public void testInitDocumentsForProject() {
        setupSecurityContext(director);
        documentService.initDocumentsForProject(project);

        Map<DocumentType, Document> docMap = documentService.getProjectDocumentMap(project.getId());
        assertNotNull(docMap);
        
        Document acta = docMap.get(DocumentType.ACTA_CONSTITUCION);
        assertNotNull(acta);
        assertEquals(DocumentStatus.POR_CREAR, acta.getStatus());
        assertEquals(project.getId(), acta.getProject().getId());
    }

    @Test
    public void testCreateDocumentAsAssignedUser() {
        setupSecurityContext(director);
        documentService.initDocumentsForProject(project);

        Document createdDoc = documentService.createDocument(project.getId(), DocumentType.ACTA_CONSTITUCION);
        
        assertNotNull(createdDoc);
        assertEquals(DocumentStatus.EN_PROCESO, createdDoc.getStatus());
        assertNotNull(createdDoc.getContent());
    }

    @Test
    public void testCreateDocumentAsOtherUserShouldFail() {
        setupSecurityContext(director);
        documentService.initDocumentsForProject(project);

        setupSecurityContext(otherUser);
        assertThrows(SecurityException.class, () -> {
            documentService.createDocument(project.getId(), DocumentType.ACTA_CONSTITUCION);
        });
    }

    @Test
    public void testUpdateDocumentContentAsAssignedUser() {
        setupSecurityContext(director);
        documentService.initDocumentsForProject(project);
        
        Document createdDoc = documentService.createDocument(project.getId(), DocumentType.ACTA_CONSTITUCION);

        Document updateReq = new Document();
        updateReq.setId(createdDoc.getId());
        updateReq.setVersion(createdDoc.getVersion());
        updateReq.setContent("Updated Content 123");
        
        Document updated = documentService.createOrUpdate(updateReq);
        assertEquals("Updated Content 123", updated.getContent());
        
        var versions = documentService.getVersions(updated.getId());
        assertTrue(versions.size() > 0);
    }
    
    @Test
    public void testProgramDirectorRating() {
        setupSecurityContext(director);
        documentService.initDocumentsForProject(project);
        Document createdDoc = documentService.createDocument(project.getId(), DocumentType.ACTA_CONSTITUCION);
        
        Document dirUpdate = new Document();
        dirUpdate.setId(createdDoc.getId());
        dirUpdate.setVersion(createdDoc.getVersion());
        dirUpdate.setContent(createdDoc.getContent());
        dirUpdate.setRating(5.0);
        Document saved1 = documentService.createOrUpdate(dirUpdate);
        assertNull(saved1.getRating());

        setupSecurityContext(progDirector);
        Document progDirUpdate = new Document();
        progDirUpdate.setId(createdDoc.getId());
        progDirUpdate.setVersion(saved1.getVersion());
        progDirUpdate.setContent(createdDoc.getContent());
        progDirUpdate.setRating(8.5);
        Document rated = documentService.createOrUpdate(progDirUpdate);
        assertEquals(8.5, rated.getRating());
        
        Document progDirUpdateInvalid = new Document();
        progDirUpdateInvalid.setId(createdDoc.getId());
        progDirUpdateInvalid.setVersion(rated.getVersion());
        progDirUpdateInvalid.setContent(createdDoc.getContent());
        progDirUpdateInvalid.setRating(11.0);
        assertThrows(IllegalArgumentException.class, () -> {
            documentService.createOrUpdate(progDirUpdateInvalid);
        });
    }

    @Test
    public void testDeleteAsAssignedUser() {
        setupSecurityContext(director);
        documentService.initDocumentsForProject(project);
        Document createdDoc = documentService.createDocument(project.getId(), DocumentType.ACTA_CONSTITUCION);
        
        documentService.delete(createdDoc.getId());
        assertNull(documentService.get(createdDoc.getId()));
    }
    
    @Test
    public void testDeleteAsOtherUserShouldFail() {
        setupSecurityContext(director);
        documentService.initDocumentsForProject(project);
        Document createdDoc = documentService.createDocument(project.getId(), DocumentType.ACTA_CONSTITUCION);
        
        setupSecurityContext(otherUser);
        assertThrows(SecurityException.class, () -> {
            documentService.delete(createdDoc.getId());
        });
    }
}
