package com.hrbotproject.hr_bot.repository;

import com.hrbotproject.hr_bot.model.SupportTicket;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SupportTicketRepository extends MongoRepository<SupportTicket, String> {
    List<SupportTicket> findByEmployeeIdOrderByUpdatedAtDesc(String employeeId);
    List<SupportTicket> findByCompanyIdOrderByUpdatedAtDesc(String companyId);
    List<SupportTicket> findByCompanyIdAndStatusOrderByUpdatedAtDesc(String companyId, SupportTicket.Status status);
    long countByCompanyIdAndStatus(String companyId, SupportTicket.Status status);
}