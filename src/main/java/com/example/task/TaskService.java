package com.example.task;

import com.example.project.Project;
import com.example.user.User;
import com.example.document.DocumentRepository;
import com.example.document.Document;
import com.example.document.DocumentType;
import com.example.document.DocumentStatus;
import com.example.document.DocumentHtmlHelper;
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
    private final DocumentRepository documentRepository;

    public TaskService(TaskRepository taskRepository, 
                       ProjectBaselineRepository projectBaselineRepository,
                       TaskBaselineRepository taskBaselineRepository,
                       DocumentRepository documentRepository) {
        this.taskRepository = taskRepository;
        this.projectBaselineRepository = projectBaselineRepository;
        this.taskBaselineRepository = taskBaselineRepository;
        this.documentRepository = documentRepository;
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
        Task saved = saveTaskInternal(task, new java.util.HashSet<>());
        syncActivitiesDocument(saved.getProject());
        return saved;
    }

    private Task saveTaskInternal(Task task, java.util.Set<Long> updatedTaskIds) {
        if (task.getId() != null && updatedTaskIds.contains(task.getId())) {
            return task;
        }

        if (isCircularDependency(task, task.getPredecessor())) {
            throw new IllegalStateException("Dependencia circular detectada: la tarea no puede depender de sí misma o de una sucesora suya.");
        }

        if (!task.isGroup()) {
            validateAssigneeAvailability(task);
            calculateDatesBasedOnDependency(task);
        }
        
        Task savedTask = taskRepository.save(task);
        if (savedTask.getId() != null) {
            updatedTaskIds.add(savedTask.getId());
        }
        
        // Update successors if this task's dates changed
        updateSuccessorDatesInternal(savedTask, updatedTaskIds);
        
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
        syncActivitiesDocument(project);
        return savedGroup;
    }

    private void updateGroupDates(Task group) {
        if (group == null || !group.isGroup()) return;
        
        List<Task> children = taskRepository.findByProjectOrderByStartDateAsc(group.getProject())
            .stream().filter(t -> group.equals(t.getParentGroup())).toList();
            
        if (children.isEmpty()) return;

        LocalDateTime minStart = children.stream().map(Task::getStartDate).min(LocalDateTime::compareTo).orElse(group.getStartDate());
        LocalDateTime maxEnd = children.stream().map(Task::getEndDate).max(LocalDateTime::compareTo).orElse(group.getEndDate());

        group.setStartDate(minStart);
        group.setEndDate(maxEnd);
        taskRepository.save(group);

        if (group.getParentGroup() != null) {
            updateGroupDates(group.getParentGroup());
        }
    }

    @Transactional
    public void deleteTask(Task task) {
        Project project = task.getProject();
        // First detach any successors
        List<Task> successors = taskRepository.findByPredecessor(task);
        for (Task successor : successors) {
            successor.setPredecessor(null);
            successor.setDependencyType(TaskDependencyType.NONE);
            taskRepository.save(successor);
        }
        taskRepository.delete(task);
        syncActivitiesDocument(project);
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
            Project project = task.getProject();
            int durationHours;
            if (project.getDurationUnit().equals("HOURS")) {
                durationHours = task.getDuration() != null ? task.getDuration() : 1;
            } else {
                long hoursPerDay = ChronoUnit.HOURS.between(project.getWorkStartHour(), project.getWorkEndHour());
                durationHours = (int)((task.getDuration() != null ? task.getDuration() : 1) * hoursPerDay);
            }

            switch (task.getDependencyType()) {
                case FINISH_TO_START:
                    LocalDateTime fsStart = task.getPredecessor().getEndDate();
                    task.setStartDate(ensureWorkingTime(fsStart, project));
                    task.setEndDate(addWorkingHours(task.getStartDate(), durationHours, project));
                    break;
                case START_TO_START:
                    task.setStartDate(ensureWorkingTime(task.getPredecessor().getStartDate(), project));
                    task.setEndDate(addWorkingHours(task.getStartDate(), durationHours, project));
                    break;
                case FINISH_TO_FINISH:
                    task.setEndDate(ensureWorkingTime(task.getPredecessor().getEndDate(), project));
                    task.setStartDate(subtractWorkingHours(task.getEndDate(), durationHours, project));
                    break;
                case START_TO_FINISH:
                    task.setEndDate(ensureWorkingTime(task.getPredecessor().getStartDate(), project));
                    task.setStartDate(subtractWorkingHours(task.getEndDate(), durationHours, project));
                    break;
                case NONE:
                default:
                    break;
            }
        }
    }

    public LocalDateTime ensureWorkingTime(LocalDateTime dateTime, Project project) {
        LocalDateTime current = dateTime;
        while (!isWorkingDay(current.toLocalDate(), project)) {
            current = current.plusDays(1).with(project.getWorkStartHour());
        }
        if (current.toLocalTime().isBefore(project.getWorkStartHour())) {
            current = current.with(project.getWorkStartHour());
        } else if (current.toLocalTime().isAfter(project.getWorkEndHour())) {
            current = current.plusDays(1).with(project.getWorkStartHour());
            return ensureWorkingTime(current, project);
        }
        return current;
    }

    public LocalDateTime addWorkingHours(LocalDateTime start, int hours, Project project) {
        LocalDateTime current = ensureWorkingTime(start, project);
        int remainingHours = hours;
        
        while (remainingHours > 0) {
            LocalDateTime endOfDay = current.with(project.getWorkEndHour());
            long hoursToday = ChronoUnit.HOURS.between(current, endOfDay);
            
            if (hoursToday >= remainingHours) {
                current = current.plusHours(remainingHours);
                remainingHours = 0;
            } else {
                remainingHours -= (int)hoursToday;
                current = current.plusDays(1).with(project.getWorkStartHour());
                current = ensureWorkingTime(current, project);
            }
        }
        return current;
    }

    public LocalDateTime subtractWorkingHours(LocalDateTime end, int hours, Project project) {
        LocalDateTime current = end;
        if (current.toLocalTime().isBefore(project.getWorkStartHour())) {
            current = current.minusDays(1).with(project.getWorkEndHour());
        } else if (current.toLocalTime().isAfter(project.getWorkEndHour())) {
            current = current.with(project.getWorkEndHour());
        }
        
        int remainingHours = hours;
        while (remainingHours > 0) {
            LocalDateTime startOfDay = current.with(project.getWorkStartHour());
            long hoursToday = ChronoUnit.HOURS.between(startOfDay, current);
            
            if (hoursToday >= remainingHours) {
                current = current.minusHours(remainingHours);
                remainingHours = 0;
            } else {
                remainingHours -= (int)hoursToday;
                current = current.minusDays(1).with(project.getWorkEndHour());
                while (!isWorkingDay(current.toLocalDate(), project)) {
                    current = current.minusDays(1);
                }
            }
        }
        return current;
    }

    private void updateSuccessorDatesInternal(Task predecessor, java.util.Set<Long> updatedTaskIds) {
        List<Task> successors = taskRepository.findByPredecessor(predecessor);
        for (Task successor : successors) {
            calculateDatesBasedOnDependency(successor);
            // Recursively update downstream tasks
            saveTaskInternal(successor, updatedTaskIds);
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
        syncActivitiesDocument(project);
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
        
        LocalDateTime maxEnd = allTasks.stream().map(Task::getEndDate).max(LocalDateTime::compareTo).orElse(null);
        if (maxEnd == null) return;
        
        List<Task> terminalTasks = allTasks.stream()
            .filter(t -> t.getEndDate().equals(maxEnd))
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
            tb.setDuration(t.getDuration());
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

    public boolean isWorkingDay(LocalDate date, Project project) {
        String workingDaysStr = project.getWorkingDays();
        if (workingDaysStr == null || workingDaysStr.isEmpty()) return true;
        
        try {
            DayOfWeek day = date.getDayOfWeek();
            return workingDaysStr.contains(day.name());
        } catch (Exception e) {
            return true;
        }
    }
    @Transactional
    public void unassignUserFromProjectTasks(User user, Project project) {
        List<Task> tasks = taskRepository.findByProjectAndAssignee(project, user);
        for (Task t : tasks) {
            t.setAssignee(null);
            taskRepository.save(t);
        }
    }

    private void syncActivitiesDocument(Project project) {
        if (project == null || project.getId() == null) return;
        try {
            documentRepository.findByProjectIdAndType(project.getId(), DocumentType.REGISTRO_ACTIVIDADES)
                .ifPresent(doc -> {
                    if (doc.getStatus() == DocumentStatus.EN_PROCESO) {
                        List<Task> tasks = taskRepository.findByProjectOrderByStartDateAsc(project);
                        String updatedHtml = DocumentHtmlHelper.updateActivitiesTableInHtml(doc.getContent(), tasks);
                        if (!updatedHtml.equals(doc.getContent())) {
                            doc.setContent(updatedHtml);
                            doc.setUpdatedAt(LocalDateTime.now());
                            documentRepository.save(doc);
                        }
                    }
                });
        } catch (Exception e) {
            // Log exception to avoid breaking the core task operations
            e.printStackTrace();
        }
    }
}
