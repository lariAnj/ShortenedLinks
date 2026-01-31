package un.links.shortenedlinks.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "link",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "full_link"),
                @UniqueConstraint(columnNames = "short_link")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Link {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_link", nullable = false, length = 2048)
    private String fullLink;

    @Column(name = "short_name", nullable = false)
    private String shortLink;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiredAt;


}
