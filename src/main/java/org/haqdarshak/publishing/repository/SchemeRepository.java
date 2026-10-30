package org.haqdarshak.publishing.repository;

import org.haqdarshak.publishing.domain.entity.Scheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SchemeRepository extends JpaRepository<Scheme, Long> {
    Optional<Scheme> findBySchemeRef(String schemeRef);
    boolean existsBySchemeRef(String schemeRef);
}
