package com.example.document;

import com.example.security.SecurityService;
import com.example.user.User;
import com.example.user.UserService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.math.BigDecimal;

@Service
@Transactional
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final SecurityService securityService;
    private final UserService userService;

    public DocumentService(DocumentRepository documentRepository,
            DocumentVersionRepository documentVersionRepository,
            SecurityService securityService,
            UserService userService) {
        this.documentRepository = documentRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.securityService = securityService;
        this.userService = userService;
    }

    public Document createOrUpdate(Document document) {
        User currentUser = userService.findByUvusWithProject(securityService.getCurrentUser().getUvus());
        if (currentUser == null) {
            throw new SecurityException("Debe autenticarse para guardar documentos");
        }

        if (document.getId() != null) {
            Document existing = documentRepository.findById(document.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));

            boolean isProgramDirector = existing.getProject() != null && existing.getProject().getProgram() != null
                    && securityService.isProgramDirector(existing.getProject().getProgram().getId());

            boolean isAssignedToProject = existing.getProject() != null
                    && existing.getProject().equals(currentUser.getProject());

            if (!isAssignedToProject && !isProgramDirector) {
                throw new SecurityException("No tiene permiso para modificar este documento");
            }

            boolean canEditContent = existing.getProject() == null || isAssignedToProject;

            if (canEditContent) {
                if (!Objects.equals(existing.getContent(), document.getContent())) {
                    documentVersionRepository.save(
                            new DocumentVersion(
                                    existing,
                                    existing.getContent(),
                                    securityService.getCurrentUser()));
                    existing.setContent(document.getContent());
                }
                existing.setTitle(document.getTitle());
            }

            if (isProgramDirector) {
                if (document.getRating() != null) {
                    double r = document.getRating();
                    if (r < 0 || r > 10) {
                        throw new IllegalArgumentException("La valoración debe estar entre 0 y 10");
                    }
                    if (BigDecimal.valueOf(r).stripTrailingZeros().scale() > 2) {
                        throw new IllegalArgumentException("La valoración no puede tener más de 2 decimales");
                    }
                }
                existing.setRating(document.getRating());
            }

            existing.setUpdatedAt(LocalDateTime.now());
            return documentRepository.save(existing);
        }

        if (currentUser.getProject() == null) {
            throw new IllegalStateException("El usuario no tiene un proyecto asignado");
        }

        document.setUpdatedAt(LocalDateTime.now());
        document.setProject(currentUser.getProject());
        return documentRepository.save(document);
    }

    public Document get(Long id) {
        return documentRepository.findByIdWithProjectAndProgram(id).orElse(null);
    }

    public void delete(Long id) {
        if (securityService.getCurrentUser() == null) {
            throw new SecurityException("Debe autenticarse para eliminar documentos");
        }

        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));

        if (document.getProject() != null && !document.getProject().equals(
                userService.findByUvusWithProject(securityService.getCurrentUser().getUvus()).getProject())) {
            throw new SecurityException("No tiene permiso para eliminar este documento");
        }

        documentRepository.delete(document);
    }

    public String buildUrl(Document document) {
        String titulo = document.getTitle().toLowerCase()
                .replaceAll("[^a-z0-9\\s]+", "").trim().replaceAll("\\s+", "-");
        return "document/" + document.getId() + "-" + titulo;
    }

    public List<Document> getDocumentsByProject(Long projectId) {
        return documentRepository.findByProjectId(projectId);
    }
}
