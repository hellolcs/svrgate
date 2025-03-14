package com.nicednb.svrgate.repository;

import com.nicednb.svrgate.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {
    Optional<SystemSetting> findByKey(String key);
    List<SystemSetting> findByGroup(String group);
    List<SystemSetting> findAllByOrderByGroupAscKeyAsc();
}