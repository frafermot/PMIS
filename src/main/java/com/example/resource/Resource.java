package com.example.resource;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "resources")
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "resource_id", nullable = false)
    @NotNull
    private Long id;

    @Column(name = "resource_type", nullable = false)
    @NotBlank
    private String resourceType;

    @Column(name = "professional_profile")
    private String professionalProfile;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getProfessionalProfile() {
        return professionalProfile;
    }

    public void setProfessionalProfile(String professionalProfile) {
        this.professionalProfile = professionalProfile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Resource)) return false;
        Resource resource = (Resource) o;
        return id != null && id.equals(resource.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
