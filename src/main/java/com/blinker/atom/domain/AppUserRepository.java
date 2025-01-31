package com.blinker.atom.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);

    Optional<Object> findByAppUserId(Long appUserId);

    Optional<Object> findByUserId(String userId);
}
