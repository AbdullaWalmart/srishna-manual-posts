package com.srishna.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "saved_items", indexes = {
    @Index(name = "idx_saved_post", columnList = "postId"),
    @Index(name = "idx_saved_user", columnList = "userId"),
    @Index(name = "idx_saved_at", columnList = "savedAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "saved_at", nullable = false, updatable = false)
    private Instant savedAt;

    @PrePersist
    void savedAt() {
        if (this.savedAt == null) this.savedAt = Instant.now();
    }
}
