package com.nicednb.svrgate.repository;

import com.nicednb.svrgate.entity.NetworkObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NetworkObjectRepository extends JpaRepository<NetworkObject, Long> {
    
    Optional<NetworkObject> findByName(String name);
    
    Optional<NetworkObject> findByNameAndIdNot(String name, Long id);
    
    // IP로 객체 찾기 (중복 체크용)
    Optional<NetworkObject> findByIpAddress(String ipAddress);
    
    // ID가 아닌 경우에 대해 IP로 객체 찾기 (수정 시 중복 체크용)
    Optional<NetworkObject> findByIpAddressAndIdNot(String ipAddress, Long id);
    
    @Query("SELECT n FROM NetworkObject n WHERE " +
            "(:searchText IS NULL OR :searchText = '' OR " +
            "LOWER(n.name) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
            "n.ipAddress LIKE CONCAT('%', :searchText, '%') OR " +
            "LOWER(n.description) LIKE LOWER(CONCAT('%', :searchText, '%'))) " +
            "ORDER BY n.id ASC")
    Page<NetworkObject> searchNetworkObjects(@Param("searchText") String searchText, Pageable pageable);
}