package com.blinker.atom.domain.sensor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SensorRepository extends JpaRepository<Sensor,Long> {
    Optional<Sensor> findBySensorGroup(SensorGroup sensorGroup);
    Optional<Sensor> findSensorByDeviceId(Long deviceId);
    Optional<Sensor> findSensorByDeviceNumber(String deviceNumber);
}
