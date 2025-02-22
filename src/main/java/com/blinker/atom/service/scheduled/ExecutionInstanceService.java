package com.blinker.atom.service.scheduled;

import com.blinker.atom.dto.sensor.SensorExecutionInstanceResponseDto;
import com.blinker.atom.util.HttpClientUtil;
import com.blinker.atom.util.XmlUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionInstanceService {

    private final ObjectMapper objectMapper;

    @Value("${thingplug.base.url}")
    private String baseUrl;

    @Value("${thingplug.app.eui}")
    private String appEui;

    @Value("${thingplug.headers.x-m2m-origin}")
    private String origin;

    @Value("${thingplug.headers.uKey}")
    private String uKey;

    @Value("${thingplug.headers.x-m2m-ri}")
    private String requestId;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    private static final Map<Long, String> EXECUTE_STATUS_MAP = Map.of(
            1L, "INITIATED",
            2L, "PENDING",
            3L, "FINISHED",
            4L, "CANCELLING",
            5L, "CANCELLED",
            6L, "STATUS_NON_CANCELLABLE"
    );

    private static final Map<Long, String> EXECUTE_RESULT_MAP = Map.ofEntries(
            Map.entry(0L, "STATUS_REQUEST_SUCCEED"),
            Map.entry(1L, "STATUS_REQUEST_UNSUPPORTED"),
            Map.entry(2L, "STATUS_REQUEST_DENIED"),
            Map.entry(3L, "STATUS_CANCELLATION_DENIED"),
            Map.entry(4L, "STATUS_INTERNAL_ERROR"),
            Map.entry(5L, "STATUS_INVALID_ARGUMENTS"),
            Map.entry(6L, "STATUS_RESOURCES_EXCEEDED"),
            Map.entry(7L, "STATUS_FILE_TRANSFER_FAILED"),
            Map.entry(8L, "STATUS_FILE_TRANSFER_SERVER_AUTHENTICATION_FAILURE"),
            Map.entry(9L, "STATUS_UNSUPPORTED_PROTOCOL"),
            Map.entry(10L, "STATUS_UPLOAD_FAILED"),
            Map.entry(11L, "STATUS_FILE_TRANSFER_FAILED_MULTICAST_GROUP_UNABLE_JOIN"),
            Map.entry(12L, "STATUS_FILE_TRANSFER_FAILED_SERVER_CONTACT_FAILED"),
            Map.entry(13L, "STATUS_FILE_TRANSFER_FAILED_FILE_ACCESS_FAILED"),
            Map.entry(14L, "STATUS_FILE_TRANSFER_FAILED_DOWNLOAD_INCOMPLETE"),
            Map.entry(15L, "STATUS_FILE_TRANSFER_FAILED_FILE_CORRUPTED"),
            Map.entry(16L, "STATUS_FILE_TRANSFER_FILE_AUTHENTICATION_FAILURE"),
            Map.entry(17L, "STATUS_FILE_TRANSFER_FAILED"),
            Map.entry(18L, "STATUS_FILE_TRANSFER_SERVER_AUTHENTICATION_FAILURE"),
            Map.entry(19L, "STATUS_FILE_TRANSFER_WINDOW_EXCEEDED"),
            Map.entry(20L, "STATUS_INVALID_UUID_FORMAT"),
            Map.entry(21L, "STATUS_UNKNOWN_EXECUTION_ENVIRONMENT"),
            Map.entry(22L, "STATUS_DISABLED_EXECUTION_ENVIRONMENT"),
            Map.entry(23L, "STATUS_EXECUTION_ENVIRONMENT_MISMATCH"),
            Map.entry(24L, "STATUS_DUPLICATE_DEPLOYMENT_UNIT"),
            Map.entry(25L, "STATUS_SYSTEM_RESOURCES_EXCEEDED"),
            Map.entry(26L, "STATUS_UNKNOWN_DEPLOYMENT_UNIT"),
            Map.entry(27L, "STATUS_INVALID_DEPLOYMENT_UNIT_STATE"),
            Map.entry(28L, "STATUS_INVALID_DEPLOYMENT_UNIT_UPDATE_DOWNGRADE_DISALLOWED"),
            Map.entry(29L, "STATUS_INVALID_DEPLOYMENT_UNIT_UPDATE_UPGRADE_DISALLOWED"),
            Map.entry(30L, "STATUS_INVALID_DEPLOYMENT_UNIT_UPDATE_VERSION_EXISTS"),
            Map.entry(31L, "STATUS_DEVICE_MANAGEMENT_TIME_OUT"),
            Map.entry(32L, "STATUS_NW_SVR_TRANSFER_FAILED")
    );

    public List<SensorExecutionInstanceResponseDto> fetchSensorExecutionLogs() {
        String url = String.format("%s/%s/v1_0?fu=1&ty=8", baseUrl, appEui);
        String response = HttpClientUtil.get(url, origin, uKey, requestId);

        log.info("Fetching Sensor Log at URL: {}", url);

        if (response == null || response.isEmpty()) {
            log.warn("API 응답이 없음");
            return new ArrayList<>();
        }

        List<String[]> executionLog = extractExecutionInstanceUri(response);
        return parseExecutionLogs(executionLog);
    }

    private List<String[]> extractExecutionInstanceUri(String response) {
        List<String[]> result = new ArrayList<>();
        Pattern pattern = Pattern.compile("/mgmtCmd-([a-zA-Z0-9_]+)/execInstance-([a-zA-Z0-9_]+)");
        Matcher matcher = pattern.matcher(response);

        while (matcher.find()) {
            result.add(new String[]{matcher.group(1), matcher.group(2)});
        }

        if (result.isEmpty()) {
            log.error("No valid Content Instance URI found in the response. {}");
        }

        return result;
    }

    private List<SensorExecutionInstanceResponseDto> parseExecutionLogs(List<String[]> eventCodes) {
        return eventCodes.stream()
                .map(event -> CompletableFuture.supplyAsync(
                        () -> fetchExecutionLogDetails(event[0], event[1]), executorService))
                .map(CompletableFuture::join)
                .sorted(Comparator.comparing(SensorExecutionInstanceResponseDto::getCreatedAt).reversed())
                .toList();
    }

    @Async
    public SensorExecutionInstanceResponseDto fetchExecutionLogDetails(String mgmtCmdId, String execInstanceId) {
        String executionInstanceUrl = String.format("%s/%s/v1_0/mgmtCmd-%s/execInstance-%s",
                baseUrl, appEui, mgmtCmdId, execInstanceId);

        try {
            String contentInstanceResponse = HttpClientUtil.get(executionInstanceUrl, origin, uKey, requestId);
            String jsonEventDetail = XmlUtil.convertXmlToJson(contentInstanceResponse);

            JsonNode jsonNode = objectMapper.readTree(jsonEventDetail);

            LocalDateTime eventDateTime = jsonNode.has("ct") ?
                    OffsetDateTime.parse(jsonNode.get("ct").asText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime() :
                    LocalDateTime.now();

            Long exs = jsonNode.has("exs") ? jsonNode.get("exs").asLong() : 0L;
            Long exr = jsonNode.has("exr") ? jsonNode.get("exr").asLong() : 0L;
            String ext = jsonNode.has("ext") ? jsonNode.get("ext").asText() : "";
            String exra = jsonNode.has("exra") ? jsonNode.get("exra").asText() : "";

            String executeStatus = EXECUTE_STATUS_MAP.getOrDefault(exs, "UNKNOWN_STATUS");
            String executeResult = EXECUTE_RESULT_MAP.getOrDefault(exr, "UNKNOWN_RESULT");

            return new SensorExecutionInstanceResponseDto(mgmtCmdId, execInstanceId, eventDateTime, executeStatus, executeResult, ext, exra);
        } catch (Exception e) {
            log.error("Error processing execution instance: {}", executionInstanceUrl, e);
            return null;
        }
    }
}
