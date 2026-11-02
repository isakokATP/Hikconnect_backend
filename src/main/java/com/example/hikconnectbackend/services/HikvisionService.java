package com.example.hikconnectbackend.services;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpHost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.example.hikconnectbackend.entities.AccessLog;
import com.example.hikconnectbackend.repositories.AccessLogRepository;
import org.springframework.http.*;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.List;
import java.util.Map;

@Service
public class HikvisionService {

    @Autowired
    private AccessLogRepository accessLogRepository;

    private final String HIK_URL = "http://192.168.10.220/ISAPI/AccessControl/AcsEvent?format=json";

    @Scheduled(fixedRate = 300000)
    public void fetchAndSaveAccessLogs() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startTime = now.minusDays(1);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            String formattedStartTime = startTime.format(formatter) + "+07:00";
            String formattedEndTime = now.format(formatter) + "+07:00";

            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    new AuthScope(new HttpHost("192.168.10.220", 80)),
                    new UsernamePasswordCredentials("admin", "Freedom15y".toCharArray())
            );

            CloseableHttpClient httpClient = HttpClients.custom()
                    .setDefaultCredentialsProvider(credentialsProvider)
                    .build();

            HttpComponentsClientHttpRequestFactory requestFactory =
                    new HttpComponentsClientHttpRequestFactory(httpClient);

            RestTemplate restTemplate = new RestTemplate(requestFactory);

            String requestBody = "{\n" +
                    "  \"AcsEventCond\": {\n" +
                    "    \"searchID\": \"1\",\n" +
                    "    \"searchResultPosition\": 0,\n" +
                    "    \"maxResults\": 50,\n" +
                    "    \"major\": 5,\n" +
                    "    \"minor\": 75,\n" +
                    "    \"startTime\": \"" + formattedStartTime + "\",\n" +
                    "    \"endTime\": \"" + formattedEndTime + "\"\n" +
                    "  }\n" +
                    "}";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response =
                    restTemplate.exchange(HIK_URL, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
                Map<String, Object> acsEvent = (Map<String, Object>) responseBody.get("AcsEvent");

                if (acsEvent != null && acsEvent.containsKey("InfoList")) {
                    List<Map<String, Object>> infoList =
                            (List<Map<String, Object>>) acsEvent.get("InfoList");

                    for (Map<String, Object> info : infoList) {
                        Integer minor = (Integer) info.get("minor");
                        String employeeId = (String) info.get("employeeNoString");
                        String time = (String) info.get("time");

                        if (minor != null && minor == 75 && employeeId != null && !employeeId.isEmpty()) {

                            if (!accessLogRepository.existsByEmployeeIdAndScanTime(employeeId, time)) {

                                String name = (String) info.get("name");
                                Integer doorNo = (Integer) info.get("doorNo");
                                Integer serialNo = (Integer) info.get("serialNo");
                                String verifyMode = (String) info.get("currentVerifyMode");

                                AccessLog log = new AccessLog();
                                log.setEmployeeId(employeeId);
                                log.setName(name);
                                log.setScanTime(time);
                                log.setDoorNo(doorNo);
                                log.setSerialNo(serialNo);
                                log.setVerifyMode(verifyMode);

                                accessLogRepository.save(log); // บันทึกเฉพาะข้อมูลใหม่
                                System.out.println("Saved new log for: " + name + " at " + time);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error fetching logs from Hikvision: " + e.getMessage());
        }
    }

    public List<AccessLog> getAllAccessLogs() {
        return accessLogRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    public Page<AccessLog> getAccessLogsWithPagination(int page, int size, String startDate, String endDate) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        if (startDate != null && endDate != null && !startDate.isEmpty() && !endDate.isEmpty()) {
            return accessLogRepository.findByScanTimeBetween(startDate, endDate, pageable);
        }

        return accessLogRepository.findAll(pageable);
    }
}
