package com.hrbotproject.hr_bot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class TodoDtos {

    /** An employee adding a task to their own list. */
    public record CreateTodoRequest(
            @NotBlank @Size(max = 200, message = "title must be 200 characters or fewer") String title,
            LocalDate dueDate,
            @Size(max = 1000, message = "notes must be 1000 characters or fewer") String notes
    ) {}

    /**
     * Changing a task. Every field is optional; only the ones sent are changed.
     * On HR-assigned tasks the employee may only change "done".
     */
    public record UpdateTodoRequest(
            @Size(max = 200, message = "title must be 200 characters or fewer") String title,
            LocalDate dueDate,
            @Size(max = 1000, message = "notes must be 1000 characters or fewer") String notes,
            Boolean done,
            Boolean clearDueDate
    ) {}

    /** An HR admin giving a task to an employee. */
    public record AssignTodoRequest(
            @NotBlank String employeeId,
            @NotBlank @Size(max = 200, message = "title must be 200 characters or fewer") String title,
            LocalDate dueDate,
            @Size(max = 1000, message = "notes must be 1000 characters or fewer") String notes
    ) {}
}