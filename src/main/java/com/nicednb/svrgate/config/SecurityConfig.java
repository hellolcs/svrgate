package com.nicednb.svrgate.config;

import com.nicednb.svrgate.config.security.CustomAuthFailureHandler;
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
    private final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        log.info("AuthenticationManager Bean 생성");
        AuthenticationManagerBuilder authBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authBuilder
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder);
        return authBuilder.build();
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
                        // 실패 시 메시지 전달
                        .failureHandler(new CustomAuthFailureHandler("/account/login?error=true"))
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
                .csrf(Customizer.withDefaults()) // 기본 CSRF 활성화
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(
                                new org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint("/account/login")
                        )
                );

        return http.build();
    }
}
