package com.example.communication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.example.project.Project;

class CccServiceTest {

    @Mock
    private CccRepository cccRepository;

    @InjectMocks
    private CccService cccService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateCccForProject() {
        Project project = new Project();
        Ccc ccc = new Ccc();
        ccc.setProject(project);
        
        when(cccRepository.save(any(Ccc.class))).thenReturn(ccc);
        
        Ccc created = cccService.createCccForProject(project);
        assertNotNull(created);
        assertEquals(project, created.getProject());
        verify(cccRepository).save(any(Ccc.class));
    }

    @Test
    void testGetCccByProject() {
        Ccc ccc = new Ccc();
        when(cccRepository.findByProjectId(1L)).thenReturn(Optional.of(ccc));
        
        Optional<Ccc> result = cccService.getCccByProject(1L);
        assertTrue(result.isPresent());
        assertEquals(ccc, result.get());
    }

    @Test
    void testFindById() {
        Ccc ccc = new Ccc();
        Project project = new Project();
        project.setName("Test Project");
        ccc.setProject(project);
        
        when(cccRepository.findById(1L)).thenReturn(Optional.of(ccc));
        
        Optional<Ccc> result = cccService.findById(1L);
        assertTrue(result.isPresent());
        assertEquals(ccc, result.get());
    }

    @Test
    void testDelete() {
        cccService.delete(1L);
        verify(cccRepository).deleteById(1L);
    }
}
