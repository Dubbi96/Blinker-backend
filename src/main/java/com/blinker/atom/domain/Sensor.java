package com.blinker.atom.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "sensor")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sensor_id")
    private Long sensorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private SensorGroup group;

    @Column(name = "sensor_key", nullable = false, length = 50)
    private String sensorKey;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "address")
    private String address;

    @Column(name = "is_fault")
    private Boolean isFault;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "cmd", length = 10)
    private String cmd;

    @Column(name = "device_number", length = 50, nullable = false)
    private String deviceNumber;

    @Column(name = "device_id", nullable = false)
    private Integer deviceId;

    @Column(name = "position_signal_strength")
    private Integer positionSignalStrength;

    @Column(name = "position_signal_threshold")
    private Integer positionSignalThreshold;

    @Column(name = "comm_signal_strength")
    private Integer commSignalStrength;

    @Column(name = "comm_signal_threshold")
    private Integer commSignalThreshold;

    @Column(name = "wireless_235_strength")
    private Integer wireless235Strength;

    @Column(name = "server_time")
    private Long serverTime;

    @Column(name = "device_settings", columnDefinition = "TEXT")
    private String deviceSettings;

    @ElementCollection
    @CollectionTable(name = "sensor_volume_settings", joinColumns = @JoinColumn(name = "sensor_id"))
    @MapKeyColumn(name = "volume_type")
    @Column(name = "volume_level")
    private Map<String, Integer> volumeSettings;

    @ElementCollection
    @CollectionTable(name = "sensor_silent_settings", joinColumns = @JoinColumn(name = "sensor_id"))
    @MapKeyColumn(name = "mute_type")
    @Column(name = "mute_level")
    private Map<String, Integer> silentSettings;

    @Column(name = "comm_interval")
    private Integer commInterval;

    @Column(name = "fault_information", columnDefinition = "TEXT")
    private String faultInformation;

    @Column(name = "sw_version")
    private Integer swVersion;

    @Column(name = "hw_version")
    private Integer hwVersion;

    @Column(name = "button_click_count", columnDefinition = "INT DEFAULT 0")
    private Integer buttonClickCount;

    @Column(name = "location_guide_count", columnDefinition = "INT DEFAULT 0")
    private Integer locationGuideCount;

    @Column(name = "signal_guide_count", columnDefinition = "INT DEFAULT 0")
    private Integer signalGuideCount;

    @Column(name = "group_number", length = 50)
    private String groupNumber;

    @Column(name = "signals_in_group")
    private Integer signalsInGroup;

    @Column(name = "group_position_number")
    private Integer groupPositionNumber;

    @Column(name = "data_type")
    private Integer dataType;

    @Column(name = "sequence_number")
    private Integer sequenceNumber;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}