package com.srishna.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityDto {

    /** Who saved this post: email/name and when */
    private List<SavedByItem> savedBy;

    /** Share events: who shared, via which app, when */
    private List<ShareActivityItem> shareActivity;

    /** Total number of times the share link was opened */
    private long openCount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SavedByItem {
        private Long userId;
        private String email;
        private String name;
        private Instant savedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShareActivityItem {
        private Long shareRecordId;
        private Long sharedByUserId;
        private String sharedByEmail;
        private String method;
        private Instant createdAt;
    }
}
