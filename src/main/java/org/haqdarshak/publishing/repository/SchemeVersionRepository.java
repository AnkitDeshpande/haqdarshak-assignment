package org.haqdarshak.publishing.repository;

import org.haqdarshak.publishing.domain.entity.SchemeVersion;
import org.haqdarshak.publishing.domain.enums.VersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchemeVersionRepository extends JpaRepository<SchemeVersion, Long> {

    List<SchemeVersion> findBySchemeId(Long schemeId);

    Optional<SchemeVersion> findBySchemeIdAndVersionNumber(Long schemeId, Integer versionNumber);

    List<SchemeVersion> findBySchemeIdAndStatus(Long schemeId, VersionStatus status);

    @Query("SELECT MAX(v.versionNumber) FROM SchemeVersion v WHERE v.scheme.id = :schemeId")
    Integer findMaxVersionNumberBySchemeId(@Param("schemeId") Long schemeId);

    @Query("SELECT v FROM SchemeVersion v JOIN v.sharedRules r WHERE r.id = :ruleId")
    List<SchemeVersion> findBySharedRuleId(@Param("ruleId") Long ruleId);
}
