package com.srishna.service;

import com.srishna.entity.Post;
import com.srishna.entity.SavedItem;
import com.srishna.repository.PostRepository;
import com.srishna.repository.SavedItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaveService {

    private final SavedItemRepository savedItemRepository;
    private final PostRepository postRepository;
    private final PostService postService;

    @Transactional
    public void save(Long postId, Long userId) {
        if (userId == null) return;
        if (savedItemRepository.existsByPostIdAndUserId(postId, userId)) return;
        savedItemRepository.save(SavedItem.builder()
                .postId(postId)
                .userId(userId)
                .build());
    }

    public List<Post> getMySaves(Long userId) {
        if (userId == null) return List.of();
        List<Long> seen = new java.util.ArrayList<>();
        return savedItemRepository.findByUserIdOrderBySavedAtDesc(userId).stream()
                .map(SavedItem::getPostId)
                .filter(postId -> {
                    if (seen.contains(postId)) return false;
                    seen.add(postId);
                    return true;
                })
                .map(postRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .collect(Collectors.toList());
    }
}
