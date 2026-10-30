package org.haqdarshak.publishing.repository;

import org.haqdarshak.publishing.domain.entity.SchemeField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchemeFieldRepository extends JpaRepository<SchemeField, Long> {

    List<SchemeField> findBySchemeVersionId(Long schemeVersionId);

    Optional<SchemeField> findBySchemeVersionIdAndFieldName(Long schemeVersionId, String fieldName);
}
