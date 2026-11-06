package com.hrbotproject.hr_bot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * "Talk to a person in HR": an employee hands a bot chat over to their own
 * company's HR team. Holds the chat with the bot plus every message after it.
 */
@Document(collection = "support_tickets")
public class SupportTicket {

    public enum Status { OPEN, ANSWERED, CLOSED }

    /** Which bot the employee was talking to when they asked for a person. */
    public enum Source { HR_ASSISTANT, ONBOARDING_BOT, DIRECT }

    public enum Author { EMPLOYEE, BOT, HR }

    /** One line in the conversation. */
    public static class Message {
        private Author author;
        private String authorName;
        private String text;
        private LocalDateTime at;

        public Message() {}

        public Message(Author author, String authorName, String text, LocalDateTime at) {
            this.author = author;
            this.authorName = authorName;
            this.text = text;
            this.at = at;
        }

        public Author getAuthor() { return author; }
        public void setAuthor(Author author) { this.author = author; }
        public String getAuthorName() { return authorName; }
        public void setAuthorName(String authorName) { this.authorName = authorName; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public LocalDateTime getAt() { return at; }
        public void setAt(LocalDateTime at) { this.at = at; }
    }

    @Id
    private String id;

    @Indexed
    private String companyId;

    @Indexed
    private String employeeId;
    private String employeeName;
    private String employeeDepartment;

    private Source source = Source.DIRECT;
    private String subject;
    private Status status = Status.OPEN;
    private List<Message> messages = new ArrayList<>();

    /** True when the other side has written something this side hasn't opened yet. */
    private boolean unreadByHr = true;
    private boolean unreadByEmployee = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getEmployeeDepartment() { return employeeDepartment; }
    public void setEmployeeDepartment(String employeeDepartment) { this.employeeDepartment = employeeDepartment; }
    public Source getSource() { return source; }
    public void setSource(Source source) { this.source = source; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }
    public boolean isUnreadByHr() { return unreadByHr; }
    public void setUnreadByHr(boolean unreadByHr) { this.unreadByHr = unreadByHr; }
    public boolean isUnreadByEmployee() { return unreadByEmployee; }
    public void setUnreadByEmployee(boolean unreadByEmployee) { this.unreadByEmployee = unreadByEmployee; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}