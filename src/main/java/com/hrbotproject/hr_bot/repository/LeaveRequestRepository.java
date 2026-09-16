package com.hrbotproject.hr_bot.repository;

import com.hrbotproject.hr_bot.model.LeaveRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface LeaveRequestRepository extends MongoRepository<LeaveRequest, String> {
    List<LeaveRequest> findByEmployeeIdOrderByCreatedAtDesc(String employeeId);
    List<LeaveRequest> findByStatusOrderByCreatedAtDesc(LeaveRequest.Status status);
    List<LeaveRequest> findAllByOrderByCreatedAtDesc();
}
