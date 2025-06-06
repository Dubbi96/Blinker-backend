package com.blinker.atom.domain.sensor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SensorLogRepository extends JpaRepository<SensorLog, Long> {
    @Query("SELECT MAX(sl.createdAt) FROM SensorLog sl")
    LocalDateTime findMaxCreatedAt();

    @Modifying
    @Transactional
    @Query("DELETE FROM SensorLog sl WHERE sl.sensorGroup = :sensorGroup")
    void deleteBySensorGroup(@Param("sensorGroup") SensorGroup sensorGroup);

    @Query("SELECT s FROM SensorLog s WHERE s.isProcessed = false AND s.createdAt >= :since")
    List<SensorLog> findUnprocessedLogs(@Param("since") LocalDateTime since);

    @Query("SELECT sl FROM SensorLog sl WHERE sl.sensorDeviceNumber = :deviceNumber " +
           "AND sl.createdAt >= :startDate AND sl.createdAt < :endDate")
    List<SensorLog> getSensorLogsBySensorDeviceNumberAndDateRange(
            @Param("deviceNumber") String deviceNumber,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT sl.eventCode FROM SensorLog sl")
    List<String> findAllEventCodes();

    @Query("SELECT s FROM SensorLog s WHERE s.createdAt < :cutoff")
    List<SensorLog> findLogsOlderThan(@Param("cutoff") LocalDateTime cutoff);

    /**
     * 특정 SensorGroup의 가장 마지막 저장 시간 반환 (null 방지용 Optional)
     */
    @Query("SELECT MAX(sl.createdAt) FROM SensorLog sl WHERE sl.sensorGroup.id = :sensorGroupId")
    Optional<LocalDateTime> findMaxCreatedAtBySensorGroupId(@Param("sensorGroupId") String sensorGroupId);

    /**
     * 특정 SensorGroup에 저장된 모든 eventCode 조회
     */
    @Query("SELECT sl.eventCode FROM SensorLog sl WHERE sl.sensorGroup.id = :sensorGroupId")
    List<String> findAllEventCodesBySensorGroupId(@Param("sensorGroupId") String sensorGroupId);

    /**
     * 특정 SensorGroup 내 처리되지 않은 로그를 최근 24시간 기준으로 조회
     */
    @Query("""
        SELECT sl FROM SensorLog sl
        WHERE sl.sensorGroup.id = :sensorGroupId
          AND sl.isProcessed = false
          AND sl.createdAt >= :cutoffTime
    """)
    List<SensorLog> findUnprocessedLogsBySensorGroupId(
            @Param("sensorGroupId") String sensorGroupId,
            @Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Find all unprocessed logs for a sensor group, excluding those with device numbers in the given list.
     */
    @Query("""
        SELECT sl FROM SensorLog sl
        WHERE sl.sensorGroup = :sensorGroup
          AND sl.isProcessed = false
          AND sl.sensorDeviceNumber NOT IN :deviceNumbers
    """)
    List<SensorLog> findAllBySensorGroupAndIsProcessedFalseAndDeviceNumberNotIn(
            @Param("sensorGroup") SensorGroup sensorGroup,
            @Param("deviceNumbers") List<String> deviceNumbers);

    /**
     * Find all unprocessed logs for a sensor group.
     */
    List<SensorLog> findAllBySensorGroupAndIsProcessedFalse(SensorGroup sensorGroup);
}
