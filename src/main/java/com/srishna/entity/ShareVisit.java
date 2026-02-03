package com.srishna.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** Records when someone opens a share link (?ref=TOKEN). Enables tracing chain: who opened which share. */
@Entity
@Table(name = "share_visits", indexes = {
    @Index(name = "idx_visit_share", columnList = "shareRecordId"),
    @Index(name = "idx_visit_at", columnList = "visitedAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long shareRecordId;

    @Column(nullable = false, updatable = false)
    private Instant visitedAt;

    /** Optional: user/session id if we have it */
    private String visitorId;

    @PrePersist
    void visitedAt() {
        if (this.visitedAt == null) this.visitedAt = Instant.now();
    }
}
