package com.example.hikconnectbackend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.file.Paths;

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

    @Column(name = "device_ip")
    private String deviceIp;

    @Column(name = "auth_type")
    private String authType;

    @Column(name = "picture_path")
    private String picturePath;

    public String getPicturePath() {
        return picturePath;
    }
    public void setPicturePath(String picturePath) {
        this.picturePath = picturePath;
    }
    @Transient
    public String getPictureUrl() {
        if (this.picturePath != null && !this.picturePath.isEmpty()) {
            String fileName = Paths.get(this.picturePath).getFileName().toString();
//base url
            return "http://localhost:9090/images/" + fileName;
        }
        return null;
    }
}
