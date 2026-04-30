package com.example.task;

import com.example.project.Project;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectBaselineRepository projectBaselineRepository;
    private final TaskBaselineRepository taskBaselineRepository;

    public TaskService(TaskRepository taskRepository, 
                       ProjectBaselineRepository projectBaselineRepository,
                       TaskBaselineRepository taskBaselineRepository) {
        this.taskRepository = taskRepository;
        this.projectBaselineRepository = projectBaselineRepository;
        this.taskBaselineRepository = taskBaselineRepository;
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
            Project project = task.getProject();
            
            switch (task.getDependencyType()) {
                case FINISH_TO_START:
                    LocalDate nextDay = getNextWorkingDay(task.getPredecessor().getEndDate().plusDays(1), project);
                    if (task.getStartDate().isBefore(nextDay)) {
                        task.setStartDate(nextDay);
                        task.setEndDate(addWorkingDays(task.getStartDate(), durationDays, project));
                    }
                    break;
                case START_TO_START:
                    if (task.getStartDate().isBefore(task.getPredecessor().getStartDate())) {
                        task.setStartDate(task.getPredecessor().getStartDate());
                        task.setEndDate(addWorkingDays(task.getStartDate(), durationDays, project));
                    }
                    break;
                case FINISH_TO_FINISH:
                    if (task.getEndDate().isBefore(task.getPredecessor().getEndDate())) {
                        task.setEndDate(task.getPredecessor().getEndDate());
                        task.setStartDate(subtractWorkingDays(task.getEndDate(), durationDays, project));
                    }
                    break;
                case START_TO_FINISH:
                    LocalDate startToFinishDay = getNextWorkingDay(task.getPredecessor().getStartDate().plusDays(1), project);
                    if (task.getEndDate().isBefore(startToFinishDay)) {
                        task.setEndDate(startToFinishDay);
                        task.setStartDate(subtractWorkingDays(task.getEndDate(), durationDays, project));
                    }
                    break;
                case NONE:
                default:
                    break;
            }
        }
    }

    private LocalDate getNextWorkingDay(LocalDate date, Project project) {
        LocalDate current = date;
        while (!isWorkingDay(current, project)) {
            current = current.plusDays(1);
        }
        return current;
    }

    private boolean isWorkingDay(LocalDate date, Project project) {
        String workingDaysStr = project.getWorkingDays();
        if (workingDaysStr == null || workingDaysStr.isEmpty()) return true;
        Set<DayOfWeek> workingDays = Arrays.stream(workingDaysStr.split(","))
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toSet());
        return workingDays.contains(date.getDayOfWeek());
    }

    private LocalDate addWorkingDays(LocalDate start, long duration, Project project) {
        LocalDate result = start;
        int added = 0;
        while (added < duration) {
            result = result.plusDays(1);
            if (isWorkingDay(result, project)) {
                added++;
            }
        }
        return result;
    }

    private LocalDate subtractWorkingDays(LocalDate end, long duration, Project project) {
        LocalDate result = end;
        int subtracted = 0;
        while (subtracted < duration) {
            result = result.minusDays(1);
            if (isWorkingDay(result, project)) {
                subtracted++;
            }
        }
        return result;
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

    @Transactional
    public void assignWBS(Project project) {
        List<Task> allTasks = taskRepository.findByProjectOrderByStartDateAsc(project);
        List<Task> topLevel = allTasks.stream().filter(t -> t.getParentGroup() == null).toList();
        
        int counter = 1;
        for (Task t : topLevel) {
            String code = "1." + counter;
            t.setWbsCode(code);
            taskRepository.save(t);
            assignWBSChildren(t, code, allTasks);
            counter++;
        }
    }

    private void assignWBSChildren(Task parent, String parentCode, List<Task> allTasks) {
        List<Task> children = allTasks.stream().filter(t -> parent.equals(t.getParentGroup())).toList();
        int counter = 1;
        for (Task child : children) {
            String code = parentCode + "." + counter;
            child.setWbsCode(code);
            taskRepository.save(child);
            assignWBSChildren(child, code, allTasks);
            counter++;
        }
    }

    @Transactional
    public void calculateCriticalPath(Project project) {
        List<Task> allTasks = taskRepository.findByProjectOrderByStartDateAsc(project);
        
        for (Task t : allTasks) {
            t.setCritical(false);
        }

        if (allTasks.isEmpty()) return;
        
        LocalDate maxEndDate = allTasks.stream().map(Task::getEndDate).max(LocalDate::compareTo).orElse(null);
        if (maxEndDate == null) return;
        
        List<Task> terminalTasks = allTasks.stream()
            .filter(t -> t.getEndDate().equals(maxEndDate))
            .toList();
            
        for (Task t : terminalTasks) {
            markCriticalPath(t);
        }
        
        taskRepository.saveAll(allTasks);
    }
    
    private void markCriticalPath(Task task) {
        if (task == null || task.isCritical()) return;
        task.setCritical(true);
        if (task.getPredecessor() != null && task.getDependencyType() != TaskDependencyType.NONE) {
            markCriticalPath(task.getPredecessor());
        }
        if (task.isGroup()) {
            for (Task child : task.getSubTasks()) {
                if (child.getEndDate().equals(task.getEndDate())) {
                    markCriticalPath(child);
                }
            }
        }
    }

    @Transactional
    public void setBaselineForProject(Project project) {
        setBaselineForProject(project, "Línea Base " + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
    }

    @Transactional
    public ProjectBaseline setBaselineForProject(Project project, String name) {
        ProjectBaseline pb = new ProjectBaseline();
        pb.setProject(project);
        pb.setName(name);
        pb = projectBaselineRepository.save(pb);

        List<Task> allTasks = taskRepository.findByProjectOrderByStartDateAsc(project);
        for (Task t : allTasks) {
            TaskBaseline tb = new TaskBaseline();
            tb.setProjectBaseline(pb);
            tb.setTask(t);
            tb.setStartDate(t.getStartDate());
            tb.setEndDate(t.getEndDate());
            taskBaselineRepository.save(tb);
            
            // Also update the legacy fields in Task for backward compatibility/simplicity in some views
            t.setBaselineStartDate(t.getStartDate());
            t.setBaselineEndDate(t.getEndDate());
            taskRepository.save(t);
        }
        return pb;
    }

    public List<ProjectBaseline> getBaselinesByProject(Project project) {
        return projectBaselineRepository.findByProjectOrderByCreatedAtDesc(project);
    }

    public List<TaskBaseline> getTaskBaselines(ProjectBaseline pb) {
        return taskBaselineRepository.findByProjectBaseline(pb);
    }

    public java.util.Map<Long, TaskBaseline> getTaskBaselineMap(ProjectBaseline pb) {
        if (pb == null) return new java.util.HashMap<>();
        return getTaskBaselines(pb).stream()
                .collect(Collectors.toMap(tb -> tb.getTask().getId(), tb -> tb));
    }
}
