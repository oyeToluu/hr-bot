package com.hrbotproject.hr_bot.service;

import com.hrbotproject.hr_bot.model.Employee;
import com.hrbotproject.hr_bot.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

// EmployeeService is read-only now (creation lives in AuthController via
// /api/auth/register, since that's the only path that sets a password).
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeService(employeeRepository);
    }

    @Test
    @DisplayName("findById() returns the employee when present")
    void findById_returnsEmployee() {
        Employee employee = Employee.builder().id("emp-1").fullName("Ada").email("ada@hrbots.test").build();
        when(employeeRepository.findById("emp-1")).thenReturn(Optional.of(employee));

        assertThat(employeeService.findById("emp-1")).isEqualTo(employee);
    }

    @Test
    @DisplayName("findById() throws for an unknown id")
    void findById_throwsWhenMissing() {
        when(employeeRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.findById("ghost"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("findAll() delegates to the repository")
    void findAll_delegatesToRepository() {
        List<Employee> expected = List.of(Employee.builder().id("emp-1").build());
        when(employeeRepository.findAll()).thenReturn(expected);

        assertThat(employeeService.findAll()).isEqualTo(expected);
    }
}
