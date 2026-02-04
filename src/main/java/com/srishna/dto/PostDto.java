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
    /** If false, post is hidden from list APIs. */
    private Boolean active;
    /** Name of the user who uploaded the post (or email if no name). */
    private String uploaderName;
    /** Uploaded / created datetime (ISO-8601). */
    private Instant createdAt;
}
