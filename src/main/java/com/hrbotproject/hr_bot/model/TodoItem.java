package com.hrbotproject.hr_bot.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One task on an employee's to-do list. Either the employee added it
 * themselves (assignedById is null) or an HR admin assigned it to them.
 */
@Document(collection = "todos")
public class TodoItem {

    @Id
    private String id;

    /** The employee whose list this task is on. */
    @Indexed
    private String employeeId;

    private String title;
    private String notes;
    private LocalDate dueDate;
    private boolean done;
    private LocalDateTime completedAt;

    /** Set when an HR admin assigned the task; null for the employee's own tasks. */
    private String assignedById;
    private String assignedByName;

    @CreatedDate
    private LocalDateTime createdAt;

    /** True when HR assigned this task (the employee can tick it off but not edit or delete it). */
    public boolean isAssignedByHr() {
        return assignedById != null;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getAssignedById() { return assignedById; }
    public void setAssignedById(String assignedById) { this.assignedById = assignedById; }

    public String getAssignedByName() { return assignedByName; }
    public void setAssignedByName(String assignedByName) { this.assignedByName = assignedByName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}