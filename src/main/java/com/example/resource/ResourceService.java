package com.example.resource;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;

    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    public List<Resource> findAll() {
        return resourceRepository.findAll();
    }

    public Optional<Resource> findById(Long id) {
        return resourceRepository.findById(id);
    }

    public Resource save(Resource resource) {
        return resourceRepository.save(resource);
    }

    public void deleteById(Long id) {
        resourceRepository.deleteById(id);
    }
}
