package com.hrbotproject.hr_bot.repository;

import com.hrbotproject.hr_bot.model.Employee;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface EmployeeRepository extends MongoRepository<Employee, String> {
    Optional<Employee> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByRole(Employee.Role role);
    long countByRoleAndOnboardingCompleteFalse(Employee.Role role);
}
