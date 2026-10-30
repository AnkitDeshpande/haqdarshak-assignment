package org.haqdarshak.publishing.repository;

import org.haqdarshak.publishing.domain.entity.SharedEligibilityRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SharedEligibilityRuleRepository extends JpaRepository<SharedEligibilityRule, Long> {

    Optional<SharedEligibilityRule> findByRuleCode(String ruleCode);

    boolean existsByRuleCode(String ruleCode);
}
