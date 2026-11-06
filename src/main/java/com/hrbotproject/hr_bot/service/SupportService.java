package com.hrbotproject.hr_bot.service;

import com.hrbotproject.hr_bot.dto.SupportDtos.ChatLine;
import com.hrbotproject.hr_bot.dto.SupportDtos.OpenTicketRequest;
import com.hrbotproject.hr_bot.dto.SupportDtos.SupportCounts;
import com.hrbotproject.hr_bot.model.Employee;
import com.hrbotproject.hr_bot.model.SupportTicket;
import com.hrbotproject.hr_bot.model.SupportTicket.Author;
import com.hrbotproject.hr_bot.model.SupportTicket.Message;
import com.hrbotproject.hr_bot.model.SupportTicket.Status;
import com.hrbotproject.hr_bot.repository.EmployeeRepository;
import com.hrbotproject.hr_bot.repository.SupportTicketRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Hand-over from the HR bots to a person: an employee's request goes to the
 * HR admins of their own company only, never to anyone else.
 */
@Service
public class SupportService {

    static final int MAX_CHAT_LINES = 20;

    private final SupportTicketRepository ticketRepository;
    private final EmployeeRepository employeeRepository;
    private final EmailSender emailSender;
    private final String webUrl;

    public SupportService(
            SupportTicketRepository ticketRepository,
            EmployeeRepository employeeRepository,
            EmailSender emailSender,
            @Value("${app.web-url:https://hr-bot-ashy.vercel.app}") String webUrl
    ) {
        this.ticketRepository = ticketRepository;
        this.employeeRepository = employeeRepository;
        this.emailSender = emailSender;
        this.webUrl = webUrl.endsWith("/") ? webUrl.substring(0, webUrl.length() - 1) : webUrl;
    }

    // ---------- employee ----------

    /** "Talk to a person in HR": copies the bot chat and the employee's message into a new request. */
    public SupportTicket open(Employee employee, OpenTicketRequest req) {
        LocalDateTime now = LocalDateTime.now();
        SupportTicket t = new SupportTicket();
        t.setCompanyId(employee.getCompanyId());
        t.setEmployeeId(employee.getId());
        t.setEmployeeName(employee.getFullName());
        t.setEmployeeDepartment(employee.getDepartment());
        t.setSource(req.source() == null ? SupportTicket.Source.DIRECT : req.source());
        t.setSubject(shorten(req.message().trim(), 80));

        List<ChatLine> chat = req.chat() == null ? List.of() : req.chat();
        // Keep only the last few lines so one request can't grow without limit
        for (ChatLine line : chat.subList(Math.max(0, chat.size() - MAX_CHAT_LINES), chat.size())) {
            boolean fromBot = "bot".equals(line.from());
            t.getMessages().add(new Message(fromBot ? Author.BOT : Author.EMPLOYEE,
                    fromBot ? "HR bot" : employee.getFullName(), line.text().trim(), now));
        }
        t.getMessages().add(new Message(Author.EMPLOYEE, employee.getFullName(), req.message().trim(), now));
        t.setStatus(Status.OPEN);
        t.setUnreadByHr(true);
        t.setUnreadByEmployee(false);
        t.setCreatedAt(now);
        t.setUpdatedAt(now);
        SupportTicket saved = ticketRepository.save(t);

        emailHr(saved, employee.getFullName() + " asked for help from HR",
                employee.getFullName() + " asked the HR bot for a person to help with: \"" + saved.getSubject() + "\"");
        return saved;
    }

    public List<SupportTicket> mine(String employeeId) {
        return ticketRepository.findByEmployeeIdOrderByUpdatedAtDesc(employeeId);
    }

    /** One of my own requests. Opening it marks HR's replies as read. */
    public SupportTicket mineOne(Employee me, String id) {
        SupportTicket t = owned(me, id);
        if (t.isUnreadByEmployee()) {
            t.setUnreadByEmployee(false);
            t = ticketRepository.save(t);
        }
        return t;
    }

    public SupportTicket employeeReply(Employee me, String id, String text) {
        SupportTicket t = owned(me, id);
        if (t.getStatus() == Status.CLOSED) {
            throw new IllegalStateException("This request is closed. Please start a new one.");
        }
        add(t, Author.EMPLOYEE, me.getFullName(), text);
        t.setStatus(Status.OPEN);
        t.setUnreadByHr(true);
        SupportTicket saved = ticketRepository.save(t);
        emailHr(saved, me.getFullName() + " replied to their HR request",
                me.getFullName() + " replied about: \"" + saved.getSubject() + "\"");
        return saved;
    }

    /** The employee says their question is sorted. */
    public SupportTicket employeeClose(Employee me, String id) {
        SupportTicket t = owned(me, id);
        t.setStatus(Status.CLOSED);
        t.setUpdatedAt(LocalDateTime.now());
        return ticketRepository.save(t);
    }

    // ---------- HR admin ----------

    public List<SupportTicket> inbox(String companyId, Status status) {
        return status == null
                ? ticketRepository.findByCompanyIdOrderByUpdatedAtDesc(companyId)
                : ticketRepository.findByCompanyIdAndStatusOrderByUpdatedAtDesc(companyId, status);
    }

    /** One request to HR's company. Opening it marks the employee's messages as read. */
    public SupportTicket inboxOne(String companyId, String id) {
        SupportTicket t = inCompany(companyId, id);
        if (t.isUnreadByHr()) {
            t.setUnreadByHr(false);
            t = ticketRepository.save(t);
        }
        return t;
    }

    public SupportTicket hrReply(Employee hr, String id, String text) {
        SupportTicket t = inCompany(hr.getCompanyId(), id);
        if (t.getStatus() == Status.CLOSED) {
            throw new IllegalStateException("This request is closed.");
        }
        add(t, Author.HR, hr.getFullName(), text);
        t.setStatus(Status.ANSWERED);
        t.setUnreadByHr(false);
        t.setUnreadByEmployee(true);
        SupportTicket saved = ticketRepository.save(t);

        employeeRepository.findById(saved.getEmployeeId()).ifPresent(e -> safeSend(e.getEmail(),
                "HR replied to your request",
                "Hi " + firstName(e.getFullName()) + ",\n\n" + hr.getFullName()
                        + " from HR replied to your request \"" + saved.getSubject() + "\".\n\n"
                        + "Read it here: " + webUrl + "/help\n\nHR Bots"));
        return saved;
    }

    public SupportTicket hrClose(String companyId, String id) {
        SupportTicket t = inCompany(companyId, id);
        t.setStatus(Status.CLOSED);
        t.setUnreadByHr(false);
        t.setUpdatedAt(LocalDateTime.now());
        return ticketRepository.save(t);
    }

    // ---------- badges for the menu ----------

    public SupportCounts countsForHr(String companyId) {
        List<SupportTicket> all = ticketRepository.findByCompanyIdOrderByUpdatedAtDesc(companyId);
        long waiting = all.stream().filter(t -> t.getStatus() == Status.OPEN).count();
        long unread = all.stream().filter(SupportTicket::isUnreadByHr).count();
        return new SupportCounts(waiting, unread);
    }

    public SupportCounts countsForEmployee(String employeeId) {
        List<SupportTicket> mine = mine(employeeId);
        long waiting = mine.stream().filter(t -> t.getStatus() == Status.OPEN).count();
        long unread = mine.stream().filter(SupportTicket::isUnreadByEmployee).count();
        return new SupportCounts(waiting, unread);
    }

    // ---------- helpers ----------

    /** Someone else's request looks exactly like one that doesn't exist. */
    private SupportTicket owned(Employee me, String id) {
        return ticketRepository.findById(id)
                .filter(t -> me.getId() != null && me.getId().equals(t.getEmployeeId()))
                .orElseThrow(() -> new NoSuchElementException("Request not found"));
    }

    private SupportTicket inCompany(String companyId, String id) {
        return ticketRepository.findById(id)
                .filter(t -> companyId != null && companyId.equals(t.getCompanyId()))
                .orElseThrow(() -> new NoSuchElementException("Request not found"));
    }

    private static void add(SupportTicket t, Author author, String name, String text) {
        LocalDateTime now = LocalDateTime.now();
        t.getMessages().add(new Message(author, name, text.trim(), now));
        t.setUpdatedAt(now);
    }

    /** Tells every HR admin of the ticket's company. Email problems never stop the request. */
    private void emailHr(SupportTicket t, String subject, String line) {
        for (Employee hr : employeeRepository.findByCompanyIdAndRole(t.getCompanyId(), Employee.Role.HR_ADMIN)) {
            safeSend(hr.getEmail(), subject, "Hi " + firstName(hr.getFullName()) + ",\n\n" + line
                    + "\n\nOpen your Help requests: " + webUrl + "/admin/support\n\nHR Bots");
        }
    }

    private void safeSend(String to, String subject, String text) {
        if (to == null || to.isBlank()) return;
        try {
            String html = "<div style=\"font-family:Arial,Helvetica,sans-serif;max-width:480px;margin:0 auto;padding:24px;color:#2e2443\">"
                    + "<p style=\"font-size:18px;font-weight:bold;color:#a3456c;margin:0 0 16px\">HR Bots</p>"
                    + escape(text).replace("\n", "<br>") + "</div>";
            emailSender.send(to, subject, html, text);
        } catch (RuntimeException e) {
            // The request is saved and shows in the app; the email is only a nudge
        }
    }

    private static String shorten(String s, int max) {
        String one = s.replaceAll("\\s+", " ");
        return one.length() <= max ? one : one.substring(0, max - 1).trim() + "…";
    }

    private static String firstName(String fullName) {
        return fullName == null || fullName.isBlank() ? "there" : fullName.trim().split("\\s+")[0];
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}