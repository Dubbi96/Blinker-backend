package com.blinker.atom.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SensorSettingRepository extends JpaRepository<SensorSetting, Long> {
}
