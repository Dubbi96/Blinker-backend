package com.blinker.atom.domain.sensor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SensorGroupRepository extends JpaRepository<SensorGroup, String> {

    @Modifying
    @Transactional
    @Query(value = "UPDATE sensor_group SET sensor_count = :sensorCount, " +
                   "sensor_group_key = :groupKey, " +
                   "fault_count = (" +
                   "   SELECT COUNT(*) FROM sensor " +
                   "   WHERE sensor_group_id = :sensorGroupId " +
                   "   AND EXISTS (" +
                   "       SELECT 1 FROM jsonb_each_text(sensor.fault_information) AS j(key, value) " +
                   "       WHERE CAST(j.value AS BOOLEAN) = true" +
                   "   )" +
                   "), " +
                   "updated_at = CURRENT_TIMESTAMP " +
                   "WHERE sensor_group_id = :sensorGroupId",
           nativeQuery = true)
    void updateSensorGroup(@Param("sensorGroupId") String sensorGroupId,
                           @Param("groupKey") String groupKey,
                           @Param("sensorCount") Long sensorCount);

    @Modifying
    @Transactional
    @Query("UPDATE SensorGroup s SET s.ssid = :ssid, s.ssidUpdatedAt = current_timestamp WHERE s.id = :id")
    void updateSsid(@Param("id") String id, @Param("ssid") String ssid);


    @Query("SELECT sg FROM SensorGroup sg " +
           "JOIN FETCH sg.sensors s " +
           "JOIN AppUserSensorGroup ausg ON sg.id = ausg.sensorGroup.id " +
           "WHERE ausg.appUser.id = :userId")
    List<SensorGroup> findSensorGroupsWithSensorsByUserId(@Param("userId") Long userId);
}