package org.haqdarshak.publishing.service;

import org.haqdarshak.publishing.domain.entity.AuditLog;
import org.haqdarshak.publishing.dto.response.AuditLogResponse;

import java.util.List;

public interface AuditService {

    void recordChange(Long schemeVersionId, String schemeRef, String fieldName,
                      String oldValue, String newValue, String changedBy, String sourceReference);

    List<AuditLogResponse> getAuditLogsForVersion(Long schemeVersionId);

    List<AuditLogResponse> getAuditLogsForScheme(String schemeRef);
}
