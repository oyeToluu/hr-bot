package com.hrbotproject.hr_bot.repository;

import com.hrbotproject.hr_bot.model.Employee;
import org.springframework.data.mongodb.repository.MongoRepository;


import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends MongoRepository<Employee, String> {
    Optional<Employee> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByRole(Employee.Role role);
    long countByRoleAndOnboardingCompleteFalse(Employee.Role role);
    
    // ---- one company's staff ----
    List<Employee> findByCompanyId(String companyId);
    long countByCompanyId(String companyId);
    long countByCompanyIdAndRole(String companyId, Employee.Role role);
    long countByCompanyIdAndRoleAndOnboardingCompleteFalse(String companyId, Employee.Role role);
    List<Employee> findByCompanyIdAndRole(String companyId, Employee.Role role);

}
