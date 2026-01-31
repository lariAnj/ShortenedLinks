package un.links.shortenedlinks.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ShortLinkCreationRequest {
    @NotBlank(message = "Link for shortening cannot be empty")
    private String fullLink;
}
