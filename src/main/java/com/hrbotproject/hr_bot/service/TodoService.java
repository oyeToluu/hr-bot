package com.hrbotproject.hr_bot.service;

import com.hrbotproject.hr_bot.dto.TodoDtos.AssignTodoRequest;
import com.hrbotproject.hr_bot.dto.TodoDtos.CreateTodoRequest;
import com.hrbotproject.hr_bot.dto.TodoDtos.UpdateTodoRequest;
import com.hrbotproject.hr_bot.model.Employee;
import com.hrbotproject.hr_bot.model.TodoItem;
import com.hrbotproject.hr_bot.repository.EmployeeRepository;
import com.hrbotproject.hr_bot.repository.TodoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class TodoService {

    /** Open tasks first, then the earliest due date (tasks with no date last), then newest. */
    static final Comparator<TodoItem> LIST_ORDER = Comparator
            .comparing(TodoItem::isDone)
            .thenComparing(TodoItem::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(TodoItem::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));

    private final TodoRepository todoRepository;
    private final EmployeeRepository employeeRepository;

    public TodoService(TodoRepository todoRepository, EmployeeRepository employeeRepository) {
        this.todoRepository = todoRepository;
        this.employeeRepository = employeeRepository;
    }

    /** The logged-in employee's own list. */
    public List<TodoItem> mine(String employeeId) {
        return sorted(todoRepository.findByEmployeeId(employeeId));
    }

    public TodoItem create(String employeeId, CreateTodoRequest req) {
        TodoItem item = new TodoItem();
        item.setEmployeeId(employeeId);
        item.setTitle(req.title().trim());
        item.setDueDate(req.dueDate());
        item.setNotes(blankToNull(req.notes()));
        item.setCreatedAt(LocalDateTime.now());
        return todoRepository.save(item);
    }

    /**
     * Employees can tick off any task on their list. They can only edit the
     * wording or date of tasks they added themselves, not ones HR assigned.
     */
    public TodoItem update(String id, String employeeId, UpdateTodoRequest req) {
        TodoItem item = findOwned(id, employeeId);

        boolean editsContent = req.title() != null || req.dueDate() != null || req.notes() != null
                || Boolean.TRUE.equals(req.clearDueDate());
        if (editsContent && item.isAssignedByHr()) {
            throw new IllegalStateException("Tasks assigned by HR can only be marked done or not done");
        }

        if (req.title() != null) {
            if (req.title().isBlank()) throw new IllegalArgumentException("title must not be blank");
            item.setTitle(req.title().trim());
        }
        if (Boolean.TRUE.equals(req.clearDueDate())) {
            item.setDueDate(null);
        } else if (req.dueDate() != null) {
            item.setDueDate(req.dueDate());
        }
        if (req.notes() != null) {
            item.setNotes(blankToNull(req.notes()));
        }
        if (req.done() != null && req.done() != item.isDone()) {
            item.setDone(req.done());
            item.setCompletedAt(req.done() ? LocalDateTime.now() : null);
        }
        return todoRepository.save(item);
    }

    public void delete(String id, String employeeId) {
        TodoItem item = findOwned(id, employeeId);
        if (item.isAssignedByHr()) {
            throw new IllegalStateException("Tasks assigned by HR can't be deleted. Mark it done instead.");
        }
        todoRepository.delete(item);
    }

    // ---------- HR admin ----------

    public TodoItem assign(Employee hr, AssignTodoRequest req) {
        Employee target = employeeRepository.findById(req.employeeId())
                .orElseThrow(() -> new NoSuchElementException("Employee not found"));

        TodoItem item = new TodoItem();
        item.setEmployeeId(target.getId());
        item.setTitle(req.title().trim());
        item.setDueDate(req.dueDate());
        item.setNotes(blankToNull(req.notes()));
        item.setAssignedById(hr.getId());
        item.setAssignedByName(hr.getFullName());
        item.setCreatedAt(LocalDateTime.now());
        return todoRepository.save(item);
    }

    /**
     * The tasks HR has assigned to one employee, so HR can follow up.
     * The employee's own personal tasks stay private to them.
     */
    public List<TodoItem> assignedByHr(String employeeId) {
        return sorted(todoRepository.findByEmployeeId(employeeId).stream()
                .filter(TodoItem::isAssignedByHr)
                .toList());
    }

    // ---------- helpers ----------

    /**
     * Someone else's task looks exactly like a missing one (404), so ids
     * can't be used to find out what is on other people's lists.
     */
    private TodoItem findOwned(String id, String employeeId) {
        TodoItem item = todoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Task not found"));
        if (!item.getEmployeeId().equals(employeeId)) {
            throw new NoSuchElementException("Task not found");
        }
        return item;
    }

    private static List<TodoItem> sorted(List<TodoItem> items) {
        return items.stream().sorted(LIST_ORDER).toList();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}