package com.blinker.atom.domain.sensor;

import com.blinker.atom.dto.thingplug.ParsedSensorLogDto;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "sensor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sensor_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_group_id", nullable = false)
    private SensorGroup sensorGroup;

    @Column(name = "device_number", length = 50, nullable = false)
    private String deviceNumber;

    @Column(name = "deviceId")
    private Double deviceId;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "position_signal_strength")
    private Long positionSignalStrength;

    @Column(name = "position_signal_threshold")
    private Long positionSignalThreshold;

    @Column(name = "communication_signal_strength")
    private Long communicationSignalStrength;

    @Column(name = "communication_signal_threshold")
    private Long communicationSignalThreshold;

    @Column(name = "wireless_235_strength")
    private Long wireless235Strength;

    @Column(name = "device_setting", columnDefinition = "text[]")
    @Type(value = ListArrayType.class, parameters = {
            @org.hibernate.annotations.Parameter(
                    name = ListArrayType.SQL_ARRAY_TYPE,
                    value = "text"
            )
    })
    private List<String> deviceSetting;

    @Column(name = "female_mute1")
    private Long femaleMute1;

    @Column(name = "female_mute2")
    private Long femaleMute2;

    @Column(name = "male_mute1")
    private Long maleMute1;

    @Column(name = "male_mute2")
    private Long maleMute2;

    @Column(name = "bird_volume")
    private Long birdVolume;

    @Column(name = "cricket_volume")
    private Long cricketVolume;

    @Column(name = "dingdong_volume")
    private Long dingdongVolume;

    @Column(name = "female_volume")
    private Long femaleVolume;

    @Column(name = "male_volume")
    private Long maleVolume;

    @Column(name = "minuet_volume")
    private Long minuetVolume;

    @Column(name = "system_volume")
    private Long systemVolume;

    @Column(name = "communication_interval")
    private Long communicationInterval;

    @Column(name = "server_time")
    private LocalDateTime serverTime;

    @Column(name = "fault_information", columnDefinition = "jsonb")
    @Type(JsonBinaryType.class)
    private Map<String,Boolean> faultInformation;

    @Column(name = "sw_version")
    private Long swVersion;

    @Column(name = "hw_version")
    private Long hwVersion;

    @Column(name = "button_count")
    private Long buttonCount;

    @Column(name = "position_guide_count")
    private Long positionGuideCount;

    @Column(name = "signal_guide_count")
    private Long signalGuideCount;

    @Column(name = "group_position_number")
    private Long groupPositionNumber;

    @Column(name = "lastly_modified_with")
    private String lastlyModifiedWith;

    @Column(name = "address")
    private String address;

    @Column(name = "created_at", updatable = false, nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void updateAddress(String address) {
        this.address = address;
    }

    public void updateLocation(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sensor sensor = (Sensor) o;
        return Objects.equals(deviceNumber, sensor.deviceNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceNumber);
    }
}