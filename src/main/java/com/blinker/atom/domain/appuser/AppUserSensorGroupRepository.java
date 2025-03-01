package com.blinker.atom.domain.appuser;

import com.blinker.atom.domain.sensor.SensorGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppUserSensorGroupRepository extends JpaRepository<AppUserSensorGroup, Long> {
    boolean existsByAppUserAndSensorGroup(AppUser appUser, SensorGroup sensorGroup);
    AppUserSensorGroup findByAppUserAndSensorGroup(AppUser appUser, SensorGroup sensorGroup);
}
