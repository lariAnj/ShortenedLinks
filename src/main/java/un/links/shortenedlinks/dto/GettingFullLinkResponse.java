package un.links.shortenedlinks.dto;

import lombok.Getter;
import un.links.shortenedlinks.model.Link;

@Getter
public class GettingFullLinkResponse {
    private final String fullLink;

    public GettingFullLinkResponse(Link link) {
        this.fullLink = link.getFullLink();
    }
}
