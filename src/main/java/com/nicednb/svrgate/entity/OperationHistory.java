package com.nicednb.svrgate.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "operation_history")
public class OperationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;            // 작업을 수행한 사용자

    private String ipAddress;           // 접속 IP

    private LocalDateTime operationTime;    // 작업 수행 시각

    private boolean success;            // 작업 성공/실패 여부

    // UTF-8(utf8mb4) 지원을 위해
    @Column(length = 255, columnDefinition = "VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String failReason;          // 실패 사유

    // 작업 유형: "로그인", "정책관리", "객체관리", "설정" 등
    @Column(length = 50, columnDefinition = "VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String logType;

    // 추가 설명 (옵션)
    @Column(length = 500, columnDefinition = "VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String description;
}
