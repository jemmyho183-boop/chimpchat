package com.chimpchat.api.auth.business;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import com.chimpchat.api.auth.entity.EmailLoginTokenEntity;
import com.chimpchat.api.auth.entity.UserEntity;
import com.chimpchat.api.auth.repository.EmailLoginTokenRepository;
import com.chimpchat.api.auth.repository.UserRepository;
import com.chimpchat.api.workspace.business.WorkspaceBusiness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailLinkBusiness {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailLoginTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final WorkspaceBusiness workspaceBusiness;
    private final LoginLinkRateLimitBusiness rateLimitBusiness;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final Duration tokenLifetime;
    private final String frontendLink;
    private final String senderAddress;
    private final String mailHost;
    private final boolean enabled;

    public EmailLinkBusiness(
            EmailLoginTokenRepository tokenRepository,
            UserRepository userRepository,
            WorkspaceBusiness workspaceBusiness,
            LoginLinkRateLimitBusiness rateLimitBusiness,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.auth.email-link.ttl:PT15M}") Duration tokenLifetime,
            @Value("${app.auth.email-link.base-url}") String frontendLink,
            @Value("${app.auth.email-link.from}") String senderAddress,
            @Value("${spring.mail.host:}") String mailHost,
            @Value("${app.auth.email.enabled:false}") boolean enabled) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.workspaceBusiness = workspaceBusiness;
        this.rateLimitBusiness = rateLimitBusiness;
        this.mailSenderProvider = mailSenderProvider;
        this.tokenLifetime = tokenLifetime;
        this.frontendLink = frontendLink;
        this.senderAddress = senderAddress;
        this.mailHost = mailHost;
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled && !mailHost.isBlank() && !senderAddress.isBlank()
                && mailSenderProvider.getIfAvailable() != null;
    }

    public void requestLink(String suppliedEmail, String requestIp) {
        if (!isEnabled()) {
            throw new AuthException(HttpStatus.SERVICE_UNAVAILABLE, "email_sign_in_unavailable",
                    "Email sign-in is not configured yet.");
        }

        String email = suppliedEmail.trim().toLowerCase(java.util.Locale.ROOT);
        rateLimitBusiness.check(email, requestIp);

        byte[] randomToken = new byte[32];
        RANDOM.nextBytes(randomToken);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomToken);
        String tokenHash = hash(rawToken);
        Instant now = Instant.now();
        tokenRepository.deleteByEmailAndConsumedAtIsNull(email);
        tokenRepository.deleteByExpiresAtBeforeOrConsumedAtBefore(now, now.minus(Duration.ofDays(1)));
        tokenRepository.save(new EmailLoginTokenEntity(
                email, tokenHash, now.plus(tokenLifetime), requestIp));

        String link = frontendLink + "?token="
                + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        try {
            var message = mailSenderProvider.getObject().createMimeMessage();
            var helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(senderAddress);
            helper.setTo(email);
            helper.setSubject("Your ChimpChat sign-in link");
            String plainText = """
                    Use this one-time link to sign in to ChimpChat:

                    %s

                    This link expires in %d minutes and can only be used once.
                    If you did not request it, you can ignore this email.
                    """.formatted(link, Math.max(1, tokenLifetime.toMinutes()));
            String html = """
                    <div style="margin:0;background:#f8f8f5;padding:40px 16px;font-family:Arial,sans-serif;color:#171717;">
                      <div style="max-width:560px;margin:0 auto;background:#ffffff;border:1px solid #d4d4d2;border-radius:24px;padding:40px 32px;text-align:center;">
                        <div style="display:inline-block;background:#ffe01b;border:1px solid #171717;border-radius:14px;padding:12px 15px;font-size:22px;font-weight:700;">@</div>
                        <p style="margin:24px 0 8px;color:#77756e;font-size:12px;font-weight:700;letter-spacing:1.6px;">SECURE SIGN-IN</p>
                        <h1 style="margin:0;font-size:30px;line-height:1.15;font-weight:700;">Your sign-in link is ready</h1>
                        <p style="margin:18px 0 28px;color:#55534b;font-size:16px;line-height:1.6;">Use the button below to securely open your ChimpChat workspace.</p>
                        <a href="%s" style="display:inline-block;background:#ffe01b;border:1px solid #171717;border-radius:999px;color:#171717;font-size:15px;font-weight:700;text-decoration:none;padding:15px 25px;">Continue to ChimpChat</a>
                        <p style="margin:28px 0 0;color:#77756e;font-size:13px;line-height:1.6;">This one-time link expires in %d minutes.</p>
                        <span style="position:absolute;width:1px;height:1px;overflow:hidden;clip:rect(0 0 0 0);">%s</span>
                      </div>
                    </div>
                    """.formatted(link, Math.max(1, tokenLifetime.toMinutes()), link);
            helper.setText(plainText, html);
            mailSenderProvider.getObject().send(message);
        } catch (MailException | jakarta.mail.MessagingException | IllegalStateException exception) {
            tokenRepository.deleteByTokenHash(tokenHash);
            throw new AuthException(HttpStatus.SERVICE_UNAVAILABLE, "email_delivery_failed",
                    "We could not send the sign-in email. Please try again later.");
        }
    }

    @Transactional
    public UserEntity consume(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "invalid_link",
                    "This sign-in link is invalid or has expired. Request a new link.");
        }
        String tokenHash = hash(rawToken);
        EmailLoginTokenEntity loginToken = tokenRepository
                .findActiveForUpdate(tokenHash, Instant.now())
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "invalid_link",
                        "This sign-in link is invalid or has expired. Request a new link."));
        loginToken.consume(Instant.now());

        UserEntity user = userRepository.findByEmail(loginToken.getEmail())
                .orElseGet(() -> userRepository.save(new UserEntity(loginToken.getEmail())));
        workspaceBusiness.getOrCreatePersonalWorkspace(user);
        return user;
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the runtime.", exception);
        }
    }
}
