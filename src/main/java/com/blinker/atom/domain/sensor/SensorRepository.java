package com.blinker.atom.domain.sensor;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SensorRepository extends JpaRepository<Sensor,Long> {
    Optional<Sensor> findBySensorGroup(SensorGroup sensorGroup);
    Optional<Sensor> findSensorByDeviceId(Long deviceId);
    @Modifying
    @Query(value = "INSERT INTO sensor_group (sensor_group_id, created_at, fault_count, sensor_group_key, sensor_count, ssid, ssid_updated_at, updated_at) " +
                   "VALUES (:sensorGroupId, :createdAt, :faultCount, :sensorGroupKey, :sensorCount, :ssid, :ssidUpdatedAt, :updatedAt) " +
                   "ON CONFLICT (sensor_group_id) DO NOTHING", nativeQuery = true)
    void insertSensorGroupIfNotExists(
        @Param("sensorGroupId") String sensorGroupId,
        @Param("createdAt") LocalDateTime createdAt,
        @Param("faultCount") int faultCount,
        @Param("sensorGroupKey") String sensorGroupKey,
        @Param("sensorCount") int sensorCount,
        @Param("ssid") String ssid,
        @Param("ssidUpdatedAt") LocalDateTime ssidUpdatedAt,
        @Param("updatedAt") LocalDateTime updatedAt);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Sensor> findSensorByDeviceNumber(String deviceNumber);
    Optional<Sensor> findSensorById(Long id);
}
