package com.example.document;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import com.example.user.User;

@Entity
@Table(name = "document_version")
public class DocumentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "document_version_id", nullable = false)
    @NotNull
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    @NotNull
    private Document document;

    @Lob
    @Column(name = "document_version_content")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "document_version_created_at")
    private LocalDateTime createdAt;

    public DocumentVersion() {
    }

    public DocumentVersion(Document document, String content, User createdBy) {
        this.document = document;
        this.content = content;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
