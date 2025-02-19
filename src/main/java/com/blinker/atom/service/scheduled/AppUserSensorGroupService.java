package com.blinker.atom.service.scheduled;

import com.blinker.atom.config.error.CustomException;
import com.blinker.atom.config.error.ErrorValue;
import com.blinker.atom.domain.appuser.*;
import com.blinker.atom.domain.sensor.SensorGroup;
import com.blinker.atom.domain.sensor.SensorGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppUserSensorGroupService {

    private final AppUserRepository appUserRepository;
    private final SensorGroupRepository sensorGroupRepository;
    private final AppUserSensorGroupRepository appUserSensorGroupRepository;

    @Async
    @Scheduled(fixedRate = 86400000)  // 1일 1회 실행 (1000ms * 60 * 60 * 24)
    @Transactional
    public void asyncUpdateAdminSensorGroups() {
        log.info("`ADMIN` 유저의 SensorGroup 자동 업데이트 실행...");
        List<AppUser> adminUsers = appUserRepository.findByRolesContaining(Role.ADMIN.name());

        for (AppUser admin : adminUsers) {
            assignSensorGroups(admin);
        }
        log.info("모든 `ADMIN` 계정의 SensorGroup 업데이트 완료");
    }

    /**
     * `ADMIN` 유저에게 모든 `SensorGroup` 할당
     */
    private void assignSensorGroups(AppUser user) {
        List<SensorGroup> sensorGroups = sensorGroupRepository.findAll();
        for (SensorGroup group : sensorGroups) {
            boolean alreadyAssigned = appUserSensorGroupRepository.existsByAppUserAndSensorGroup(user, group);
            if (!alreadyAssigned) {
                AppUserSensorGroup newAssignment = AppUserSensorGroup.builder()
                        .appUser(user)
                        .sensorGroup(group)
                        .build();
                appUserSensorGroupRepository.save(newAssignment);
            }
        }
    }

    @Async
    @Transactional
    public void assignUserToAllSensorGroupsAsync(Long userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorValue.ACCOUNT_NOT_FOUND.getMessage()));

        if (user.getRoles().contains(Role.ADMIN)) {
            assignSensorGroups(user);
            log.info("ADMIN 계정 {}에게 모든 SensorGroup 자동 할당 완료", userId);
        }
    }
}