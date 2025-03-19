package com.nicednb.svrgate.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "general_objects")
public class GeneralObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name; // 객체 이름

    @Column(nullable = false, length = 20)
    private String ipAddress; // IP 주소

    @Column(length = 255, columnDefinition = "VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String description; // 설명

    // Zone과의 관계 - 다대일 (일반 객체는 하나의 Zone만 선택 가능)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_object_id") // zone_id에서 zone_object_id로 변경
    private ZoneObject zoneObject; // Zone에서 ZoneObject로 타입 변경
}