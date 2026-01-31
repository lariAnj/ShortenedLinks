package un.links.shortenedlinks.dto;

import lombok.Getter;

import java.time.Instant;

@Getter
public class ShortLinkCreationResponse {
    private final String shortLink;
    private final Instant expiredAt;

    public ShortLinkCreationResponse(ShortLinkCreationResult result) {
        this.shortLink = result.link().getShortLink();
        this.expiredAt = result.link().getCreatedAt();
    }
}
