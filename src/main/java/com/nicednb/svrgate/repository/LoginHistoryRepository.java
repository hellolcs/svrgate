package com.nicednb.svrgate.repository;

import com.nicednb.svrgate.entity.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
    // 필요 시 로그인 기록 검색 메서드 추가 가능
}
