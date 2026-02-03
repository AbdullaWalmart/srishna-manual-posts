package com.srishna.service;

import com.srishna.dto.ActivityDto;
import com.srishna.entity.*;
import com.srishna.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostActivityService {

    private final SavedItemRepository savedItemRepository;
    private final ShareRecordRepository shareRecordRepository;
    private final ShareMethodLogRepository shareMethodLogRepository;
    private final ShareVisitRepository shareVisitRepository;
    private final UserRepository userRepository;

    public ActivityDto getActivity(Long postId) {
        List<ActivityDto.SavedByItem> savedBy = savedItemRepository.findByPostIdOrderBySavedAtDesc(postId).stream()
                .map(s -> {
                    var user = userRepository.findById(s.getUserId());
                    return user.map(u -> new ActivityDto.SavedByItem(
                            u.getId(), u.getEmail(), u.getName(), s.getSavedAt())).orElse(null);
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        List<ShareRecord> records = shareRecordRepository.findByPostIdOrderByCreatedAtDesc(postId);
        List<ActivityDto.ShareActivityItem> shareActivity = new ArrayList<>();
        long openCount = 0;
        for (ShareRecord rec : records) {
            openCount += shareVisitRepository.countByShareRecordId(rec.getId());
            List<ShareMethodLog> methods = shareMethodLogRepository.findByShareRecordIdOrderByCreatedAtAsc(rec.getId());
            String sharedByEmail = rec.getUserId() != null
                    ? userRepository.findById(rec.getUserId()).map(User::getEmail).orElse(null)
                    : null;
            for (ShareMethodLog m : methods) {
                shareActivity.add(new ActivityDto.ShareActivityItem(
                        rec.getId(), rec.getUserId(), sharedByEmail, m.getMethod(), m.getCreatedAt()));
            }
            if (methods.isEmpty() && rec.getUserId() != null) {
                shareActivity.add(new ActivityDto.ShareActivityItem(
                        rec.getId(), rec.getUserId(), sharedByEmail, "link", rec.getCreatedAt()));
            }
        }

        return ActivityDto.builder()
                .savedBy(savedBy)
                .shareActivity(shareActivity)
                .openCount(openCount)
                .build();
    }
}
