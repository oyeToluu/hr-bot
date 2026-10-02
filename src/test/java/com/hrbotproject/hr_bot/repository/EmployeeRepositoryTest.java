package com.hrbotproject.hr_bot.repository;

import com.hrbotproject.hr_bot.model.Employee;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;

import static org.assertj.core.api.Assertions.assertThat;

// Spring Boot 4.1.1 note: @DataMongoTest lives at
// org.springframework.boot.data.mongodb.test.autoconfigure (this package
// changed between 4.0 and 4.1 - verified against the 4.1.1 API docs).
//
// This test needs a reachable MongoDB instance. Your pom.xml doesn't include
// an embedded-Mongo library (e.g. flapdoodle), so @DataMongoTest will try to
// connect to a real Mongo - most likely the one from compose.yaml. I could
// not verify in this sandbox whether Spring's docker-compose support starts
// that container automatically for a test-slice context the same way it
// does for the full app. If this test fails to connect when you run it,
// start Mongo manually first:
//   docker compose up -d
// then re-run `./mvnw test`.
@DataMongoTest
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;
    
    @BeforeEach 
    void startClean() {
        employeeRepository.deleteAll();
    }

    @AfterEach
    void cleanUp() {
        employeeRepository.deleteAll();
    }

    @Test
    void save_thenFindByEmail_returnsTheEmployee() {
        Employee employee = Employee.builder()
                .fullName("Ada Okafor")
                .email("ada@hrbots.test")
                .department("Engineering")
                .build();

        employeeRepository.save(employee);

        assertThat(employeeRepository.findByEmail("ada@hrbots.test")).isPresent();
        assertThat(employeeRepository.existsByEmail("ada@hrbots.test")).isTrue();
        assertThat(employeeRepository.existsByEmail("nobody@hrbots.test")).isFalse();
    }

    @Test
    void countByRole_countsOnlyMatchingRole() {
        employeeRepository.save(Employee.builder()
                .fullName("Ada").email("ada@hrbots.test").role(Employee.Role.EMPLOYEE).build());
        employeeRepository.save(Employee.builder()
                .fullName("Toni").email("toni@hrbots.test").role(Employee.Role.HR_ADMIN).build());

        assertThat(employeeRepository.countByRole(Employee.Role.EMPLOYEE)).isEqualTo(1);
        assertThat(employeeRepository.countByRole(Employee.Role.HR_ADMIN)).isEqualTo(1);
    }
}
