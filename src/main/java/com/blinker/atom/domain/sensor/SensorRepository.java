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
    @Query(value = "SELECT s.longitude, s.latitude FROM Sensor s WHERE s.sensorGroup.id = :sensorGroupId")
    Optional<Sensor> findMasterSensorBySensorGroup(String sensorGroupId);

    Optional<Sensor> findSensorByDeviceId(Long deviceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Sensor> findSensorByDeviceNumber(String deviceNumber);

    Optional<Sensor> findSensorById(Long id);
}
