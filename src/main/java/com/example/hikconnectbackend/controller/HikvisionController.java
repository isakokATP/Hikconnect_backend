package com.example.hikconnectbackend.controller;

import com.example.hikconnectbackend.entities.AccessLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.hikconnectbackend.services.HikvisionService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/hikvision")
public class HikvisionController {
    @Autowired
    private HikvisionService hikvisionService;

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
}
