package un.links.shortenedlinks.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimiter {

    private final StringRedisTemplate redisTemplate;

    @Value("${link.rate-limit.max-requests}")
    private int LIMIT;

    @Value("${link.rate-limit.window}")
    private Duration WINDOW;

    public boolean isAllowed(String ip) {
        String key = "client_key" + ip;

        try {
            Long count = redisTemplate.opsForValue().increment(key);

            // If that's the first request in time unit, we need to remember the end of interval live time
            if (count == 1) {
                // We want to delete key after the interval live time has ended
                redisTemplate.expire(key, ttlWithReserve(WINDOW));
            }
            return count <= LIMIT;
        } catch (Exception ex) {
            log.error("Redis is not available, service works without rate limiting", ex);
            return true;
        }
    }

    // To avoid boundary errors
    private Duration ttlWithReserve(Duration window) {
        long windowMs = window.toMillis();
        long reserveMs = Math.min(windowMs / 100, 5000); // Our reserve is 1% or max 5s
        return window.plusMillis(reserveMs);
    }
}


