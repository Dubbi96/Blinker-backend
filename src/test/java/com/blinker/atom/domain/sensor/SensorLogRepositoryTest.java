package com.blinker.atom.domain.sensor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@ActiveProfiles("test")
class SensorLogRepositoryTest {

    @Autowired
    private SensorLogRepository sensorLogRepository;

    @Test
    void 최근_그룹별_기기_로그_수_쿼리를_기동시_검증한다() {
        // Spring Data는 컨텍스트 생성 중 모든 @Query를 파싱한다.
        // 빈 H2 스키마에 SQL을 실행하지 않고 저장소 생성 성공 자체를 검증한다.
        assertNotNull(sensorLogRepository);
    }
}
