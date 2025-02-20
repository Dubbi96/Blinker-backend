package com.blinker.atom.service.scheduled;

import com.blinker.atom.config.error.CustomException;
import com.blinker.atom.config.error.ErrorValue;
import com.blinker.atom.domain.appuser.*;
import com.blinker.atom.domain.sensor.SensorGroup;
import com.blinker.atom.domain.sensor.SensorGroupRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppUserSensorGroupService {

    private final AppUserRepository appUserRepository;
    private final SensorGroupRepository sensorGroupRepository;
    private final AppUserSensorGroupRepository appUserSensorGroupRepository;

    public static boolean IS_UPDATE_ADMIN_GROUP_RUNNING = false;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @PreDestroy
    public void shutdownExecutor() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    @Transactional
    public void updateAdminSensorGroups() {
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

    public void startScheduler() {
        IS_UPDATE_ADMIN_GROUP_RUNNING = true;
        log.info("✅ 스케줄러가 활성화되었습니다.");
    }

    public void stopScheduler() {
        IS_UPDATE_ADMIN_GROUP_RUNNING = false;
        log.info("⛔ 스케줄러가 비활성화되었습니다.");
    }
}