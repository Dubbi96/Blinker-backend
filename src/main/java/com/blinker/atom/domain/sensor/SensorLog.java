package com.blinker.atom.domain.sensor;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_log")
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

    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}