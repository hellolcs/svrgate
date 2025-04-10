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
@Table(name = "policies")
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer priority; // 우선순위

    // 출발지 객체 (연동서버, 일반, 네트워크 객체 중 하나)
    @Column(nullable = false)
    private Long sourceObjectId; // 출발지 객체 ID
    
    @Column(nullable = false, length = 20)
    private String sourceObjectType; // 출발지 객체 유형 (SERVER, GENERAL, NETWORK)
    
    // 방화벽 정책 매핑을 위한 추가 필드
    @Column(length = 50)
    private String sourceObjectIp;  // 출발지 IP

    @Column
    private Integer sourceObjectBit;  // 출발지 넷마스크 비트

    @Column(nullable = false, length = 10)
    private String protocol; // 프로토콜 (tcp, udp)

    @Column(nullable = false, length = 10)
    private String portMode; // 포트 모드 (single, multi)

    @Column(nullable = false)
    private Integer startPort; // 시작 포트
    
    @Column
    private Integer endPort; // 끝 포트 (portMode가 multi인 경우에만 사용)

    @Column(nullable = false, length = 10)
    private String action; // 동작 (accept, reject)

    @Column
    private Integer timeLimit; // 시간제한(h)

    @Column(nullable = false)
    private Boolean logging; // 로깅 (true: 사용, false: 미사용)

    @Column(nullable = false)
    private LocalDateTime registrationDate; // 등록일

    @Column
    private LocalDateTime expiresAt; // 정책 만료 시간

    @Column(length = 32, columnDefinition = "VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String requester; // 요청자

    @Column(nullable = false, length = 32, columnDefinition = "VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String registrar; // 등록자 (계정 Name)

    @Column(length = 255, columnDefinition = "VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String description; // 설명

    // 어떤 서버에 속한 정책인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_object_id")
    private ServerObject serverObject;

    // 추가 생성 시각 및 수정 시각
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;
    
    // 낙관적 락을 위한 버전 필드
    @Version
    @Column(columnDefinition = "BIGINT DEFAULT 0")
    private Long version = 0L; // 기본값 0으로 초기화

    // 생성 전 이벤트
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.registrationDate == null) {
            this.registrationDate = this.createdAt;
        }
        
        // 시간제한이 설정된 경우 만료 시간 계산
        if (this.timeLimit != null && this.timeLimit > 0) {
            this.expiresAt = this.createdAt.plusHours(this.timeLimit);
        }
        
        // 버전 필드 초기화 (null인 경우)
        if (this.version == null) {
            this.version = 0L;
        }
    }

    // 수정 전 이벤트
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        
        // 시간제한이 변경된 경우 만료 시간 재계산
        if (this.timeLimit != null && this.timeLimit > 0) {
            // 등록일 기준으로 만료 시간 계산
            this.expiresAt = this.registrationDate.plusHours(this.timeLimit);
        } else {
            this.expiresAt = null; // 시간제한이 없는 경우 만료 시간을 null로 설정
        }
        
        // 버전 필드 초기화 (null인 경우)
        if (this.version == null) {
            this.version = 0L;
        }
    }
}