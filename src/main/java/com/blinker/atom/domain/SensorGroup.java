package com.blinker.atom.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "sensor_group")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "group_key", nullable = false, length = 50)
    private String groupKey;

    @Column(name = "group_name", length = 100)
    private String groupName;

    @Column(name = "description")
    private String description;

    @Column(name = "sensor_count")
    private Integer sensorCount;

    @Column(name = "fault_count")
    private Integer faultCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Sensor> sensors;
}