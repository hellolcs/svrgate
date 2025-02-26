package com.nicednb.svrgate.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "login_history")
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;            // 로그인 시도한 아이디
    private String ipAddress;           // 접속 IP
    private LocalDateTime loginTime;    // 로그인 시도 시각
    private boolean success;            // 로그인 성공/실패 여부

    @Column(length = 255)
    private String failReason;          // 실패 사유 (IP 불일치, 비밀번호 틀림 등)
}
