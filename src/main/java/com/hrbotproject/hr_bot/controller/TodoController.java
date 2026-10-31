package com.hrbotproject.hr_bot.controller;

import com.hrbotproject.hr_bot.dto.TodoDtos.AssignTodoRequest;
import com.hrbotproject.hr_bot.dto.TodoDtos.CreateTodoRequest;
import com.hrbotproject.hr_bot.dto.TodoDtos.UpdateTodoRequest;
import com.hrbotproject.hr_bot.model.TodoItem;
import com.hrbotproject.hr_bot.security.EmployeePrincipal;
import com.hrbotproject.hr_bot.service.TodoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * To-do lists. The owner always comes from the login token, never from the
 * request, so employees only ever see and change their own tasks.
 * /assign and /employee/** are HR-admin only (enforced in SecurityConfig).
 */
@RestController
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping
    public List<TodoItem> mine(@AuthenticationPrincipal EmployeePrincipal me) {
        return todoService.mine(me.getEmployeeId());
    }

    @PostMapping
    public ResponseEntity<TodoItem> create(
            @AuthenticationPrincipal EmployeePrincipal me,
            @Valid @RequestBody CreateTodoRequest req
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(todoService.create(me.getEmployeeId(), req));
    }

    @PatchMapping("/{id}")
    public TodoItem update(
            @AuthenticationPrincipal EmployeePrincipal me,
            @PathVariable String id,
            @Valid @RequestBody UpdateTodoRequest req
    ) {
        return todoService.update(id, me.getEmployeeId(), req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal EmployeePrincipal me, @PathVariable String id) {
        todoService.delete(id, me.getEmployeeId());
        return ResponseEntity.noContent().build();
    }

    // ---------- HR admin ----------

    @PostMapping("/assign")
    public ResponseEntity<TodoItem> assign(
            @AuthenticationPrincipal EmployeePrincipal hr,
            @Valid @RequestBody AssignTodoRequest req
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(todoService.assign(hr.getEmployee(), req));
    }

    // Only the tasks HR assigned; the employee's personal tasks stay private.
    @GetMapping("/employee/{employeeId}")
    public List<TodoItem> assignedTo(@PathVariable String employeeId) {
        return todoService.assignedByHr(employeeId);
    }
}