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
@Table(name = "network_objects")
public class NetworkObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name; // 네트워크 이름

    @Column(nullable = false, length = 50)
    private String ipAddress; // IP 주소

    @Column(length = 255, columnDefinition = "VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String description; // 설명

    // Zone과의 관계 - 다대다
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "network_object_zones", joinColumns = @JoinColumn(name = "network_object_id"), inverseJoinColumns = @JoinColumn(name = "zone_object_id"))
    private Set<ZoneObject> zones = new HashSet<>();

    /**
     * 연결된 Zone 이름들을 콤마로 구분하여 문자열로 반환
     */
    public String getZoneNames() {
        if (zones == null || zones.isEmpty()) {
            return "";
        }
        return zones.stream()
                .map(ZoneObject::getName) // Zone에서 ZoneObject로 변경
                .sorted()
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }
}