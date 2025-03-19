package com.nicednb.svrgate.repository;

import com.nicednb.svrgate.entity.GeneralObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GeneralObjectRepository extends JpaRepository<GeneralObject, Long> {
    
    Optional<GeneralObject> findByName(String name);
    
    Optional<GeneralObject> findByNameAndIdNot(String name, Long id);
    
    @Query("SELECT g FROM GeneralObject g WHERE " +
            "(:searchText IS NULL OR :searchText = '' OR " +
            "LOWER(g.name) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
            "g.ipAddress LIKE CONCAT('%', :searchText, '%') OR " +
            "LOWER(g.description) LIKE LOWER(CONCAT('%', :searchText, '%'))) " +
            "ORDER BY g.id ASC")
    Page<GeneralObject> searchGeneralObjects(@Param("searchText") String searchText, Pageable pageable);
}