package com.blinker.atom.domain.sensor;

import jakarta.persistence.*;
import lombok.*;


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
    @Column(name = "sensor_group_id", length = 50)
    private String id;

    @Column(name = "sensor_group_key", length = 50)
    private String groupKey;

    @Column(name = "sensor_count")
    private Long sensorCount;

    @Column(name = "fault_count")
    private Long faultCount;

    @Column(name = "ssid")
    private String ssid;

    @Column(name = "display_order")
    private Long displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime updatedAt;

    @Column(name = "ssid_updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime ssidUpdatedAt;

    @OneToMany(mappedBy = "sensorGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Sensor> sensors;

    public void updateOrder(Long order) {
        this.displayOrder = order;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

}