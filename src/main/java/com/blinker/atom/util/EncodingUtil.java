package com.blinker.atom.util;

import com.blinker.atom.dto.thingplug.SensorUpdateRequestDto;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
public class EncodingUtil {

    public static String encodeToHex(SensorUpdateRequestDto content, Integer cmd, int sequenceNumber) {
        if (content == null) {
            throw new IllegalArgumentException("Invalid request: content is null");
        }

        StringBuilder hexString = new StringBuilder();

        // 1. CMD (2자리)
        hexString.append(String.format("%02x", cmd));

        // 2. Device Number (8자리)
        hexString.append(content.getDeviceNumber());

        // 3. Device ID (2자리)
        hexString.append(String.format("%02x", content.getDeviceId()));

        // 4. Signal Strength (2자리)
        hexString.append(String.format("%02x", content.getPositionSignalStrength()));

        // 5. Signal Threshold (2자리)
        hexString.append(String.format("%02x", content.getPositionSignalThreshold()));

        // 6. Communication Signal Strength (2자리)
        hexString.append(String.format("%02x", content.getCommunicationSignalStrength()));

        // 7. Communication Signal Threshold (2자리)
        hexString.append(String.format("%02x", content.getCommunicationSignalThreshold()));

        // 8. Wireless 235 Strength (2자리)
        hexString.append(String.format("%02x", content.getWireless235Strength()));

        // 9. Server Time (8자리)
        String serverTimeHex = encodeServerTime();
        hexString.append(serverTimeHex);

        // 10. Device Settings (2자리)
        String deviceSettingsHex = encodeDeviceSettings(content.getDeviceSettings());
        hexString.append(deviceSettingsHex);

        // 11. Volume Settings (14자리)
        String volumeSettingsHex = encodeVolumeSettings(content);
        hexString.append(volumeSettingsHex);

        // 12. Silent Settings (8자리)
        String silentSettingsHex = encodeSilentSettings(content);
        hexString.append(silentSettingsHex);

        // 13. Communication Interval (2자리)
        hexString.append(String.format("%02x", content.getCommunicationInterval()));

        // 14. Fault Information (4자리)
        hexString.append("0000");

        // 15. SW Version (2자리)
        hexString.append(String.format("%02x", content.getSwVersion()));

        // 16. HW Version (2자리)
        hexString.append(String.format("%02x", content.getHwVersion()));

        // 17. Button Count (2자리)
        hexString.append("00");

        // 18. Position Guide Count (2자리)
        hexString.append("00");

        // 19. Signal Guide Count (2자리)
        hexString.append("00");

        // 20. Group Number (8자리 - Little-Endian 처리)
        hexString.append(content.getGroupNumber());

        // 21. Signals in Group (2자리)
        hexString.append(String.format("%02x", content.getSignalsInGroup()));

        // 22. Group Position Number (2자리)
        hexString.append(String.format("%02x", content.getGroupPositionNumber()));

        // 23. padding (16 자리)
        hexString.append("0000000000000000");

        // 24. Data Type (2자리)
        hexString.append("00");

        // 25. Sequence Number (2자리)
        hexString.append(String.format("%02x", sequenceNumber));

        if (hexString.length() != 102) {
            System.out.println(hexString);
            throw new IllegalStateException("Final encoded data length is not 102: " + hexString.length());
        }

        return hexString.toString().toUpperCase();
    }

     public static String encodeServerTime() {
        // 현재 시간 가져오기
        LocalDateTime now = LocalDateTime.now();

        // 날짜/시간을 YYMMDDHHmm 포맷으로 변환
        String formattedTime = now.format(DateTimeFormatter.ofPattern("yyMMddHHmm"));

        // BCD(Binary Coded Decimal) 변환을 위한 16진수 변환
        long bcdTime = Long.parseLong(formattedTime);

        // 8자리 16진수 변환
         return String.format("%08X", bcdTime);
    }

    private static String encodeDeviceSettings(Map<String, String> deviceSettings) {
        if (deviceSettings == null || deviceSettings.isEmpty()) {
            return "00"; // 기본값 (설정 없음)
        }

        int deviceSettingValue = 0;

        // "Proximity" 설정 (일반지주 연산상 없음)
        String proximity = deviceSettings.get("Proximity");
        if ("Close Proximity".equals(proximity)) deviceSettingValue |= 8;
        if ("Single Proximity".equals(proximity)) deviceSettingValue |= 16;

        // "Configuration" 설정 (미설정 연산상 없음)
        String configuration = deviceSettings.get("Configuration");
        if ("Configured".equals(configuration)) deviceSettingValue |= 32;

        // "Priority" 설정 (남자 우선 방송 연산상 없음)
        String priority = deviceSettings.get("Priority");
        if ("Female Priority Broadcast".equals(priority)) deviceSettingValue |= 64;

        // "Sound" 설정 (새 연산상 없음)
        String sound = deviceSettings.get("Sound");
        if ("Cricket".equals(sound)) deviceSettingValue |= 2;

        // "Crossroad" 설정 (단일로 연산상 없음)
        String crossroad = deviceSettings.get("Crossroad");
        if ("Intersection".equals(crossroad)) deviceSettingValue |= 4;

        // "Gender" 설정 (남자 음성 연산상 없음)
        String gender = deviceSettings.get("Gender");
        if ("Female".equals(gender)) deviceSettingValue |= 1;

        return String.format("%02x", deviceSettingValue);
    }

    private static String encodeVolumeSettings(SensorUpdateRequestDto content) {
        return String.format("%02x%02x%02x%02x%02x%02x%02x",
                content.getBirdVolume() != null ? content.getBirdVolume() : 0,
                content.getCricketVolume() != null ? content.getCricketVolume() : 0,
                content.getDingdongVolume() != null ? content.getDingdongVolume() : 0,
                content.getFemaleVolume() != null ? content.getFemaleVolume() : 0,
                content.getMaleVolume() != null ? content.getMaleVolume() : 0,
                content.getMinuetVolume() != null ? content.getMinuetVolume() : 0,
                content.getSystemVolume() != null ? content.getSystemVolume() : 0
        );
    }

    private static String encodeSilentSettings(SensorUpdateRequestDto content) {
        return String.format("%02x%02x%02x%02x",
                content.getFemaleMute1() != null ? content.getFemaleMute1() : 0,
                content.getFemaleMute2() != null ? content.getFemaleMute2() : 0,
                content.getMaleMute1() != null ? content.getMaleMute1() : 0,
                content.getMaleMute2() != null ? content.getMaleMute2() : 0
        );
    }

}
