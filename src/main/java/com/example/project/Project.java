package com.example.project;

import com.example.user.User;
import com.example.program.Program;
import com.example.communication.Ccc;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Entity
@Table(name = "project")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "project_id", nullable = false)
    @NotNull
    private Long id;

    @Column(name = "project_name", nullable = false)
    @NotBlank
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "director_id")
    private User director;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    @NotNull
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sponsor_id")
    private User sponsor;

    @OneToOne(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private Ccc ccc;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "working_days")
    private String workingDays = "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY";

    @Column(name = "work_start_hour")
    private java.time.LocalTime workStartHour = java.time.LocalTime.of(9, 0);

    @Column(name = "work_end_hour")
    private java.time.LocalTime workEndHour = java.time.LocalTime.of(18, 0);

    @Column(name = "duration_unit")
    private String durationUnit = "DAYS"; // "DAYS" or "HOURS"

    public String getDurationUnit() {
        return durationUnit;
    }

    public void setDurationUnit(String durationUnit) {
        this.durationUnit = durationUnit;
    }

    public String getWorkingDays() {
        return workingDays;
    }

    public void setWorkingDays(String workingDays) {
        this.workingDays = workingDays;
    }

    public java.time.LocalTime getWorkStartHour() {
        return workStartHour;
    }

    public void setWorkStartHour(java.time.LocalTime workStartHour) {
        this.workStartHour = workStartHour;
    }

    public java.time.LocalTime getWorkEndHour() {
        return workEndHour;
    }

    public void setWorkEndHour(java.time.LocalTime workEndHour) {
        this.workEndHour = workEndHour;
    }

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

    public User getDirector() {
        return director;
    }

    public void setDirector(User director) {
        this.director = director;
    }

    public Program getProgram() {
        return program;
    }

    public void setProgram(Program program) {
        this.program = program;
    }

    public User getSponsor() {
        return sponsor;
    }

    public void setSponsor(User sponsor) {
        this.sponsor = sponsor;
    }

    public Ccc getCcc() {
        return ccc;
    }

    public void setCcc(Ccc ccc) {
        this.ccc = ccc;
        if (ccc != null && ccc.getProject() != this) {
            ccc.setProject(this);
        }
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Project))
            return false;
        Project project = (Project) o;
        return getId() != null && getId().equals(project.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
