package com.example.demo.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "reviews", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "event_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "comment_text", columnDefinition = "TEXT")
    private String commentText;

    @Column(name = "is_hidden", nullable = false)
    private boolean isHidden = false;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "event_count")
    private Integer eventCount;

    @Column(name = "performance_rating")
    private Integer performanceRating;

    @Column(name = "sound_lighting_rating")
    private Integer soundLightingRating;

    @Column(name = "venue_rating")
    private Integer venueRating;

    @Column(name = "overall_rating")
    private Integer overallRating;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Comment> comments = new HashSet<>();
}
