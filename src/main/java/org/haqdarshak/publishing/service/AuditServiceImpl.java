package org.haqdarshak.publishing.service;

import org.haqdarshak.publishing.domain.entity.AuditLog;
import org.haqdarshak.publishing.dto.response.AuditLogResponse;
import org.haqdarshak.publishing.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void recordChange(Long schemeVersionId, String schemeRef, String fieldName,
                             String oldValue, String newValue, String changedBy, String sourceReference) {
        AuditLog auditLog = new AuditLog(
                schemeVersionId,
                schemeRef,
                fieldName,
                oldValue,
                newValue,
                changedBy,
                Instant.now(),
                sourceReference
        );
        auditLogRepository.save(auditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogsForVersion(Long schemeVersionId) {
        return auditLogRepository.findBySchemeVersionIdOrderByChangedAtDesc(schemeVersionId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogsForScheme(String schemeRef) {
        return auditLogRepository.findBySchemeRefOrderByChangedAtDesc(schemeRef)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getSchemeVersionId(),
                log.getSchemeRef(),
                log.getFieldName(),
                log.getOldValue(),
                log.getNewValue(),
                log.getChangedBy(),
                log.getChangedAt(),
                log.getSourceReference()
        );
    }
}
