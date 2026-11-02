package com.example.hikconnectbackend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table( name = "access_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccessLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id")
    private String employeeId;

    @Column(name = "employee_name")
    private String name;

    @Column(name = "scan_time")
    private String scanTime;

    @Column(name = "door_no")
    private Integer doorNo;

    @Column(name = "serial_no")
    private Integer serialNo;

    @Column(name = "verify_mode")
    private String verifyMode;
}
