package un.links.shortenedlinks.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GlobalRateLimiter {
    private final StringRedisTemplate redisTemplate;

    @Value("${link.rate-limit.max-requests-global}")
    private int LIMIT;

    // common key for requests
    String key = "global_link_shortener_key";

    String withoutRedis = "WITHOUT_REDIS"; // if an error occurred while redis working

    // Timeout for key that contains current count of requests
    private final Duration TIMEOUT_FOR_ONE_REQUEST = Duration.ofSeconds(30);

    public String tryAcquire() {
        try {
            long now = Instant.now().toEpochMilli();
            long expiredAt = now + TIMEOUT_FOR_ONE_REQUEST.toMillis(); // score

            // remove old that are expired (accidentally left in redis)
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, now - 1); // remove those who has score (expiredAt) < now. 1ms reserve to avoid removing boarder value

            // count active requests
            Long active = redisTemplate.opsForZSet().zCard(key);
            if (active != null && active >= LIMIT) {
                return null;
            }

            // unique identifier for each request to remove them
            String permitValue = UUID.randomUUID().toString();
            redisTemplate.opsForZSet().add(key, permitValue, expiredAt);

            return permitValue;
        } catch (Exception ex) {
            log.error("Redis is not available, service works without global rate limiting", ex);
            return withoutRedis;
        }
    }

    public void free(String permitValue) {
        if (permitValue == null || withoutRedis.equals(permitValue)) {
            return;
        }
        try {
            redisTemplate.opsForZSet().remove(key, permitValue);
        } catch (Exception ex) {
            log.error("Redis is  not available, cannot remove permit", ex);
        }
    }
}
