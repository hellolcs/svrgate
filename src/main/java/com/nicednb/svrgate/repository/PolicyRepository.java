package com.nicednb.svrgate.repository;

import com.nicednb.svrgate.entity.Policy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

    // 서버 ID로 정책 목록 조회
    List<Policy> findByServerObjectIdOrderByPriorityAsc(Long serverObjectId);
    
    // 서버 ID로 정책 목록 페이징 조회
    Page<Policy> findByServerObjectIdOrderByPriorityAsc(Long serverObjectId, Pageable pageable);
    
    // 검색 조건으로 정책 목록 조회
    @Query("SELECT p FROM Policy p WHERE " +
            "(:serverId IS NULL OR p.serverObject.id = :serverId) AND " +
            "(:searchText IS NULL OR :searchText = '' OR " +
            "CAST(p.priority AS string) LIKE %:searchText% OR " +
            "p.protocol LIKE %:searchText% OR " +
            "CAST(p.port AS string) LIKE %:searchText% OR " +
            "p.action LIKE %:searchText% OR " +
            "p.requester LIKE %:searchText% OR " +
            "p.registrar LIKE %:searchText% OR " +
            "p.description LIKE %:searchText%) " +
            "ORDER BY p.serverObject.id ASC, p.priority ASC")
    Page<Policy> searchPolicies(
            @Param("serverId") Long serverId,
            @Param("searchText") String searchText,
            Pageable pageable);
    
    // 특정 서버에 속한 정책 개수 조회
    long countByServerObjectId(Long serverObjectId);
}