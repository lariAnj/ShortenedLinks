package un.links.shortenedlinks.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import un.links.shortenedlinks.model.Link;

import java.util.Optional;

public interface LinkRepo extends JpaRepository<Link,Long> {
    Optional<Link> findLinkByFullLink(String fullLink);
    Optional<Link> findLinkByShortLink(String shortLink);

    boolean existsLinkByFullLink(String fullLink);
}
