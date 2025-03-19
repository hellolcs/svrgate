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
    
    // 검색 쿼리
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
    
    // 모든 Zone을 ID 기준 오름차순으로 정렬하여 조회
    List<Zone> findAllByOrderByIdAsc();
}