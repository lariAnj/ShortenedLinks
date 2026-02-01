package un.links.shortenedlinks.exception;

import org.springframework.http.HttpStatus;

public class ExpiredLinkException extends BaseCustomerException {
    public ExpiredLinkException(String message) {
        super("LINK_IS_EXPIRED", message, HttpStatus.GONE);
    }
}
