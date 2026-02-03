package com.srishna.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Tracks share chain: 1st → 2nd → 3rd user.
 * Each share has a unique token in the link (?ref=TOKEN). When someone opens that link,
 * we record a visit. If they share again, we create a new ShareRecord with parentShareId = this id.
 */
@Entity
@Table(name = "share_records", indexes = {
    @Index(name = "idx_share_token", columnList = "shareToken", unique = true),
    @Index(name = "idx_share_post", columnList = "postId"),
    @Index(name = "idx_share_parent", columnList = "parentShareId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique token used in share link (?ref=...) */
    @Column(nullable = false, unique = true)
    private String shareToken;

    @Column(nullable = false)
    private Long postId;

    /** User who created this share link (if logged in) */
    @Column(name = "user_id")
    private Long userId;

    /** Who shared: null = original share, otherwise ID of the ShareRecord whose link was used to share */
    private Long parentShareId;

    /** When this share link was created */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void createdAt() {
        if (this.createdAt == null) this.createdAt = Instant.now();
    }
}
