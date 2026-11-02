package com.chimpchat.api.auth.business;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class LoginLinkRateLimitBusiness {
    private static final int MAX_PER_EMAIL = 5;
    private static final int MAX_PER_IP = 20;
    private static final Duration WINDOW = Duration.ofHours(1);
    private static final DefaultRedisScript<Long> INCREMENT_WITH_EXPIRY = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]); "
                    + "if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]); end; "
                    + "return count;",
            Long.class);

    private final StringRedisTemplate redis;
    private final Environment environment;
    private final ConcurrentHashMap<String, LocalBucket> localBuckets = new ConcurrentHashMap<>();

    public LoginLinkRateLimitBusiness(StringRedisTemplate redis, Environment environment) {
        this.redis = redis;
        this.environment = environment;
    }

    public void check(String email, String ip) {
        if (increment("email:" + digest(email)) > MAX_PER_EMAIL
                || increment("ip:" + digest(ip == null ? "unknown" : ip)) > MAX_PER_IP) {
            throw new AuthException(HttpStatus.TOO_MANY_REQUESTS, "rate_limited",
                    "Too many sign-in link requests. Please try again later.");
        }
    }

    private long increment(String key) {
        if (environment.matchesProfiles("production")) {
            Long count = redis.execute(INCREMENT_WITH_EXPIRY,
                    java.util.List.of("auth:email-link:" + key),
                    Long.toString(WINDOW.toSeconds()));
            return count == null ? 1 : count;
        }

        long now = System.nanoTime();
        LocalBucket bucket = localBuckets.compute(key, (ignored, current) -> {
            if (current == null || now - current.startedAtNanos() > WINDOW.toNanos()) {
                return new LocalBucket(now, 1);
            }
            return new LocalBucket(current.startedAtNanos(), current.count() + 1);
        });
        if (localBuckets.size() > 10_000) {
            long cutoff = now - WINDOW.toNanos();
            localBuckets.entrySet().removeIf(entry -> cutoff > entry.getValue().startedAtNanos());
        }
        return bucket.count();
    }

    private String digest(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the runtime.", exception);
        }
    }

    private record LocalBucket(long startedAtNanos, int count) {}
}
