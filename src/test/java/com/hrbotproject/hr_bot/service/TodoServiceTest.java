package com.hrbotproject.hr_bot.service;

import com.hrbotproject.hr_bot.dto.TodoDtos.AssignTodoRequest;
import com.hrbotproject.hr_bot.dto.TodoDtos.CreateTodoRequest;
import com.hrbotproject.hr_bot.dto.TodoDtos.UpdateTodoRequest;
import com.hrbotproject.hr_bot.model.Employee;
import com.hrbotproject.hr_bot.model.TodoItem;
import com.hrbotproject.hr_bot.repository.EmployeeRepository;
import com.hrbotproject.hr_bot.repository.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    private TodoService todoService;

    @BeforeEach
    void setUp() {
        todoService = new TodoService(todoRepository, employeeRepository);
    }

    private TodoItem task(String id, String owner, String title, LocalDate due, boolean done) {
        TodoItem t = new TodoItem();
        t.setId(id);
        t.setEmployeeId(owner);
        t.setTitle(title);
        t.setDueDate(due);
        t.setDone(done);
        return t;
    }

    @Test
    @DisplayName("mine() lists open tasks first, earliest due date first, undated last")
    void mine_sortsOpenFirstByDueDate() {
        when(todoRepository.findByEmployeeId("ada")).thenReturn(List.of(
                task("1", "ada", "Done task", LocalDate.of(2026, 11, 1), true),
                task("2", "ada", "No date", null, false),
                task("3", "ada", "Later", LocalDate.of(2026, 11, 20), false),
                task("4", "ada", "Sooner", LocalDate.of(2026, 11, 5), false)));

        List<TodoItem> list = todoService.mine("ada");

        assertThat(list).extracting(TodoItem::getTitle)
                .containsExactly("Sooner", "Later", "No date", "Done task");
    }

    @Test
    @DisplayName("create() trims the title and saves it on the employee's own list")
    void create_savesForOwner() {
        when(todoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TodoItem saved = todoService.create("ada", new CreateTodoRequest("  Submit bank details  ", null, " "));

        assertThat(saved.getEmployeeId()).isEqualTo("ada");
        assertThat(saved.getTitle()).isEqualTo("Submit bank details");
        assertThat(saved.getNotes()).isNull();
        assertThat(saved.isAssignedByHr()).isFalse();
    }

    @Test
    @DisplayName("update() marks a task done and records when")
    void update_marksDone() {
        when(todoRepository.findById("1")).thenReturn(Optional.of(task("1", "ada", "Read handbook", null, false)));
        when(todoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TodoItem saved = todoService.update("1", "ada", new UpdateTodoRequest(null, null, null, true, null));

        assertThat(saved.isDone()).isTrue();
        assertThat(saved.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("someone else's task looks like it doesn't exist")
    void update_otherPeoplesTaskIsNotFound() {
        when(todoRepository.findById("1")).thenReturn(Optional.of(task("1", "bola", "Private", null, false)));

        assertThatThrownBy(() -> todoService.update("1", "ada", new UpdateTodoRequest(null, null, null, true, null)))
                .isInstanceOf(NoSuchElementException.class);
        verify(todoRepository, never()).save(any());
    }

    @Test
    @DisplayName("HR-assigned tasks can be ticked off but not edited or deleted")
    void hrAssignedTask_onlyDoneCanChange() {
        TodoItem fromHr = task("1", "ada", "Sign NDA", LocalDate.of(2026, 11, 3), false);
        fromHr.setAssignedById("hr-1");
        when(todoRepository.findById("1")).thenReturn(Optional.of(fromHr));

        assertThatThrownBy(() -> todoService.update("1", "ada", new UpdateTodoRequest("Skip it", null, null, null, null)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> todoService.delete("1", "ada"))
                .isInstanceOf(IllegalStateException.class);
        verify(todoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("assign() puts the task on the employee's list with the HR admin's name")
    void assign_addsTaskFromHr() {
        Employee hr = Employee.builder().id("hr-1").fullName("Toni HR Admin").build();
        Employee ada = Employee.builder().id("ada").fullName("Ada Okafor").build();
        when(employeeRepository.findById("ada")).thenReturn(Optional.of(ada));
        when(todoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TodoItem saved = todoService.assign(hr,
                new AssignTodoRequest("ada", "Submit bank details", LocalDate.of(2026, 11, 3), null));

        assertThat(saved.getEmployeeId()).isEqualTo("ada");
        assertThat(saved.isAssignedByHr()).isTrue();
        assertThat(saved.getAssignedByName()).isEqualTo("Toni HR Admin");
    }

    @Test
    @DisplayName("assign() rejects an employee who doesn't exist")
    void assign_unknownEmployee() {
        Employee hr = Employee.builder().id("hr-1").fullName("Toni HR Admin").build();
        when(employeeRepository.findById("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> todoService.assign(hr, new AssignTodoRequest("nobody", "Task", null, null)))
                .isInstanceOf(NoSuchElementException.class);
    }
}