package com.blinker.atom.domain.appuser;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);

    Optional<Object> findByUserId(String userId);

    @Query(value = "SELECT * FROM app_user WHERE :role = ANY(roles)", nativeQuery = true)
    List<AppUser> findByRolesContaining(@Param("role") String role);

    void deleteAppUserById(Long id);

    Optional<AppUser> findAppUserById(Long id);
}
