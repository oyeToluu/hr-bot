package com.hrbotproject.hr_bot.service;

import com.hrbotproject.hr_bot.model.EmailCode;
import com.hrbotproject.hr_bot.model.EmailCode.Purpose;
import com.hrbotproject.hr_bot.repository.EmailCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Creates, emails and checks 6-digit codes.
 * Codes last 10 minutes, allow 5 tries, and can be re-sent at most once a minute.
 */
@Service
public class EmailCodeService {

    static final Duration LIFETIME = Duration.ofMinutes(10);
    static final Duration RESEND_WAIT = Duration.ofSeconds(60);
    static final Duration RESEND_WINDOW = Duration.ofMinutes(30);
    static final int MAX_ATTEMPTS = 5;

    private final EmailCodeRepository repository;
    private final EmailSender sender;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public EmailCodeService(EmailCodeRepository repository, EmailSender sender) {
        this(repository, sender, Clock.systemUTC());
    }

    EmailCodeService(EmailCodeRepository repository, EmailSender sender, Clock clock) {
        this.repository = repository;
        this.sender = sender;
        this.clock = clock;
    }

    /**
     * Emails a new code. If one was sent less than a minute ago, either skips
     * quietly (it's already on its way) or, when strict, tells the user to wait.
     */
    public void issue(String email, Purpose purpose, String name, boolean strict) {
        Instant now = clock.instant();
        String id = EmailCode.idFor(email, purpose);
        Optional<EmailCode> existing = repository.findById(id);
        if (existing.isPresent() && existing.get().getLastSentAt() != null
                && now.isBefore(existing.get().getLastSentAt().plus(RESEND_WAIT))) {
            if (strict) throw new IllegalStateException("Please wait a minute before asking for a new code.");
            return;
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        sender.send(email, subject(purpose, code), html(purpose, name, code), text(purpose, name, code));

        EmailCode c = new EmailCode();
        c.setId(id);
        c.setEmail(email);
        c.setPurpose(purpose);
        c.setCodeHash(hash(email, purpose, code));
        c.setExpiresAt(now.plus(LIFETIME));
        c.setLastSentAt(now);
        c.setAttempts(0);
        repository.save(c);
    }

    /** True if a code for this purpose was sent recently (so "Resend code" is allowed). */
    public boolean hasRecent(String email, Purpose purpose) {
        return repository.findById(EmailCode.idFor(email, purpose))
                .map(c -> c.getLastSentAt() != null
                        && clock.instant().isBefore(c.getLastSentAt().plus(RESEND_WINDOW)))
                .orElse(false);
    }

    /** Checks a code. Throws a friendly message if it's wrong, expired or out of tries. */
    public void verify(String email, Purpose purpose, String code) {
        String id = EmailCode.idFor(email, purpose);
        EmailCode c = repository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("That code has expired. Please ask for a new one."));

        if (clock.instant().isAfter(c.getExpiresAt())) {
            repository.deleteById(id);
            throw new IllegalArgumentException("That code has expired. Please ask for a new one.");
        }

        String given = code == null ? "" : code.trim();
        boolean matches = MessageDigest.isEqual(
                hash(email, purpose, given).getBytes(StandardCharsets.UTF_8),
                c.getCodeHash().getBytes(StandardCharsets.UTF_8));
        if (!matches) {
            int attempts = c.getAttempts() + 1;
            if (attempts >= MAX_ATTEMPTS) {
                repository.deleteById(id);
                throw new IllegalArgumentException("Too many wrong tries. Please ask for a new code.");
            }
            c.setAttempts(attempts);
            repository.save(c);
            int left = MAX_ATTEMPTS - attempts;
            throw new IllegalArgumentException(
                    "That code isn't right. You have " + left + (left == 1 ? " try" : " tries") + " left.");
        }
        repository.deleteById(id); // a code works only once
    }

    static String hash(String email, Purpose purpose, String code) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] digest = sha.digest((email + ":" + purpose + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------- email wording ----------

    private static String subject(Purpose purpose, String code) {
        return switch (purpose) {
            case SIGNUP -> code + " is your HR Bots confirmation code";
            case LOGIN -> code + " is your HR Bots sign-in code";
            case RESET -> code + " is your HR Bots password reset code";
        };
    }

    private static String intro(Purpose purpose) {
        return switch (purpose) {
            case SIGNUP -> "Welcome to HR Bots! Use this code to confirm your email address:";
            case LOGIN -> "Someone (hopefully you) is signing in to HR Bots on a new device. Use this code to continue:";
            case RESET -> "Use this code to reset your HR Bots password:";
        };
    }

    private static String text(Purpose purpose, String name, String code) {
        return "Hi " + firstName(name) + ",\n\n" + intro(purpose) + "\n\n" + code
                + "\n\nThe code expires in 10 minutes. If you didn't ask for it, you can ignore this email.\n\nHR Bots";
    }

    private static String html(Purpose purpose, String name, String code) {
        return """
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:480px;margin:0 auto;padding:24px;color:#2e2443">
                  <p style="font-size:18px;font-weight:bold;color:#a3456c;margin:0 0 16px">HR Bots</p>
                  <p>Hi %s,</p>
                  <p>%s</p>
                  <p style="font-size:32px;letter-spacing:8px;font-weight:bold;background:#f8e6ee;border-radius:12px;padding:16px;text-align:center;margin:24px 0">%s</p>
                  <p style="color:#6b6280;font-size:14px">The code expires in 10 minutes. If you didn't ask for it, you can safely ignore this email.</p>
                </div>
                """.formatted(escape(firstName(name)), intro(purpose), code);
    }

    private static String firstName(String name) {
        if (name == null || name.isBlank()) return "there";
        return name.trim().split("\\s+")[0];
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}