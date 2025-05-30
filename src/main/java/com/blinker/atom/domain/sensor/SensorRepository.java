package com.blinker.atom.domain.sensor;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface SensorRepository extends JpaRepository<Sensor,Long> {
    @Query(value = "SELECT s FROM Sensor s WHERE s.sensorGroup.id = :sensorGroupId AND s.groupPositionNumber = 0")
    Optional<Sensor> findMasterSensorBySensorGroup(String sensorGroupId);

    @Modifying
    @Transactional
    void deleteBySensorGroup(SensorGroup sensorGroup);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Sensor> findSensorByDeviceNumber(String deviceNumber);

    Optional<Sensor> findSensorById(Long id);

    Optional<Sensor> getSensorByDeviceNumberAndGroupPositionNumber(String deviceNumber, Long groupPositionNumber);

    @Query("SELECT s.deviceNumber FROM Sensor s WHERE s.sensorGroup.id = :sensorGroupId")
    List<String> findDeviceNumbersBySensorGroupId(@Param("sensorGroupId") String sensorGroupId);

    List<Sensor> findBySensorGroupId(String sensorGroupId);
}
