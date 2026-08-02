package com.blinker.atom.util;

import com.blinker.atom.dto.thingplug.ParsedSensorLogDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 페이로드는 전부 운영 아카이브(gs://dubbi-blinker/sensor-log-archive)에서 그대로 가져온 실물이다.
 */
class ParsingUtilTest {

    // --- 정상 로그 (2026-07-27) ---
    // 41번 그룹(컨테이너 ...1e299f) 마스터
    private static final String MASTER_5BB57C50 =
            "675bb57c5001003237320000000000630808080808080800000000900000561f0000009f291efe080000000000000000000001";
    // 같은 기기가 컨테이너마다 다른 묶음내번호로 보고된다 — 핀 번호 중복의 원인
    private static final String A6291EFE_IN_299F =
            "67a6291efe0454323632000000000061070707070707070000000090000014000300009f291efe0807000000000000000000ef";
    private static final String A6291EFE_IN_29A2 =
            "67a6291efe0100326632000000000062080808080808080000000090000014000d0000a2291efe060400000000000000000084";

    // --- 손상 프레임 (2026-06-01 09:37~09:39, 컨테이너 ...1df132) ---
    private static final String CORRUPT_FF =
            "67ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffd0d0d0d0d0c401003247324400006105";
    private static final String CORRUPT_ZERO =
            "6700040000000000000000000000000000000000000000000000000000000000000000d0d0d0d0d0c600000000000000000000";
    private static final String CORRUPT_REPEAT =
            "6706060606060605050505574900000000000000000000000000000000000000000000d0d0d0d0d0ca0000906e4c5f4500d0ed";

    @Test
    void 실물_정상로그의_묶음정보를_그대로_읽는다() {
        ParsedSensorLogDto master = ParsingUtil.parseMessage(MASTER_5BB57C50);
        assertEquals("5bb57c50", master.getDeviceNumber());
        assertEquals("9f291efe", master.getGroupNumber());
        assertEquals(8, master.getSignalsInGroup());
        assertEquals(0, master.getGroupPositionNumber()); // 0 = 마스터

        ParsedSensorLogDto own = ParsingUtil.parseMessage(A6291EFE_IN_299F);
        assertEquals(7, own.getGroupPositionNumber());

        ParsedSensorLogDto echo = ParsingUtil.parseMessage(A6291EFE_IN_29A2);
        assertEquals("a6291efe", echo.getDeviceNumber()); // 같은 기기인데
        assertEquals(4, echo.getGroupPositionNumber());   // 이웃 컨테이너에서는 4번
    }

    @Test
    void 정상로그는_묶음정보_검증을_통과한다() {
        for (String payload : new String[]{MASTER_5BB57C50, A6291EFE_IN_299F, A6291EFE_IN_29A2}) {
            assertTrue(ParsingUtil.hasValidBundleInfo(ParsingUtil.parseMessage(payload)), payload);
        }
    }

    @Test
    void 길이만_102자인_손상프레임은_묶음정보_검증에서_걸린다() {
        for (String payload : new String[]{CORRUPT_FF, CORRUPT_ZERO, CORRUPT_REPEAT}) {
            ParsedSensorLogDto parsed = ParsingUtil.parseMessage(payload);
            assertEquals(102, payload.length());
            assertFalse(parsed.isParsingError(), "길이 검증만으로는 못 걸러낸다: " + payload);
            assertEquals(208, parsed.getSignalsInGroup());
            assertTrue(parsed.getGroupPositionNumber() >= 194); // 슬레이브 100번대의 정체
            assertFalse(ParsingUtil.hasValidBundleInfo(parsed), payload);
        }
    }

    @Test
    void 묶음_없음_0_0_은_정상으로_본다() {
        ParsedSensorLogDto noBundle = new ParsedSensorLogDto();
        noBundle.setSignalsInGroup(0);
        noBundle.setGroupPositionNumber(0);
        assertTrue(ParsingUtil.hasValidBundleInfo(noBundle));
    }

    @Test
    void 번호가_신호기수_이상이면_걸린다() {
        ParsedSensorLogDto overflow = new ParsedSensorLogDto();
        overflow.setSignalsInGroup(4);
        overflow.setGroupPositionNumber(4); // 4대 묶음의 유효 번호는 0~3
        assertFalse(ParsingUtil.hasValidBundleInfo(overflow));
    }

    @Test
    void 손상된_디바이스_번호와_센서_보고를_거부한다() {
        assertFalse(ParsingUtil.hasValidDeviceNumber("00000000"));
        assertFalse(ParsingUtil.hasValidDeviceNumber("ffffffff"));
        assertFalse(ParsingUtil.isValidSensorReport(ParsingUtil.parseMessage(CORRUPT_FF)));
        assertFalse(ParsingUtil.isValidSensorReport(ParsingUtil.parseMessage(CORRUPT_ZERO)));
        assertFalse(ParsingUtil.isValidSensorReport(ParsingUtil.parseMessage(CORRUPT_REPEAT)));
    }
}
