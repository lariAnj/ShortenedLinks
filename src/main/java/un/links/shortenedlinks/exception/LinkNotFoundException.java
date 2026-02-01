package un.links.shortenedlinks.exception;

import org.springframework.http.HttpStatus;

public class LinkNotFoundException extends BaseCustomerException {
    public LinkNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }
}