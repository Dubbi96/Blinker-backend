package com.blinker.atom.domain.sensor;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SensorRepository extends JpaRepository<Sensor,Long> {
    @Query(value = "SELECT s FROM Sensor s WHERE s.sensorGroup.id = :sensorGroupId AND s.groupPositionNumber = 0")
    Optional<Sensor> findMasterSensorBySensorGroup(String sensorGroupId);

    Optional<Sensor> findSensorByDeviceId(Long deviceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Sensor> findSensorByDeviceNumber(String deviceNumber);

    Optional<Sensor> findSensorById(Long id);
}
