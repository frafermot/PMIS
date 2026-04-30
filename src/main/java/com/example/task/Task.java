package com.example.task;

import com.example.project.Project;
import com.example.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "task_id", nullable = false)
    private Long id;

    @Column(name = "task_name", nullable = false)
    @NotBlank
    private String name;

    @Column(name = "task_description", length = 1000)
    private String description;

    @Column(name = "start_date", nullable = false)
    @NotNull
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    @NotNull
    private LocalDate endDate;

    @Column(name = "baseline_start_date")
    private LocalDate baselineStartDate;

    @Column(name = "baseline_end_date")
    private LocalDate baselineEndDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @NotNull
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "predecessor_id")
    private Task predecessor;

    @Enumerated(EnumType.STRING)
    @Column(name = "dependency_type")
    private TaskDependencyType dependencyType = TaskDependencyType.NONE;

    @Column(name = "is_group")
    private boolean isGroup = false;

    @Column(name = "duration")
    private Integer duration;

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    @Column(name = "is_milestone")
    private boolean isMilestone = false;

    @Column(name = "is_critical")
    private boolean isCritical = false;

    @Column(name = "wbs_code")
    private String wbsCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_group_id")
    private Task parentGroup;

    @OneToMany(mappedBy = "parentGroup", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<Task> subTasks = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    public Task getPredecessor() {
        return predecessor;
    }

    public void setPredecessor(Task predecessor) {
        this.predecessor = predecessor;
    }

    public TaskDependencyType getDependencyType() {
        return dependencyType;
    }

    public void setDependencyType(TaskDependencyType dependencyType) {
        this.dependencyType = dependencyType;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public void setGroup(boolean isGroup) {
        this.isGroup = isGroup;
    }

    public boolean isMilestone() {
        return isMilestone;
    }

    public void setMilestone(boolean milestone) {
        isMilestone = milestone;
    }

    public boolean isCritical() {
        return isCritical;
    }

    public void setCritical(boolean critical) {
        isCritical = critical;
    }

    public String getWbsCode() {
        return wbsCode;
    }

    public void setWbsCode(String wbsCode) {
        this.wbsCode = wbsCode;
    }

    public Task getParentGroup() {
        return parentGroup;
    }

    public void setParentGroup(Task parentGroup) {
        this.parentGroup = parentGroup;
    }

    public List<Task> getSubTasks() {
        return subTasks;
    }

    public void setSubTasks(List<Task> subTasks) {
        this.subTasks = subTasks;
    }

    public LocalDate getBaselineStartDate() {
        return baselineStartDate;
    }

    public void setBaselineStartDate(LocalDate baselineStartDate) {
        this.baselineStartDate = baselineStartDate;
    }

    public LocalDate getBaselineEndDate() {
        return baselineEndDate;
    }

    public void setBaselineEndDate(LocalDate baselineEndDate) {
        this.baselineEndDate = baselineEndDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task)) return false;
        Task task = (Task) o;
        return getId() != null && getId().equals(task.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
