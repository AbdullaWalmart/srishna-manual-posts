package com.srishna.service;

import com.srishna.entity.ShareMethodLog;
import com.srishna.entity.ShareRecord;
import com.srishna.entity.ShareVisit;
import com.srishna.repository.ShareMethodLogRepository;
import com.srishna.repository.ShareRecordRepository;
import com.srishna.repository.ShareVisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShareTrackingService {

    private final ShareRecordRepository shareRecordRepository;
    private final ShareVisitRepository shareVisitRepository;
    private final ShareMethodLogRepository shareMethodLogRepository;
    private final DbSyncHelper dbSyncHelper;

    /** Create a new share link for a post. parentShareId = null for first share; userId = who shared (if logged in). */
    @Transactional
    public ShareRecord createShare(Long postId, Long parentShareId, Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        ShareRecord record = ShareRecord.builder()
                .shareToken(token)
                .postId(postId)
                .userId(userId)
                .parentShareId(parentShareId)
                .build();
        record = shareRecordRepository.save(record);
        dbSyncHelper.syncToGcsAfterCommit();
        return record;
    }

    /** Record that a share was used via a specific app (e.g. whatsapp, twitter). */
    @Transactional
    public void recordShareMethod(String shareToken, String method) {
        shareRecordRepository.findByShareToken(shareToken).ifPresent(record -> {
            shareMethodLogRepository.save(ShareMethodLog.builder()
                    .shareRecordId(record.getId())
                    .method(method != null ? method : "unknown")
                    .build());
            dbSyncHelper.syncToGcsAfterCommit();
        });
    }

    /** Record that someone opened a share link (?ref=TOKEN). Returns postId to show. */
    @Transactional
    public Optional<Long> recordVisit(String shareToken, String visitorId) {
        Optional<ShareRecord> record = shareRecordRepository.findByShareToken(shareToken);
        if (record.isEmpty()) return Optional.empty();
        ShareVisit visit = ShareVisit.builder()
                .shareRecordId(record.get().getId())
                .visitorId(visitorId)
                .build();
        shareVisitRepository.save(visit);
        dbSyncHelper.syncToGcsAfterCommit();
        return Optional.of(record.get().getPostId());
    }

    public Optional<ShareRecord> getByToken(String shareToken) {
        return shareRecordRepository.findByShareToken(shareToken);
    }
}
