package com.hrbotproject.hr_bot.repository;

import com.hrbotproject.hr_bot.model.TodoItem;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TodoRepository extends MongoRepository<TodoItem, String> {
    List<TodoItem> findByEmployeeId(String employeeId);
}