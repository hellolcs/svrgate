package com.nicednb.svrgate.repository;

import com.nicednb.svrgate.entity.Policy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

        // 서버 ID로 정책 목록 조회
        List<Policy> findByServerObjectIdOrderByPriorityAsc(Long serverObjectId);

        // 서버 ID로 정책 목록 페이징 조회
        Page<Policy> findByServerObjectIdOrderByPriorityAsc(Long serverObjectId, Pageable pageable);

        // 검색 조건으로 정책 목록 조회 (port 필드를 startPort로 변경)
        @Query("SELECT p FROM Policy p WHERE " +
                        "(:serverId IS NULL OR p.serverObject.id = :serverId) AND " +
                        "(:searchText IS NULL OR :searchText = '' OR " +
                        "CAST(p.priority AS string) LIKE CONCAT('%', :searchText, '%') OR " +
                        "p.protocol LIKE CONCAT('%', :searchText, '%') OR " +
                        "CAST(p.startPort AS string) LIKE CONCAT('%', :searchText, '%') OR " + // port를 startPort로 변경
                        "CAST(p.endPort AS string) LIKE CONCAT('%', :searchText, '%') OR " + // endPort 검색 추가
                        "p.action LIKE CONCAT('%', :searchText, '%') OR " +
                        "p.requester LIKE CONCAT('%', :searchText, '%') OR " +
                        "p.registrar LIKE CONCAT('%', :searchText, '%') OR " +
                        "p.description LIKE CONCAT('%', :searchText, '%')) " +
                        "ORDER BY p.serverObject.id ASC, p.priority ASC")
        Page<Policy> searchPolicies(
                        @Param("serverId") Long serverId,
                        @Param("searchText") String searchText,
                        Pageable pageable);

        // 특정 서버에 속한 정책 개수 조회
        long countByServerObjectId(Long serverObjectId);

        // 만료된 정책 찾기
        @Query("SELECT p FROM Policy p WHERE p.expiresAt IS NOT NULL AND p.expiresAt <= :currentTime")
        List<Policy> findExpiredPolicies(@Param("currentTime") LocalDateTime currentTime);

        /**
         * 검색 타입과 검색어로 정책 목록 조회
         * 검색 타입에 따라 다른 필드를 검색합니다.
         */
        @Query("SELECT p FROM Policy p " +
                        "WHERE (:serverId IS NULL OR p.serverObject.id = :serverId) " +
                        "AND (" +
                        "    (:searchType IS NULL OR :searchType = '' OR :searchType = 'all') AND (" +
                        "        :searchText IS NULL OR :searchText = '' OR " +
                        "        CAST(p.priority AS string) LIKE CONCAT('%', :searchText, '%') OR " +
                        "        p.protocol LIKE CONCAT('%', :searchText, '%') OR " +
                        "        CAST(p.startPort AS string) LIKE CONCAT('%', :searchText, '%') OR " +
                        "        CAST(p.endPort AS string) LIKE CONCAT('%', :searchText, '%') OR " +
                        "        p.action LIKE CONCAT('%', :searchText, '%') OR " +
                        "        p.requester LIKE CONCAT('%', :searchText, '%') OR " +
                        "        p.registrar LIKE CONCAT('%', :searchText, '%') OR " +
                        "        p.description LIKE CONCAT('%', :searchText, '%') OR " +
                        "        p.serverObject.name LIKE CONCAT('%', :searchText, '%')" +
                        "    ) OR " +
                        "    (:searchType = 'server' AND p.serverObject.name LIKE CONCAT('%', :searchText, '%')) OR " +
                        "    (:searchType = 'priority' AND CAST(p.priority AS string) LIKE CONCAT('%', :searchText, '%')) OR "
                        +
                        "    (:searchType = 'protocol' AND p.protocol LIKE CONCAT('%', :searchText, '%')) OR " +
                        "    (:searchType = 'port' AND (CAST(p.startPort AS string) LIKE CONCAT('%', :searchText, '%') OR CAST(p.endPort AS string) LIKE CONCAT('%', :searchText, '%'))) OR "
                        +
                        "    (:searchType = 'action' AND p.action LIKE CONCAT('%', :searchText, '%')) OR " +
                        "    (:searchType = 'requester' AND p.requester LIKE CONCAT('%', :searchText, '%')) OR " +
                        "    (:searchType = 'registrar' AND p.registrar LIKE CONCAT('%', :searchText, '%')) OR " +
                        "    (:searchType = 'description' AND p.description LIKE CONCAT('%', :searchText, '%'))" +
                        ") " +
                        "ORDER BY p.serverObject.id ASC, p.priority ASC")
        Page<Policy> searchPoliciesByType(
                        @Param("serverId") Long serverId,
                        @Param("searchType") String searchType,
                        @Param("searchText") String searchText,
                        Pageable pageable);
                        
}