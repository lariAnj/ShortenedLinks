package un.links.shortenedlinks.exception;

import org.springframework.http.HttpStatus;

public class RateLimitException extends BaseCustomerException {
    public RateLimitException(String message) { super("TOO_MANY_REQUESTS", message, HttpStatus.TOO_MANY_REQUESTS); }
}