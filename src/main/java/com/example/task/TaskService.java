package com.example.task;

import com.example.project.Project;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<Task> getTasksByProject(Project project) {
        return taskRepository.findByProjectOrderByStartDateAsc(project);
    }

    public Task getTaskById(Long id) {
        return taskRepository.findById(id).orElse(null);
    }

    public Task getFirstChildOfGroup(Task group) {
        if (group == null || !group.isGroup()) return group;
        List<Task> children = taskRepository.findByProjectOrderByStartDateAsc(group.getProject())
            .stream().filter(t -> group.equals(t.getParentGroup())).toList();
        if (children.isEmpty()) return null;
        return children.get(0);
    }

    @Transactional
    public Task saveTask(Task task) {
        if (isCircularDependency(task, task.getPredecessor())) {
            throw new IllegalStateException("Dependencia circular detectada: la tarea no puede depender de sí misma o de una sucesora suya.");
        }

        if (!task.isGroup()) {
            validateAssigneeAvailability(task);
            calculateDatesBasedOnDependency(task);
        }
        
        Task savedTask = taskRepository.save(task);
        
        // Update successors if this task's dates changed
        updateSuccessorDates(savedTask);
        
        // Update parent group dates
        if (savedTask.getParentGroup() != null) {
            updateGroupDates(savedTask.getParentGroup());
        }
        
        return savedTask;
    }

    @Transactional
    public Task createTaskGroup(Project project, String groupName, List<Task> selectedTasks) {
        if (selectedTasks == null || selectedTasks.isEmpty()) return null;

        Task group = new Task();
        group.setProject(project);
        group.setName(groupName);
        group.setGroup(true);
        group.setStartDate(selectedTasks.get(0).getStartDate());
        group.setEndDate(selectedTasks.get(0).getEndDate());
        
        Task savedGroup = taskRepository.save(group);

        for (Task t : selectedTasks) {
            t.setParentGroup(savedGroup);
            taskRepository.save(t);
        }

        updateGroupDates(savedGroup);
        return savedGroup;
    }

    private void updateGroupDates(Task group) {
        if (group == null || !group.isGroup()) return;
        
        List<Task> children = taskRepository.findByProjectOrderByStartDateAsc(group.getProject())
            .stream().filter(t -> group.equals(t.getParentGroup())).toList();
            
        if (children.isEmpty()) return;

        LocalDate minStart = children.stream().map(Task::getStartDate).min(LocalDate::compareTo).orElse(group.getStartDate());
        LocalDate maxEnd = children.stream().map(Task::getEndDate).max(LocalDate::compareTo).orElse(group.getEndDate());

        group.setStartDate(minStart);
        group.setEndDate(maxEnd);
        taskRepository.save(group);

        if (group.getParentGroup() != null) {
            updateGroupDates(group.getParentGroup());
        }
    }

    @Transactional
    public void deleteTask(Task task) {
        // First detach any successors
        List<Task> successors = taskRepository.findByPredecessor(task);
        for (Task successor : successors) {
            successor.setPredecessor(null);
            successor.setDependencyType(TaskDependencyType.NONE);
            taskRepository.save(successor);
        }
        taskRepository.delete(task);
    }

    private void validateAssigneeAvailability(Task task) {
        if (task.getAssignee() != null) {
            Long excludeId = task.getId() != null ? task.getId() : -1L;
            List<Task> overlappingTasks = taskRepository.findOverlappingTasksForAssignee(
                    task.getAssignee(), task.getStartDate(), task.getEndDate(), excludeId);
            
            if (!overlappingTasks.isEmpty()) {
                throw new IllegalStateException("El usuario " + task.getAssignee().getName() + 
                    " ya tiene tareas asignadas en esas fechas y no puede solaparse.");
            }
        }
    }

    private void calculateDatesBasedOnDependency(Task task) {
        if (task.getPredecessor() != null && task.getDependencyType() != TaskDependencyType.NONE) {
            long durationDays = ChronoUnit.DAYS.between(task.getStartDate(), task.getEndDate());
            
            switch (task.getDependencyType()) {
                case FINISH_TO_START:
                    if (task.getStartDate().isBefore(task.getPredecessor().getEndDate().plusDays(1))) {
                        task.setStartDate(task.getPredecessor().getEndDate().plusDays(1));
                        task.setEndDate(task.getStartDate().plusDays(durationDays));
                    }
                    break;
                case START_TO_START:
                    if (task.getStartDate().isBefore(task.getPredecessor().getStartDate())) {
                        task.setStartDate(task.getPredecessor().getStartDate());
                        task.setEndDate(task.getStartDate().plusDays(durationDays));
                    }
                    break;
                case FINISH_TO_FINISH:
                    if (task.getEndDate().isBefore(task.getPredecessor().getEndDate())) {
                        task.setEndDate(task.getPredecessor().getEndDate());
                        task.setStartDate(task.getEndDate().minusDays(durationDays));
                    }
                    break;
                case START_TO_FINISH:
                    if (task.getEndDate().isBefore(task.getPredecessor().getStartDate().plusDays(1))) {
                        task.setEndDate(task.getPredecessor().getStartDate().plusDays(1));
                        task.setStartDate(task.getEndDate().minusDays(durationDays));
                    }
                    break;
                case NONE:
                default:
                    break;
            }
        }
    }

    private void updateSuccessorDates(Task predecessor) {
        List<Task> successors = taskRepository.findByPredecessor(predecessor);
        for (Task successor : successors) {
            calculateDatesBasedOnDependency(successor);
            // Recursively update downstream tasks
            saveTask(successor);
        }
    }

    private boolean isCircularDependency(Task task, Task potentialPredecessor) {
        if (task == null || potentialPredecessor == null) return false;
        if (task.equals(potentialPredecessor) || (task.getId() != null && task.getId().equals(potentialPredecessor.getId()))) {
            return true;
        }
        
        Task pred = potentialPredecessor.getPredecessor();
        while (pred != null) {
            if (task.equals(pred) || (task.getId() != null && task.getId().equals(pred.getId()))) {
                return true;
            }
            pred = pred.getPredecessor();
        }
        return false;
    }
}
