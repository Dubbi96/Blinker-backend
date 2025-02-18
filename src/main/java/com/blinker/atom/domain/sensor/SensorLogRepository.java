package com.blinker.atom.domain.sensor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SensorLogRepository extends JpaRepository<SensorLog, Long> {
    Optional<SensorLog> findByEventCode(String eventCode);
}
