package com.blinker.atom.domain.appuser;

import com.blinker.atom.domain.sensor.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserSensorRepository extends JpaRepository<AppUserSensor, Long> {
    Optional<AppUserSensor> findBySensorAndAppUser(Sensor sensor, AppUser appUser);
}
