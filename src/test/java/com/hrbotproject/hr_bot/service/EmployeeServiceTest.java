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

// EmployeeService is read-only (staff join through the company's invite link
// at /api/auth/register). HR only ever sees their own company's staff.
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
    @DisplayName("findById() returns the employee when they are in HR's company")
    void findById_returnsEmployee() {
        Employee employee = Employee.builder().id("emp-1").fullName("Ada").email("ada@hrbots.test").companyId("co-1").build();
        when(employeeRepository.findById("emp-1")).thenReturn(Optional.of(employee));

        assertThat(employeeService.findById("co-1", "emp-1")).isEqualTo(employee);
    }

    @Test
    @DisplayName("findById() hides staff of another company")
    void findById_hidesOtherCompanies() {
        Employee employee = Employee.builder().id("emp-1").fullName("Ada").email("ada@hrbots.test").companyId("co-1").build();
        when(employeeRepository.findById("emp-1")).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> employeeService.findById("co-2", "emp-1"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("findById() throws for an unknown id")
    void findById_throwsWhenMissing() {
        when(employeeRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.findById("co-1", "ghost"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("findAll() lists only the given company's staff")
    void findAll_onlyOneCompany() {
        List<Employee> expected = List.of(Employee.builder().id("emp-1").companyId("co-1").build());
        when(employeeRepository.findByCompanyId("co-1")).thenReturn(expected);

        assertThat(employeeService.findAll("co-1")).isEqualTo(expected);
    }
}
