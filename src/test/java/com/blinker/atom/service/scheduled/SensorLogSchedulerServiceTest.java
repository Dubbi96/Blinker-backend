package com.blinker.atom.service.scheduled;

import com.blinker.atom.domain.sensor.Sensor;
import com.blinker.atom.domain.sensor.SensorGroup;
import com.blinker.atom.domain.sensor.SensorGroupRepository;
import com.blinker.atom.domain.sensor.SensorLog;
import com.blinker.atom.domain.sensor.SensorLogRepository;
import com.blinker.atom.domain.sensor.SensorRepository;
import com.blinker.atom.dto.thingplug.ParsedSensorLogDto;
import com.blinker.atom.util.GCSUtil;
import com.blinker.atom.util.ParsingUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensorLogSchedulerServiceTest {

    private static final String OWN_GROUP_ID = "0000102140ca63fffe1e299f";
    private static final String ECHO_GROUP_ID = "0000102140ca63fffe1e29a2";

    private static final String OWN_REPORT =
            "67a6291efe0454323632000000000061070707070707070000000090000014000300009f291efe0807000000000000000000ef";
    private static final String ECHO_REPORT =
            "67a6291efe0100326632000000000062080808080808080000000090000014000d0000a2291efe060400000000000000000084";
    private static final String CORRUPT_REPORT =
            "67ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffd0d0d0d0d0c401003247324400006105";

    @Mock
    private SensorGroupRepository sensorGroupRepository;
    @Mock
    private SensorLogRepository sensorLogRepository;
    @Mock
    private SensorRepository sensorRepository;
    @Mock
    private GCSUtil gcsUtil;

    private SensorLogSchedulerService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new SensorLogSchedulerService(
                sensorGroupRepository, sensorLogRepository, sensorRepository, objectMapper, gcsUtil);
    }

    @Test
    void 손상된_프레임은_센서를_생성하지_않고_처리_완료한다() throws Exception {
        SensorGroup group = group(OWN_GROUP_ID, "9f291efe");
        SensorLog log = sensorLog("corrupt-event", group, CORRUPT_REPORT);
        when(sensorGroupRepository.findById(OWN_GROUP_ID)).thenReturn(Optional.of(group));
        when(sensorLogRepository.save(any(SensorLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.processSensorLog(log);

        assertTrue(log.isProcessed());
        verify(sensorRepository, never()).findByDeviceNumber(any());
        verify(sensorRepository, never()).save(any());
    }

    @Test
    void 이웃_그룹_로그는_번호와_롤백_원문을_덮어쓰지_않는다() {
        Sensor sensor = Sensor.builder()
                .id(531L)
                .sensorGroup(group(OWN_GROUP_ID, "9f291efe"))
                .deviceNumber("a6291efe")
                .groupPositionNumber(7L)
                .lastlyModifiedWith(OWN_REPORT)
                .latitude(36.0)
                .longitude(127.0)
                .build();
        ParsedSensorLogDto echo = ParsingUtil.parseMessage(ECHO_REPORT);

        service.updateSensor(sensor, echo, ECHO_REPORT, false);

        ArgumentCaptor<Sensor> captor = ArgumentCaptor.forClass(Sensor.class);
        verify(sensorRepository).save(captor.capture());
        assertEquals(7L, captor.getValue().getGroupPositionNumber());
        assertEquals(OWN_REPORT, captor.getValue().getLastlyModifiedWith());
    }

    @Test
    void 이웃_그룹_원문은_롤백에서_무시한다() {
        Sensor sensor = Sensor.builder()
                .id(531L)
                .sensorGroup(group(OWN_GROUP_ID, "9f291efe"))
                .deviceNumber("a6291efe")
                .groupPositionNumber(7L)
                .lastlyModifiedWith(ECHO_REPORT)
                .build();

        service.rollbackSensors(sensor);

        verify(sensorRepository, never()).save(any());
    }

    @Test
    void 현재_그룹의_보고가_있으면_이웃_그룹_보고가_더_많아도_옮기지_않는다() throws Exception {
        SensorGroup ownGroup = group(OWN_GROUP_ID, "9f291efe");
        SensorGroup echoGroup = group(ECHO_GROUP_ID, "a2291efe");
        Sensor sensor = Sensor.builder()
                .id(531L)
                .sensorGroup(ownGroup)
                .deviceNumber("a6291efe")
                .groupPositionNumber(7L)
                .lastlyModifiedWith(OWN_REPORT)
                .build();
        SensorLog echoLog = sensorLog("echo-event", echoGroup, ECHO_REPORT);
        when(sensorLogRepository.findByEventCode("echo-event")).thenReturn(Optional.of(echoLog));
        when(sensorRepository.findByDeviceNumber("a6291efe")).thenReturn(Optional.of(sensor));
        when(sensorLogRepository.countRecentByDeviceNumberAndSensorGroupId(
                eq("a6291efe"), eq(OWN_GROUP_ID), any(LocalDateTime.class))).thenReturn(100L);
        when(sensorLogRepository.countRecentByDeviceNumberAndSensorGroupId(
                eq("a6291efe"), eq(ECHO_GROUP_ID), any(LocalDateTime.class))).thenReturn(200L);

        service.checkRedirectedSensor(ECHO_GROUP_ID, List.of("echo-event"), echoGroup);

        assertEquals(OWN_GROUP_ID, sensor.getSensorGroup().getId());
        assertEquals(7L, sensor.getGroupPositionNumber());
        verify(sensorRepository, never()).save(any());
    }

    @Test
    void 현재_그룹_보고가_끊기고_새_그룹_보고가_있으면_번호와_롤백_원문을_같이_옮긴다() throws Exception {
        SensorGroup oldGroup = group(OWN_GROUP_ID, "9f291efe");
        SensorGroup newGroup = group(ECHO_GROUP_ID, "a2291efe");
        Sensor sensor = Sensor.builder()
                .id(531L)
                .sensorGroup(oldGroup)
                .deviceNumber("a6291efe")
                .groupPositionNumber(7L)
                .lastlyModifiedWith(OWN_REPORT)
                .build();
        SensorLog newGroupLog = sensorLog("move-event", newGroup, ECHO_REPORT);
        when(sensorLogRepository.findByEventCode("move-event")).thenReturn(Optional.of(newGroupLog));
        when(sensorRepository.findByDeviceNumber("a6291efe")).thenReturn(Optional.of(sensor));
        when(sensorLogRepository.countRecentByDeviceNumberAndSensorGroupId(
                eq("a6291efe"), eq(OWN_GROUP_ID), any(LocalDateTime.class))).thenReturn(0L);
        when(sensorLogRepository.countRecentByDeviceNumberAndSensorGroupId(
                eq("a6291efe"), eq(ECHO_GROUP_ID), any(LocalDateTime.class))).thenReturn(5L);

        service.checkRedirectedSensor(ECHO_GROUP_ID, List.of("move-event"), newGroup);

        assertEquals(ECHO_GROUP_ID, sensor.getSensorGroup().getId());
        assertEquals(4L, sensor.getGroupPositionNumber());
        assertEquals(ECHO_REPORT, sensor.getLastlyModifiedWith());
        verify(sensorRepository).save(sensor);
    }

    private SensorGroup group(String id, String groupKey) {
        return SensorGroup.builder().id(id).groupKey(groupKey).build();
    }

    private SensorLog sensorLog(String eventCode, SensorGroup group, String payload) throws Exception {
        String eventDetails = objectMapper.writeValueAsString(java.util.Map.of(
                "con", payload,
                "ct", "2026-08-02T08:01:00+09:00"));
        return SensorLog.builder()
                .sensorGroup(group)
                .eventCode(eventCode)
                .eventDetails(eventDetails)
                .sensorDeviceNumber(payload.substring(2, 10))
                .isProcessed(false)
                .createdAt(LocalDateTime.of(2026, 8, 2, 8, 1))
                .build();
    }
}
