package com.example.hikconnectbackend.controller;

import com.example.hikconnectbackend.entities.AccessLog;
import com.example.hikconnectbackend.repositories.AccessLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.hikconnectbackend.services.HikvisionService;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/hikvision")
public class HikvisionController {
    @Autowired
    private HikvisionService hikvisionService;

    @Autowired
    private AccessLogRepository accessLogRepository;

    private final String UPLOAD_DIR = "uploads/images/";

    @GetMapping("/sync-logs")
    public String synLogs() {
        hikvisionService.fetchAndSaveAccessLogs();
        log.info("Access logs synchronized successfully.");
        return "Access logs synchronized successfully.";
    }

    @GetMapping("/logs")
    public ResponseEntity<List<AccessLog>> getAllAccessLogs() {
        List<AccessLog> accessLogs = hikvisionService.getAllAccessLogs();
        return ResponseEntity.ok(accessLogs);
    }

    @GetMapping("/logs/page")
    public ResponseEntity<Page<AccessLog>> getAccessLogsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        Page<AccessLog> logsPage = hikvisionService.getAccessLogsWithPagination(page, size, startDate, endDate);
        return ResponseEntity.ok(logsPage);
    }

    @PostMapping(value = "/receive", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> receiveEvent(MultipartHttpServletRequest request) {
        try {
            String eventLog = request.getParameter("AccessControllerEvent");

            if (eventLog != null) {
                log.info("Raw JSON from Webhook: {}", eventLog);

                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(eventLog);

                JsonNode accessEvent = rootNode.has("AccessControllerEvent") ? rootNode.path("AccessControllerEvent") : rootNode;

                if (!accessEvent.isMissingNode()) {
                    int minor = accessEvent.path("subEventType").asInt();

                    if (minor == 75) {
                        // 1. ดึงเวลาจากชั้นนอกสุด (Root Node) ที่ชื่อ dateTime
                        String time = rootNode.path("dateTime").asText();

                        // 2. ดึงรหัสพนักงาน โดยเผื่อไว้ทั้งกรณีที่เครื่องส่งเป็น String หรือ Integer
                        String employeeId = accessEvent.has("employeeNoString")
                                ? accessEvent.path("employeeNoString").asText()
                                : accessEvent.path("employeeNo").asText();

                        if (employeeId != null && !employeeId.isEmpty() && !time.isEmpty()) {

                            if (!accessLogRepository.existsByEmployeeIdAndScanTime(employeeId, time)) {
                                String name = accessEvent.path("name").asText();
                                int serialNo = accessEvent.path("serialNo").asInt();

                                String deviceIp = rootNode.path("ipAddress").asText(); // ดึง IP จาก Root
                                Integer doorNo = accessEvent.has("doorNo") ? accessEvent.path("doorNo").asInt() : 1;
                                String authType = accessEvent.path("currentVerifyMode").asText();

                                String savedPicturePath = null;

                                // รับไฟล์รูปภาพ
                                MultipartFile pictureFile = request.getFile("Picture");
                                if (pictureFile != null && !pictureFile.isEmpty()) {
                                    Path uploadPath = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
                                    if (!Files.exists(uploadPath)) {
                                        Files.createDirectories(uploadPath);
                                    }

                                    String fileName = employeeId + "_" + System.currentTimeMillis() + ".jpg";
                                    Path filePath = uploadPath.resolve(fileName);
                                    pictureFile.transferTo(filePath.toFile());
                                    savedPicturePath = filePath.toString();
                                }

                                AccessLog logEntry = new AccessLog();
                                logEntry.setEmployeeId(employeeId);
                                logEntry.setName(name);
                                logEntry.setScanTime(time);
                                logEntry.setSerialNo(serialNo);
                                logEntry.setDoorNo(doorNo);
                                logEntry.setDeviceIp(deviceIp);
                                logEntry.setAuthType(authType);

                                logEntry.setPicturePath(savedPicturePath);

                                accessLogRepository.save(logEntry);
                                log.info("Saved new access log for: {} at {}", name, time);
                            }
                        }
                    }
                }
            }
            return ResponseEntity.ok("Success");

        } catch (Exception e) {
            log.error("Error processing Hikvision webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
        }
    }
}
