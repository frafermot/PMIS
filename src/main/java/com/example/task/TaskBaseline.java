package com.example.task;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_baseline")
public class TaskBaseline {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_baseline_id", nullable = false)
    @NotNull
    private ProjectBaseline projectBaseline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    @NotNull
    private Task task;

    @Column(name = "start_date", nullable = false)
    @NotNull
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    @NotNull
    private LocalDateTime endDate;

    @Column(name = "duration")
    private Integer duration;

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProjectBaseline getProjectBaseline() {
        return projectBaseline;
    }

    public void setProjectBaseline(ProjectBaseline projectBaseline) {
        this.projectBaseline = projectBaseline;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }
}
