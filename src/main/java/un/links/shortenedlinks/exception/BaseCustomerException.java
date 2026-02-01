package un.links.shortenedlinks.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class BaseCustomerException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus status;

    public BaseCustomerException(String errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }
}