package com.hrbotproject.hr_bot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Sends emails through Resend (resend.com) over HTTPS. Render's free plan blocks
 * normal email (SMTP), so a web API is the way to send from there.
 * Without RESEND_API_KEY (e.g. on your laptop) nothing is sent: the email is
 * printed in the console instead, so you can still read the code while testing.
 */
@Component
public class EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final String apiKey;
    private final String from;
    private final RestClient restClient;

    public EmailSender(
            @Value("${app.mail.resend-api-key:}") String apiKey,
            @Value("${app.mail.from:HR Bots <onboarding@resend.dev>}") String from,
            @Value("${app.mail.url:https://api.resend.com/emails}") String url
    ) {
        this.apiKey = apiKey;
        this.from = from;
        SimpleClientHttpRequestFactory timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(Duration.ofSeconds(5));
        timeouts.setReadTimeout(Duration.ofSeconds(15));
        this.restClient = RestClient.builder().baseUrl(url).requestFactory(timeouts).build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public void send(String to, String subject, String html, String text) {
        if (!isConfigured()) {
            log.warn("[email not sent: RESEND_API_KEY is not set] To: {} | {} | {}", to, subject, text);
            return;
        }
        try {
            restClient.post()
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "from", from,
                            "to", List.of(to),
                            "subject", subject,
                            "html", html,
                            "text", text))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException e) {
            log.warn("Email to {} failed: {}", to, e.getMessage());
            throw new IllegalStateException("We couldn't send the email right now. Please try again in a few minutes.");
        }
    }
}