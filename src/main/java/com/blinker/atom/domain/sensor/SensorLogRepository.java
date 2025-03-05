package com.blinker.atom.domain.sensor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SensorLogRepository extends JpaRepository<SensorLog, Long> {
    Optional<SensorLog> findByEventCode(String eventCode);

    @Query("SELECT s FROM SensorLog s WHERE s.isProcessed = false AND s.createdAt >= :since")
    List<SensorLog> findUnprocessedLogs(@Param("since") LocalDateTime since);

    List<SensorLog> getSensorLogsBySensorDeviceNumber(String sensorDeviceNumber);

    @Query("SELECT sl FROM SensorLog sl WHERE sl.sensorDeviceNumber = :deviceNumber " +
           "AND sl.createdAt >= :startDate AND sl.createdAt < :endDate")
    List<SensorLog> getSensorLogsBySensorDeviceNumberAndDateRange(
            @Param("deviceNumber") String deviceNumber,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT sl.eventCode FROM SensorLog sl")
    List<String> findAllEventCodes();
}
