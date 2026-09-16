package com.hrbotproject.hr_bot.dto;

import com.hrbotproject.hr_bot.model.Employee;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class EmployeeDtos {

    public record CreateEmployeeRequest(
            @NotBlank String fullName,
            @NotBlank @Email String email,
            String department,
            Employee.Role role
    ) {}
}
