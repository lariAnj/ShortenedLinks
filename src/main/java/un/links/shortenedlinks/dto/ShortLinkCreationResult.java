package un.links.shortenedlinks.dto;

import un.links.shortenedlinks.model.Link;
import un.links.shortenedlinks.model.ShortLinkSource;

// To keep the history of shortLink creation and set the correct HttpStatus
public record ShortLinkCreationResult(
    Link link,
    ShortLinkSource source
) {}
