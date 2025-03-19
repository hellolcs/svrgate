package com.nicednb.svrgate.repository;

import com.nicednb.svrgate.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByUsername(String username);
    
    /**
     * 검색어를 포함하는 계정 목록 조회
     * username, name, department, email, phoneNumber 필드에서 검색
     * 
     * @param searchText 검색어
     * @return 검색 결과 계정 목록
     */
    @Query("SELECT a FROM Account a WHERE " +
           "LOWER(a.username) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
           "LOWER(a.name) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
           "LOWER(a.department) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
           "LOWER(a.email) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
           "a.phoneNumber LIKE CONCAT('%', :searchText, '%')")
    List<Account> searchByKeyword(@Param("searchText") String searchText);
}