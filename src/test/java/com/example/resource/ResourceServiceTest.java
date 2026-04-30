package com.example.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @InjectMocks
    private ResourceService resourceService;

    private Resource resource1;
    private Resource resource2;

    @BeforeEach
    void setUp() {
        resource1 = new Resource();
        resource1.setId(1L);
        resource1.setResourceType("HUMAN");
        resource1.setProfessionalProfile("SENIOR");

        resource2 = new Resource();
        resource2.setId(2L);
        resource2.setResourceType("MATERIAL");
        resource2.setProfessionalProfile("SERVER");
    }

    @Test
    void findAll_ShouldReturnAllResources() {
        when(resourceRepository.findAll()).thenReturn(Arrays.asList(resource1, resource2));

        List<Resource> result = resourceService.findAll();

        assertEquals(2, result.size());
        assertEquals("HUMAN", result.get(0).getResourceType());
        verify(resourceRepository, times(1)).findAll();
    }

    @Test
    void findById_WhenResourceExists_ShouldReturnResource() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource1));

        Optional<Resource> result = resourceService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        assertEquals("HUMAN", result.get().getResourceType());
        verify(resourceRepository, times(1)).findById(1L);
    }

    @Test
    void findById_WhenResourceDoesNotExist_ShouldReturnEmptyOptional() {
        when(resourceRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Resource> result = resourceService.findById(99L);

        assertFalse(result.isPresent());
        verify(resourceRepository, times(1)).findById(99L);
    }

    @Test
    void save_ShouldSaveAndReturnResource() {
        when(resourceRepository.save(any(Resource.class))).thenReturn(resource1);

        Resource result = resourceService.save(resource1);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(resourceRepository, times(1)).save(resource1);
    }

    @Test
    void deleteById_ShouldDeleteResource() {
        doNothing().when(resourceRepository).deleteById(1L);

        resourceService.deleteById(1L);

        verify(resourceRepository, times(1)).deleteById(1L);
    }
}
