
package com.blinker.atom.domain.appuser;

import com.blinker.atom.domain.sensor.SensorGroup;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "app_user_sensor_group")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppUserSensorGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "app_user_sensor_group_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_group_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SensorGroup sensorGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private AppUser appUser;

}
