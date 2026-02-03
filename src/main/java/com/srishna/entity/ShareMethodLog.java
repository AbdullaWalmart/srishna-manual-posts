package com.srishna.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "share_method_logs", indexes = {
    @Index(name = "idx_share_method_record", columnList = "shareRecordId"),
    @Index(name = "idx_share_method_at", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareMethodLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "share_record_id", nullable = false)
    private Long shareRecordId;

    @Column(nullable = false)
    private String method;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void createdAt() {
        if (this.createdAt == null) this.createdAt = Instant.now();
    }
}
