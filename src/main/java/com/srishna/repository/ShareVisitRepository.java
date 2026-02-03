package com.srishna.repository;

import com.srishna.entity.ShareVisit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShareVisitRepository extends JpaRepository<ShareVisit, Long> {

    long countByShareRecordId(Long shareRecordId);
}
