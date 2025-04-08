package com.nicednb.svrgate.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nicednb.svrgate.entity.ServerObject;

@Repository
public interface ServerObjectRepository extends JpaRepository<ServerObject, Long> {

        Optional<ServerObject> findByName(String name);

        Optional<ServerObject> findByNameAndIdNot(String name, Long id);

        Optional<ServerObject> findByIpAddress(String ipAddress);

        Optional<ServerObject> findByIpAddressAndIdNot(String ipAddress, Long id);

        @Query("SELECT s FROM ServerObject s WHERE " +
                        "(:searchText IS NULL OR :searchText = '' OR " +
                        "LOWER(s.name) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
                        "s.ipAddress LIKE CONCAT('%', :searchText, '%') OR " +
                        "LOWER(s.description) LIKE LOWER(CONCAT('%', :searchText, '%'))) " +
                        "AND (:active IS NULL OR s.active = :active) " +
                        "ORDER BY s.id ASC")
        Page<ServerObject> searchServerObjects(@Param("searchText") String searchText,
                        @Param("active") Boolean active,
                        Pageable pageable);

        List<ServerObject> findByActiveTrue();
}