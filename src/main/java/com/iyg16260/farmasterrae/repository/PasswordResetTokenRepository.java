package com.iyg16260.farmasterrae.repository;

import com.iyg16260.farmasterrae.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from PasswordResetToken t where t.tokenHash = :tokenHash")
    Optional<PasswordResetToken> findByTokenHash(@Param("tokenHash") String tokenHash);
    Optional<PasswordResetToken> findFirstByTokenHash(String tokenHash);
    @Transactional
    void deleteByUser_IdAndUsedAtIsNull(Long userId);
    @Transactional
    void deleteByUser_IdAndUsedAtIsNullAndIdNot(Long userId, Long id);
}
