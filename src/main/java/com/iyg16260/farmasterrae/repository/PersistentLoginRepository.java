package com.iyg16260.farmasterrae.repository;

import com.iyg16260.farmasterrae.model.PersistentLogin;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PersistentLoginRepository extends JpaRepository<PersistentLogin, String> {
    Optional<PersistentLogin> findByUsername(String username);
    void deleteByUsername(String username);
}
