package com.srishna.repository;

import com.srishna.entity.SavedItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavedItemRepository extends JpaRepository<SavedItem, Long> {

    List<SavedItem> findByUserIdOrderBySavedAtDesc(Long userId);

    List<SavedItem> findByPostIdOrderBySavedAtDesc(Long postId);

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    void deleteByPostId(Long postId);
}
