package com.blinker.atom.domain.sensor;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_log", uniqueConstraints = @UniqueConstraint(columnNames = {"sensor_group_id","event_code"}))
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_group_id", nullable = false)
    private SensorGroup sensorGroup;

    @Column(name = "event_code", length = 50, nullable = false)
    private String eventCode;

    @Type(JsonType.class)
    @Column(name = "event_details", columnDefinition = "jsonb")
    private String eventDetails;

    @Column(name = "sensor_device_number")
    private String sensorDeviceNumber;

    @Column(name = "is_processed")
    private boolean isProcessed;

    @Column(name = "created_at", updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createdAt;

    public void markAsProcessed() {
        this.isProcessed = true;
    }
}