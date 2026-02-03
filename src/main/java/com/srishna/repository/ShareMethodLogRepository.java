package com.srishna.repository;

import com.srishna.entity.ShareMethodLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShareMethodLogRepository extends JpaRepository<ShareMethodLog, Long> {

    List<ShareMethodLog> findByShareRecordIdOrderByCreatedAtAsc(Long shareRecordId);
}
