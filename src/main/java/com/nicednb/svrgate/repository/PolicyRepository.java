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

        // 서버 ID로 시간제한이 없는 정책 목록 조회
        List<Policy> findByServerObjectIdAndTimeLimitIsNull(Long serverObjectId);

        // 서버 ID로 정책 목록과 구체적인 조건으로 정책 조회
        @Query("SELECT p FROM Policy p WHERE p.serverObject.id = :serverId " +
                        "AND p.timeLimit IS NULL " +
                        "AND p.sourceObjectIp = :ipAddress " +
                        "AND p.sourceObjectBit = :bit " +
                        "AND p.protocol = :protocol " +
                        "AND p.portMode = :portMode " +
                        "AND p.startPort = :startPort " +
                        "AND p.endPort = :endPort " +
                        "AND p.action = :action")
        List<Policy> findMatchingPolicies(
                        @Param("serverId") Long serverId,
                        @Param("ipAddress") String ipAddress,
                        @Param("bit") Integer bit,
                        @Param("protocol") String protocol,
                        @Param("portMode") String portMode,
                        @Param("startPort") Integer startPort,
                        @Param("endPort") Integer endPort,
                        @Param("action") String action);
}