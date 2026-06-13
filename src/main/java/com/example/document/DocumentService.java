package com.example.document;

import com.example.project.Project;
import com.example.security.SecurityService;
import com.example.user.User;
import com.example.user.UserService;
import com.example.task.Task;
import com.example.task.TaskRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.math.BigDecimal;

@Service
@Transactional
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final SecurityService securityService;
    private final UserService userService;
    private final TemplateService templateService;
    private final TaskRepository taskRepository;

    public DocumentService(DocumentRepository documentRepository,
            DocumentVersionRepository documentVersionRepository,
            SecurityService securityService,
            UserService userService,
            TemplateService templateService,
            TaskRepository taskRepository) {
        this.documentRepository = documentRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.securityService = securityService;
        this.userService = userService;
        this.templateService = templateService;
        this.taskRepository = taskRepository;
    }

    /**
     * Seeds one Document placeholder (POR_CREAR) per DocumentType for a project.
     * Skips types that already have a document.
     */
    public void initDocumentsForProject(Project project) {
        for (DocumentType type : DocumentType.values()) {
            boolean hasPorCrear = documentRepository.existsByProjectIdAndTypeAndStatus(project.getId(), type, DocumentStatus.POR_CREAR);
            if (!hasPorCrear) {
                if (!type.isMultiple()) {
                    boolean exists = documentRepository.findFirstByProjectIdAndTypeOrderByIdDesc(project.getId(), type).isPresent();
                    if (exists) continue;
                }
                Document doc = new Document();
                doc.setTitle(type.getLabel());
                doc.setType(type);
                doc.setStatus(DocumentStatus.POR_CREAR);
                doc.setProject(project);
                doc.setUpdatedAt(LocalDateTime.now());
                documentRepository.save(doc);
            }
        }
    }

    /**
     * Creates the actual content for a POR_CREAR document (loads template, sets
     * EN_PROCESO).
     */
    public Document createDocument(Long projectId, DocumentType type) {
        User currentUser = userService.findByUvusWithProject(securityService.getCurrentUser().getUvus());
        if (currentUser == null || currentUser.getProject() == null
                || !currentUser.getProject().getId().equals(projectId)) {
            throw new SecurityException("No tiene permiso para crear documentos en este proyecto");
        }

        Document doc = documentRepository.findFirstByProjectIdAndTypeOrderByIdDesc(projectId, type)
                .filter(d -> d.getStatus() == DocumentStatus.POR_CREAR)
                .orElseThrow(() -> new IllegalStateException("Documento no encontrado o ya creado para tipo: " + type));

        if (doc.getStatus() != DocumentStatus.POR_CREAR) {
            throw new IllegalStateException("El documento ya ha sido creado");
        }

        String template = templateService.loadTemplate(type);
        
        // Autorellenar metadatos del proyecto
        template = DocumentHtmlHelper.fillProjectMetadata(template, doc.getProject());
        
        // Si es el registro de actividades o de hitos, precargar la lista actual de tareas
        if (type == DocumentType.LISTA_ACTIVIDADES) {
            List<Task> tasks = taskRepository.findByProjectOrderByStartDateAsc(doc.getProject());
            template = DocumentHtmlHelper.updateActivitiesTableInHtml(template, tasks);
        } else if (type == DocumentType.LISTA_HITOS) {
            List<Task> tasks = taskRepository.findByProjectOrderByStartDateAsc(doc.getProject());
            template = DocumentHtmlHelper.updateMilestonesTableInHtml(template, tasks);
        }

        doc.setContent(template);
        doc.setStatus(DocumentStatus.EN_PROCESO);
        doc.setUpdatedAt(LocalDateTime.now());
        doc = documentRepository.save(doc);
        documentVersionRepository.save(new DocumentVersion(doc, template, null));
        
        if (type.isMultiple()) {
            Document newPlaceholder = new Document();
            newPlaceholder.setTitle(type.getLabel());
            newPlaceholder.setType(type);
            newPlaceholder.setStatus(DocumentStatus.POR_CREAR);
            newPlaceholder.setProject(doc.getProject());
            newPlaceholder.setUpdatedAt(LocalDateTime.now());
            documentRepository.save(newPlaceholder);
        }
        
        return doc;
    }

    /**
     * Updates an existing document's content. Only allowed when status !=
     * POR_CREAR.
     */
    public Document createOrUpdate(Document document) {
        User currentUser = userService.findByUvusWithProject(securityService.getCurrentUser().getUvus());
        if (currentUser == null) {
            throw new SecurityException("Debe autenticarse para guardar documentos");
        }

        if (document.getId() != null) {
            Document existing = documentRepository.findById(document.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));

            if (!Objects.equals(existing.getVersion(), document.getVersion())) {
                throw new org.springframework.orm.ObjectOptimisticLockingFailureException(Document.class, document.getId());
            }

            boolean canRate = securityService.isAdminOrManager();

            boolean isAssignedToProject = existing.getProject() != null
                    && existing.getProject().equals(currentUser.getProject());

            if (!isAssignedToProject && !canRate) {
                throw new SecurityException("No tiene permiso para modificar este documento");
            }

            // Cannot edit content if status is POR_CREAR
            if (existing.getStatus() == DocumentStatus.POR_CREAR) {
                throw new IllegalStateException(
                        "El documento debe ser creado primero antes de poder modificarse");
            }

            boolean canEditContent = existing.getProject() == null || isAssignedToProject;

            if (canEditContent) {
                if (!Objects.equals(existing.getContent(), document.getContent())) {
                    existing.setContent(document.getContent());
                    documentVersionRepository.save(
                            new DocumentVersion(
                                    existing,
                                    existing.getContent(),
                                    securityService.getCurrentUser()));
                }
            }

            if (canRate) {
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

            if (document.getStatus() != null) {
                existing.setStatus(document.getStatus());
            }

            existing.setUpdatedAt(LocalDateTime.now());
            return documentRepository.save(existing);
        }

        // Creating a new document directly — only allowed internally
        // (initDocumentsForProject / createDocument)
        if (document.getType() == null) {
            throw new IllegalArgumentException("El tipo de documento es obligatorio");
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

        List<DocumentVersion> versions = getVersions(id);
        if (versions != null && !versions.isEmpty()) {
            documentVersionRepository.deleteAll(versions);
        }

        documentRepository.delete(document);
    }

    public String buildUrl(Document document) {
        return "document/" + document.getId();
    }

    public List<Document> getDocumentsByProject(Long projectId) {
        return documentRepository.findByProjectId(projectId);
    }

    public List<DocumentVersion> getVersions(Long documentId) {
        return documentVersionRepository.findByDocumentIdOrderByCreatedAtDesc(documentId);
    }

    /**
     * Returns a map of DocumentType → Document for a project, with one entry per
     * type.
     */
    public Map<DocumentType, List<Document>> getProjectDocumentMap(Long projectId) {
        List<Document> docs = documentRepository.findByProjectId(projectId);
        Map<DocumentType, List<Document>> map = new EnumMap<>(DocumentType.class);
        for (Document d : docs) {
            if (d.getType() != null) {
                map.computeIfAbsent(d.getType(), k -> new java.util.ArrayList<>()).add(d);
            }
        }
        // Ensure all types are present (fallback for projects not yet seeded)
        for (DocumentType type : DocumentType.values()) {
            map.putIfAbsent(type, new java.util.ArrayList<>());
        }
        return map;
    }
}
