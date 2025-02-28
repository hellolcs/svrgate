package com.nicednb.svrgate.repository;

import com.nicednb.svrgate.entity.OperationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface OperationHistoryRepository extends JpaRepository<OperationHistory, Long> {

    @Query("SELECT o FROM OperationHistory o " +
            "WHERE (:searchText IS NULL OR :searchText = '' OR " +
            "      (o.username LIKE CONCAT('%', :searchText, '%') " +
            "       OR o.ipAddress LIKE CONCAT('%', :searchText, '%') " +
            "       OR o.failReason LIKE CONCAT('%', :searchText, '%') " +
            "       OR o.description LIKE CONCAT('%', :searchText, '%'))) " +
            "AND (:logType IS NULL OR :logType = '' OR o.logType = :logType) " +
            "AND o.operationTime BETWEEN :startDate AND :endDate " +
            "ORDER BY o.operationTime DESC")
    Page<OperationHistory> searchOperations(@Param("searchText") String searchText,
                                            @Param("logType") String logType,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate,
                                            Pageable pageable);
}
