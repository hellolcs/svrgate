package com.nicednb.svrgate.repository;

import com.nicednb.svrgate.entity.ZoneObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ZoneObjectRepository extends JpaRepository<ZoneObject, Long> {
    
    // Zone명으로 Zone 찾기 (중복 체크용)
    Optional<ZoneObject> findByName(String name);
    
    // ID가 아닌 경우에 대해 Zone명으로 Zone 찾기 (수정 시 중복 체크용)
    Optional<ZoneObject> findByNameAndIdNot(String name, Long id);
    
    // 방화벽 IP로 Zone 찾기 (중복 체크용)
    Optional<ZoneObject> findByFirewallIp(String firewallIp);
    
    // ID가 아닌 경우에 대해 방화벽 IP로 Zone 찾기 (수정 시 중복 체크용)
    Optional<ZoneObject> findByFirewallIpAndIdNot(String firewallIp, Long id);
    
    // 검색 쿼리 - ZoneObject 엔티티명으로 수정
    @Query("SELECT z FROM ZoneObject z WHERE " +
            "(:searchText IS NULL OR :searchText = '' OR " +
            "LOWER(z.name) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
            "z.firewallIp LIKE CONCAT('%', :searchText, '%') OR " +
            "LOWER(z.description) LIKE LOWER(CONCAT('%', :searchText, '%'))) " +
            "AND (:active IS NULL OR z.active = :active) " +
            "ORDER BY z.id ASC")
    Page<ZoneObject> searchZones(@Param("searchText") String searchText,
                          @Param("active") Boolean active,
                          Pageable pageable);
    
    // 모든 Zone 목록 ID 기준 정렬
    List<ZoneObject> findAllByOrderByIdAsc();
}