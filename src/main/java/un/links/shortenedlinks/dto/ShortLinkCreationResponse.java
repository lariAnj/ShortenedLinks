package un.links.shortenedlinks.dto;

import lombok.Getter;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Instant;

@Getter
public class ShortLinkCreationResponse {
    private final String shortLink;
    private final Instant expiredAt;
    private final String shortUrl;

    public ShortLinkCreationResponse(ShortLinkCreationResult result) {
        this.shortLink = result.link().getShortLink();
        this.expiredAt = result.link().getCreatedAt();
        this.shortUrl = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/short-links/redirect/")
                .path(this.shortLink)
                .toUriString();
    }
}
