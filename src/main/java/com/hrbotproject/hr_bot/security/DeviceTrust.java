package com.hrbotproject.hr_bot.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * "Remember this device for 30 days". After someone enters an emailed code, their
 * browser gets a signed device token. While it's valid, logging in from that browser
 * doesn't need another code. Changing the password makes all old device tokens
 * stop working, because each token is tied to the current password.
 */
@Component
public class DeviceTrust {

    static final Duration TRUST_FOR = Duration.ofDays(30);

    private final byte[] key;
    private final Clock clock;

    @Autowired
    public DeviceTrust(@Value("${app.jwt.secret}") String secret) {
        this(secret, Clock.systemUTC());
    }

    DeviceTrust(String secret, Clock clock) {
        this.key = sha256("device-trust:" + secret);
        this.clock = clock;
    }

    public String issue(String email, String passwordHash) {
        long expires = clock.instant().plus(TRUST_FOR).getEpochSecond();
        String payload = email.toLowerCase(java.util.Locale.ROOT) + "|" + expires + "|" + fingerprint(passwordHash);
        String body = b64(payload.getBytes(StandardCharsets.UTF_8));
        return body + "." + b64(hmac(body));
    }

    public boolean isTrusted(String token, String email, String passwordHash) {
        if (token == null || email == null || passwordHash == null) return false;
        int dot = token.indexOf('.');
        if (dot <= 0) return false;
        String body = token.substring(0, dot);
        byte[] givenSig;
        try {
            givenSig = Base64.getUrlDecoder().decode(token.substring(dot + 1));
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (!MessageDigest.isEqual(hmac(body), givenSig)) return false;

        String[] parts;
        try {
            parts = new String(Base64.getUrlDecoder().decode(body), StandardCharsets.UTF_8).split("\\|");
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (parts.length != 3) return false;
        try {
            return parts[0].equals(email.toLowerCase(java.util.Locale.ROOT))
                    && Instant.ofEpochSecond(Long.parseLong(parts[1])).isAfter(clock.instant())
                    && parts[2].equals(fingerprint(passwordHash));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String fingerprint(String passwordHash) {
        return HexFormat.of().formatHex(sha256(passwordHash)).substring(0, 16);
    }

    private static byte[] sha256(String s) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String b64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}