package com.nicednb.svrgate.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "zones")
public class Zone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name; // Zone명 (영문만 입력 가능, Unique)

    @Column(nullable = false, length = 15)
    private String firewallIp; // 방화벽 IP (IPv4 형식)

    // 비보안Zone - 다대다 관계
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "zone_nonsecure_mappings",
        joinColumns = @JoinColumn(name = "zone_id"),
        inverseJoinColumns = @JoinColumn(name = "nonsecure_zone_id")
    )
    private Set<Zone> nonSecureZones = new HashSet<>();

    // 보안Zone - 다대다 관계
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "zone_secure_mappings",
        joinColumns = @JoinColumn(name = "zone_id"),
        inverseJoinColumns = @JoinColumn(name = "secure_zone_id")
    )
    private Set<Zone> secureZones = new HashSet<>();

    @Column(nullable = false)
    private boolean active = true; // 연동여부(변경됨)

    @Column(length = 255, columnDefinition = "VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String description; // 설명 (utf8mb4 지원 추가)
    
    // 선택된 Zone들의 ID 목록을 문자열로 반환 (UI 표시용)
    public String getNonSecureZoneNames() {
        if (nonSecureZones == null || nonSecureZones.isEmpty()) {
            return "";
        }
        return nonSecureZones.stream()
                .map(Zone::getName)
                .sorted()
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }
    
    // 선택된 Zone들의 ID 목록을 문자열로 반환 (UI 표시용)
    public String getSecureZoneNames() {
        if (secureZones == null || secureZones.isEmpty()) {
            return "";
        }
        return secureZones.stream()
                .map(Zone::getName)
                .sorted()
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }
}