package com.nicednb.svrgate.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "server_objects")
public class ServerObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name; // 서버 이름

    @Column(nullable = false, length = 20)
    private String ipAddress; // IP 주소

    @Column(nullable = false)
    private boolean active = true; // 연동여부
    
    @Column(length = 255)
    private String apiKey; // API Key - 연동서버 Agent와 통신 시 인증에 사용

    @Column(length = 255, columnDefinition = "VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String description; // 설명

    // Zone과의 관계 - 다대일 (서버 객체는 하나의 Zone만 선택 가능)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_object_id")
    private ZoneObject zoneObject;

    // 마지막 연동 시각
    @Column
    private java.time.LocalDateTime lastSyncTime;
}