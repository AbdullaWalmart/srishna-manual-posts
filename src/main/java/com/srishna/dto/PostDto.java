package com.srishna.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDto {
    private Long id;
    /** Full URL to access the image (signed or public). */
    private String imageUrl;
    /** Complete storage path in bucket (e.g. images/uuid.jpg). */
    private String imagePath;
    private String textContent;
    /** Uploaded / created datetime (ISO-8601). */
    private Instant createdAt;
}
