package com.blinker.atom.domain.appuser;

import com.blinker.atom.common.ApplicationContextProvider;
import com.blinker.atom.service.scheduled.AppUserSensorGroupService;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "app_user")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "app_user_id")
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true, length = 50)
    private String userId;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "salt", nullable = false, length = 50)
    private String salt;

    @Column(name = "roles", columnDefinition = "text[]")
    @Type(value = ListArrayType.class, parameters = {
            @Parameter(
                    name = ListArrayType.SQL_ARRAY_TYPE,
                    value = "text"
            )
    })
    @Builder.Default
    private List<Role> roles = new ArrayList<>();

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

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

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PostPersist
    public void afterUserCreated() {
        if (this.roles.contains(Role.ADMIN)) {
            AppUserSensorGroupService service = ApplicationContextProvider.getBean(AppUserSensorGroupService.class);
            service.assignUserToAllSensorGroupsAsync(this.id);
        }
    }
}