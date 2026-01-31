package un.links.shortenedlinks.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import un.links.shortenedlinks.dto.GettingFullLinkResponse;
import un.links.shortenedlinks.dto.ShortLinkCreationRequest;
import un.links.shortenedlinks.dto.ShortLinkCreationResponse;
import un.links.shortenedlinks.dto.ShortLinkCreationResult;
import un.links.shortenedlinks.model.Link;
import un.links.shortenedlinks.model.ShortLinkSource;
import un.links.shortenedlinks.service.LinkService;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/short-links")
public class LinkController {
    private final LinkService linkService;

    public LinkController(LinkService linkService) {
        this.linkService = linkService;
    }

    @PostMapping("/shorten")
    public ResponseEntity<ShortLinkCreationResponse> create(@RequestBody @Valid ShortLinkCreationRequest request) {
        ShortLinkCreationResult result = linkService.createShortLink(request.getFullLink());
        HttpStatus status = result.source() == ShortLinkSource.CREATED ?
                HttpStatus.CREATED :
                HttpStatus.OK;
        return new ResponseEntity<>(new ShortLinkCreationResponse(result), status);
    }

    @GetMapping("/{short-link}")
    public ResponseEntity<GettingFullLinkResponse> getFullLink(@PathVariable("short-link") @NotBlank String shortLink) {
        Link link = linkService.getFullLink(shortLink);
        return new ResponseEntity<>(new GettingFullLinkResponse(link), HttpStatus.OK);
    }

    @GetMapping("/redirect/{short-link}")
    public ResponseEntity<Void> redirect(@PathVariable("short-link") @NotBlank String shortLink) {
        String fullLink = linkService.getFullLink(shortLink).getFullLink();
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(fullLink));
        return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
    }
}
