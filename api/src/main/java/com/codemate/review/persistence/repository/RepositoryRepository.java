package com.codemate.review.persistence.repository;

import com.codemate.review.persistence.entity.RepositoryEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositoryRepository extends JpaRepository<RepositoryEntity, Long> {

    Optional<RepositoryEntity> findByFullName(String fullName);
}
