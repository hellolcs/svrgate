package com.nicednb.svrgate.config;

import com.nicednb.svrgate.config.security.CustomAuthFailureHandler;
import com.nicednb.svrgate.config.security.CustomDaoAuthenticationProvider;
import com.nicednb.svrgate.service.AccountService;
import com.nicednb.svrgate.service.OperationLogService;
import com.nicednb.svrgate.service.SystemSettingService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final CustomAuthFailureHandler customAuthFailureHandler;
    private final AccountService accountService;
    private final OperationLogService operationLogService;
    private final SystemSettingService systemSettingService;
    private final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public CustomDaoAuthenticationProvider customDaoAuthenticationProvider() {
        CustomDaoAuthenticationProvider provider = new CustomDaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setHideUserNotFoundExceptions(false);
        // 추가: AccountService 및 OperationLogService 주입
        provider.setAccountService(accountService);
        provider.setOperationLogService(operationLogService);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authBuilder.authenticationProvider(customDaoAuthenticationProvider());
        return authBuilder.build();
    }
    
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("SecurityFilterChain 설정 시작");
        AuthenticationManager authenticationManager = authenticationManager(http);
        http.authenticationManager(authenticationManager);

        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/bootstrap/**").permitAll()
                        .requestMatchers("/", "/account/login", "/error").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/account/login")
                        // 커스텀 실패 핸들러 적용 (한글 메시지 URL 인코딩 및 로그인 실패 기록)
                        .failureHandler(customAuthFailureHandler)
                        .defaultSuccessUrl("/dashboard", true)
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutSuccessUrl("/account/login")
                        .invalidateHttpSession(true)
                        .permitAll()
                )
                .csrf(Customizer.withDefaults())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(
                                new org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint("/account/login")
                        )
                )
                .sessionManagement(session -> {
                    // 시스템 설정의 최대 유휴시간을 초 단위로 설정
                    int maxIdleTimeSeconds = systemSettingService.getMaxIdleTime();
                    log.info("세션 최대 유휴시간 설정: {} 초", maxIdleTimeSeconds);
                    session.maximumSessions(1)
                          .expiredUrl("/account/login?expired");
                    session.invalidSessionUrl("/account/login?invalid");
                    // 세션 타임아웃 설정
                    session.sessionFixation().newSession();
                });

        return http.build();
    }
}