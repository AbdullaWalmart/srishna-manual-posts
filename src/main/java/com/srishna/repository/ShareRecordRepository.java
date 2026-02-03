package com.srishna.repository;

import com.srishna.entity.ShareRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShareRecordRepository extends JpaRepository<ShareRecord, Long> {

    Optional<ShareRecord> findByShareToken(String shareToken);

    List<ShareRecord> findByPostIdOrderByCreatedAtDesc(Long postId);
}
