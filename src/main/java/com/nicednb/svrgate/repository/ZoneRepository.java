package com.nicednb.svrgate.repository;

import com.nicednb.svrgate.entity.Zone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ZoneRepository extends JpaRepository<Zone, Long> {
    
    // Zone명으로 Zone 찾기 (중복 체크용)
    Optional<Zone> findByName(String name);
    
    // ID가 아닌 경우에 대해 Zone명으로 Zone 찾기 (수정 시 중복 체크용)
    Optional<Zone> findByNameAndIdNot(String name, Long id);
    
    // 검색 쿼리 - 정렬 기준을 ID로 변경
    @Query("SELECT z FROM Zone z WHERE " +
            "(:searchText IS NULL OR :searchText = '' OR " +
            "LOWER(z.name) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
            "z.firewallIp LIKE CONCAT('%', :searchText, '%') OR " +
            "LOWER(z.description) LIKE LOWER(CONCAT('%', :searchText, '%'))) " +
            "AND (:active IS NULL OR z.active = :active) " +
            "ORDER BY z.id ASC")
    Page<Zone> searchZones(@Param("searchText") String searchText,
                          @Param("active") Boolean active,
                          Pageable pageable);
    
    // 활성화된 모든 Zone 목록 조회 (드롭다운 선택용) - 정렬 기준 변경
    List<Zone> findByActiveOrderById(boolean active);
}