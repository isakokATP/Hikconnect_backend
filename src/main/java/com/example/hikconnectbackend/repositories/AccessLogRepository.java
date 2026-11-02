package com.example.hikconnectbackend.repositories;

import com.example.hikconnectbackend.entities.AccessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {
    Page<AccessLog> findByScanTimeBetween(String startDate, String endDate, Pageable pageable);
    boolean existsByEmployeeIdAndScanTime(String employeeId, String scanTime);
}
