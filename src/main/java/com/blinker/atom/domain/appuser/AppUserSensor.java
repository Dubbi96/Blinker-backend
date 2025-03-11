package com.blinker.atom.domain.appuser;

import com.blinker.atom.domain.sensor.Sensor;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "app_user_sensor", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"sensor_id", "app_user_id"})
})
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppUserSensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "app_user_sensor_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Sensor sensor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private AppUser appUser;

    @Column(name = "memo", nullable = false)
    private String memo;

    public void updateMemo(String memo) {
        this.memo = memo;
    }
}
