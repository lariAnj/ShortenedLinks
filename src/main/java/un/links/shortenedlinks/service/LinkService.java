package un.links.shortenedlinks.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import un.links.shortenedlinks.dto.ShortLinkCreationResult;
import un.links.shortenedlinks.model.Link;
import un.links.shortenedlinks.model.ShortLinkSource;
import un.links.shortenedlinks.repository.LinkRepo;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.NoSuchElementException;

@Service
@Slf4j
@RequiredArgsConstructor
public class LinkService {
    @Value("${link.ttl-minutes}")
    private int LINK_TTL;

    private static final String BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int SHORT_LINK_LENGTH = 7;
    private static final int MAX_ATTEMPTS = 5;

    private final SecureRandom secureRandom = new SecureRandom();
    private final LinkRepo linkRepo;

    @Transactional
    public ShortLinkCreationResult createShortLink(String fullLink) {
        return linkRepo.findLinkByFullLink(fullLink)
//                .filter(link -> !isExpired(link))
                .map(link -> {
                    if (!isExpired(link)) {
                        return new ShortLinkCreationResult(link, ShortLinkSource.EXISTED);
                    }
                    Link regenerated = regenerateExpiredLink(link);
                    return new ShortLinkCreationResult(regenerated, ShortLinkSource.CREATED);
                })
                .orElseGet(() -> {
                    Link created = generateNewShortLinkWithRetry(fullLink);
                    return new ShortLinkCreationResult(created, ShortLinkSource.CREATED);
                });
    }

    public Link getFullLink(String shortLink) throws IllegalStateException {
        Link link = linkRepo.findLinkByShortLink(shortLink)
                .orElseThrow(() -> new NoSuchElementException("Link not found."));
        if (isExpired(link)) {
            throw new IllegalStateException("The link has expired.");
        }
        return link;
    }

    private Link regenerateExpiredLink(Link link) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                link.setShortLink(generateRandomBase62());
                link.setCreatedAt(Instant.now());
                link.setExpiredAt(Instant.now().plus(Duration.ofMinutes(LINK_TTL)));

                return linkRepo.save(link);

            } catch (DataIntegrityViolationException ex) {
                log.warn("Collision detected while regenerating shortLink, attempt={}", attempt);
            }
        }

        throw new IllegalStateException("Failed to regenerate short link");
    }

    private Link generateNewShortLinkWithRetry(String fullLink) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return linkRepo.save(buildNewLink(fullLink));
            } catch (DataIntegrityViolationException ex) {
                log.warn("Collision detected while generating shortLink, attempt={}", attempt);

                // Check if another thread insert data for this link (DataIntegrity...Exception occurs)
                if (linkRepo.existsLinkByFullLink(fullLink)) {
                    return linkRepo.findLinkByFullLink(fullLink)
                            .orElseThrow(() -> ex);
                }
            }
        }
        // If after all attempts the link wasn't received
        throw new IllegalStateException("Failed to generate unique shortLink.");
    }

    private Link buildNewLink(String fullLink) {
        Link newLink = new Link();
        newLink.setFullLink(fullLink);
        newLink.setShortLink(generateRandomBase62());
        newLink.setCreatedAt(Instant.now());
        newLink.setExpiredAt(Instant.now().plus(Duration.ofMinutes(LINK_TTL)));
        return newLink;
    }

    private String generateRandomBase62() {
        StringBuilder encodedStr = new StringBuilder(SHORT_LINK_LENGTH);
        for (int i = 0; i < SHORT_LINK_LENGTH; i++) {
            encodedStr.append(BASE62.charAt(secureRandom.nextInt(BASE62.length())));
        }
        return encodedStr.toString();
    }

    private boolean isExpired(Link link) {
        return Instant.now().isAfter(link.getExpiredAt());
    }

}
