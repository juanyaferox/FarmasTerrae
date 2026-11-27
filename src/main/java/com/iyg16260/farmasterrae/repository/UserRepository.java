package com.iyg16260.farmasterrae.repository;

import com.iyg16260.farmasterrae.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findAllByProfileType(String type);

    /**
     * Bloquea ({@code SELECT ... FOR UPDATE}) las cuentas activas con ese perfil. Las operaciones que pueden dejar
     * el sistema sin administradores lo llaman antes que nada: así se ejecutan de una en una y la comprobación
     * "al menos un administrador" ve siempre los datos confirmados. Requiere una transacción activa.
     */
    @Lock (LockModeType.PESSIMISTIC_WRITE)
    @Query ("select u from User u where u.profile.type = :type order by u.id")
    List<User> findAllByProfileTypeForUpdate(@Param ("type") String type);

    /** Lee y bloquea una cuenta activa para modificarla. Requiere una transacción activa. */
    @Lock (LockModeType.PESSIMISTIC_WRITE)
    @Query ("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param ("id") Long id);

    /** Lee y bloquea una cuenta activa para modificarla. Requiere una transacción activa. */
    @Lock (LockModeType.PESSIMISTIC_WRITE)
    @Query ("select u from User u where u.username = :username")
    Optional<User> findByUsernameForUpdate(@Param ("username") String username);
}
