package com.hrbotproject.hr_bot.repository;

import com.hrbotproject.hr_bot.model.LeaveRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface LeaveRequestRepository extends MongoRepository<LeaveRequest, String> {
    List<LeaveRequest> findByEmployeeIdOrderByCreatedAtDesc(String employeeId);
    List<LeaveRequest> findByStatusOrderByCreatedAtDesc(LeaveRequest.Status status);
    List<LeaveRequest> findAllByOrderByCreatedAtDesc();
    
    // ---- one company's requests ----
    List<LeaveRequest> findByCompanyIdOrderByCreatedAtDesc(String companyId);
    List<LeaveRequest> findByCompanyIdAndStatusOrderByCreatedAtDesc(String companyId, LeaveRequest.Status status);
    long countByCompanyIdAndStatus(String companyId, LeaveRequest.Status status);
}
