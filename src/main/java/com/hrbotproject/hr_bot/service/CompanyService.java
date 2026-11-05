package com.hrbotproject.hr_bot.service;

import com.hrbotproject.hr_bot.dto.CompanyDtos.CompanyView;
import com.hrbotproject.hr_bot.dto.SecurityDtos.MessageResponse;
import com.hrbotproject.hr_bot.model.Company;
import com.hrbotproject.hr_bot.model.Employee;
import com.hrbotproject.hr_bot.repository.CompanyRepository;
import com.hrbotproject.hr_bot.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Companies (the businesses using HR Bots): signing up, the "join your
 * company" link HR shares with staff, and invite emails.
 */
@Service
public class CompanyService {

    static final int TRIAL_DAYS = 30;
    static final String BAD_INVITE =
            "This invite link isn't valid any more. Please ask your HR team for a new one.";

    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;
    private final EmailSender emailSender;
    private final String webUrl;
    private final SecureRandom random = new SecureRandom();

    public CompanyService(
            CompanyRepository companyRepository,
            EmployeeRepository employeeRepository,
            EmailSender emailSender,
            @Value("${app.web-url:https://hr-bot-ashy.vercel.app}") String webUrl
    ) {
        this.companyRepository = companyRepository;
        this.employeeRepository = employeeRepository;
        this.emailSender = emailSender;
        this.webUrl = webUrl.endsWith("/") ? webUrl.substring(0, webUrl.length() - 1) : webUrl;
    }

    /**
     * A new company on a free trial, with its own join link. The name is saved
     * exactly as registered, and no two companies may have the same name
     * ("Acme Ltd", "ACME LTD" and "Acme Ltd." all count as the same name).
     */
    public Company create(String name, String industry) {
        String slug = slugify(name);
        Optional<Company> taken = companyRepository.findBySlug(slug);
        if (taken.isPresent() && !releaseIfAbandoned(taken.get())) {
            throw new IllegalArgumentException("A company called \"" + taken.get().getName()
                    + "\" is already registered on HR Bots. If you work there, ask its HR team for an invite link.");
        }
        Company c = new Company();
        c.setName(name.trim());
        c.setIndustry(industry == null || industry.isBlank() ? null : industry.trim());
        c.setSlug(slug);
        c.setJoinCode(newJoinCode());
        c.setPlan(Company.Plan.FREE_TRIAL);
        c.setCreatedAt(LocalDateTime.now());
        c.setTrialEndsAt(c.getCreatedAt().plusDays(TRIAL_DAYS));
        return companyRepository.save(c);
    }

    public Company findById(String id) {
        if (id == null) throw new NoSuchElementException("Company not found");
        return companyRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Company not found"));
    }

    public Company bySlug(String slug) {
        return companyRepository.findBySlug(slug == null ? "" : slug.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new NoSuchElementException("Company not found"));
    }

    /** The company whose join link this is. */
    public Company byJoinCode(String code) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException(BAD_INVITE);
        return companyRepository.findByJoinCode(code.trim())
                .orElseThrow(() -> new IllegalArgumentException(BAD_INVITE));
    }

    /** The logged-in staff member's company. Only HR admins get the join link. */
    public CompanyView view(Employee me) {
        Company c = findById(me.getCompanyId());
        boolean hr = me.getRole() == Employee.Role.HR_ADMIN;
        return new CompanyView(c.getId(), c.getName(), c.getSlug(), c.getIndustry(), c.getPlan(),
                c.getTrialEndsAt(), employeeRepository.countByCompanyId(c.getId()),
                hr ? joinLink(c) : null);
    }

    /** HR: make a new join link. The old link stops working straight away. */
    public CompanyView resetJoinLink(Employee hr) {
        Company c = findById(hr.getCompanyId());
        c.setJoinCode(newJoinCode());
        companyRepository.save(c);
        return view(hr);
    }

    /** HR: email the join link to someone. */
    public MessageResponse invite(Employee hr, String rawEmail) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        Company c = findById(hr.getCompanyId());
        String link = joinLink(c);
        String subject = hr.getFullName() + " invited you to join " + c.getName() + " on HR Bots";
        String text = "Hi,\n\n" + hr.getFullName() + " has invited you to join " + c.getName()
                + " on HR Bots, where you can request leave, see your tasks and ask the HR assistant questions.\n\n"
                + "Create your account here:\n" + link + "\n\nHR Bots";
        String html = """
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:480px;margin:0 auto;padding:24px;color:#2e2443">
                  <p style="font-size:18px;font-weight:bold;color:#a3456c;margin:0 0 16px">HR Bots</p>
                  <p>Hi,</p>
                  <p><strong>%s</strong> has invited you to join <strong>%s</strong> on HR Bots, where you can request leave, see your tasks and ask the HR assistant questions.</p>
                  <p style="margin:28px 0;text-align:center"><a href="%s" style="background:#a3456c;color:#fff;text-decoration:none;padding:14px 24px;border-radius:999px;font-weight:bold">Create my account</a></p>
                  <p style="color:#6b6280;font-size:14px">Or copy this link into your browser:<br>%s</p>
                </div>
                """.formatted(escape(hr.getFullName()), escape(c.getName()), link, link);
        emailSender.send(email, subject, html, text);
        return new MessageResponse("Invite sent to " + email + ".");
    }

    /** Removes a company nobody belongs to (e.g. a sign-up that was never confirmed). */
    public void deleteIfEmpty(String companyId) {
        if (companyId != null && employeeRepository.countByCompanyId(companyId) == 0) {
            companyRepository.deleteById(companyId);
        }
    }

    String joinLink(Company c) {
        return webUrl + "/join/" + c.getJoinCode();
    }

    // ---------- helpers ----------

    private String newJoinCode() {
        byte[] bytes = new byte[12];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

        /**
     * A company sign-up that nobody ever confirmed by email doesn't hold on to
     * the name: it is removed so the real owner can register it.
     */
    private boolean releaseIfAbandoned(Company c) {
        List<Employee> staff = employeeRepository.findByCompanyId(c.getId());
        if (staff.stream().anyMatch(e -> !Boolean.FALSE.equals(e.getEmailVerified()))) {
            return false;
        }
        employeeRepository.deleteAll(staff);
        companyRepository.delete(c);
        return true;
    }

    /** The name used in web addresses: "Zenith Foods Ltd" -> "zenith-foods-ltd". */

    public static String slugify(String name) {
        String plain = Normalizer.normalize(name == null ? "" : name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = plain.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (slug.length() > 50) slug = slug.substring(0, 50).replaceAll("-+$", "");
        return slug.isEmpty() ? "company" : slug;
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}