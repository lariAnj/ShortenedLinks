package un.links.shortenedlinks.exception;

import org.springframework.http.HttpStatus;

public class GlobalRateLimitException extends BaseCustomerException {
    public GlobalRateLimitException(String message) { super("SERVICE_IS_BUSY", message, HttpStatus.SERVICE_UNAVAILABLE); }

}
