package com.srishna.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** GCS path or public URL for the image (e.g. images/uuid.jpg) */
    @Column(nullable = false)
    private String imagePath;

    /** GCS path for text content or inline text (e.g. texts/uuid.txt or null if text stored in DB) */
    private String textPath;

    /** Text content - can be stored here or in GCS; searchable */
    @Column(columnDefinition = "TEXT")
    @Size(max = 100_000)
    private String textContent;

    /** User who created the post */
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void createdAt() {
        if (this.createdAt == null) this.createdAt = Instant.now();
    }
}
