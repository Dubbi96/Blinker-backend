package com.blinker.atom.domain;

import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

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
    private Long appUserId;

    @Column(name = "user_id", nullable = false, unique = true, length = 50)
    private String userId;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", unique = true, length = 100)
    private String email;

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

    @Column(name = "created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;
}