package com.nicednb.svrgate.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "account")
public class Account implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;           // 아이디

    @Column(nullable = false, length = 100)
    private String password;           // 해시된 비밀번호

    @Column(nullable = false, length = 50)
    private String name;               // 사용자 이름

    @Column(length = 100)
    private String department;         // 부서명

    @Column(length = 20)
    private String phoneNumber;        // 연락처

    @Column(length = 100)
    private String email;              // 이메일

    @Column(length = 200)
    private String allowedLoginIps;    // 접속 허용 IP 목록 (쉼표 구분)

    private LocalDateTime lastLoginTime;  // 마지막 로그인 시각

    // 역할 정보: 예) "ADMIN", "USER"
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "account_roles", joinColumns = @JoinColumn(name = "account_id"))
    @Column(name = "role")
    private Set<String> roles;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        // 각 역할에 'ROLE_' 접두사를 붙여 SimpleGrantedAuthority로 변환
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toSet());
    }

    // 이하 UserDetails 필드들 true 고정(추후 계정잠금 등 필요시 로직 확장)
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
