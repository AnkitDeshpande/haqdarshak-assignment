package org.haqdarshak.publishing.repository;

import org.haqdarshak.publishing.domain.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findBySchemeVersionIdOrderByChangedAtDesc(Long schemeVersionId);

    List<AuditLog> findBySchemeRefOrderByChangedAtDesc(String schemeRef);
}
