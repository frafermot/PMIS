package com.example.task;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.example.project.Project;

class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ProjectBaselineRepository projectBaselineRepository;
    @Mock
    private TaskBaselineRepository taskBaselineRepository;

    @InjectMocks
    private TaskService taskService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetTasksByProject() {
        Project project = new Project();
        List<Task> tasks = new ArrayList<>();
        tasks.add(new Task());
        when(taskRepository.findByProjectOrderByStartDateAsc(project)).thenReturn(tasks);
        
        List<Task> result = taskService.getTasksByProject(project);
        assertEquals(1, result.size());
    }

    @Test
    void testGetTaskById() {
        Task task = new Task();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        assertEquals(task, taskService.getTaskById(1L));
    }

    @Test
    void testSaveTaskNoDependency() {
        Project project = new Project();
        Task task = new Task();
        task.setProject(project);
        when(taskRepository.save(task)).thenReturn(task);
        
        Task saved = taskService.saveTask(task);
        assertNotNull(saved);
        verify(taskRepository, times(1)).save(task);
    }

    @Test
    void testCircularDependencyThrowsException() {
        Task task1 = new Task();
        task1.setId(1L);
        Task task2 = new Task();
        task2.setId(2L);
        
        task1.setPredecessor(task2);
        task2.setPredecessor(task1);
        
        assertThrows(IllegalStateException.class, () -> taskService.saveTask(task1));
    }

    @Test
    void testDeleteTask() {
        Task task = new Task();
        when(taskRepository.findByPredecessor(task)).thenReturn(new ArrayList<>());
        taskService.deleteTask(task);
        verify(taskRepository).delete(task);
    }
}
