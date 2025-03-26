package com.nicednb.svrgate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableScheduling  // 추가
public class SvrgateApplication {

	public static void main(String[] args) {
		SpringApplication.run(SvrgateApplication.class, args);
	}

	@Bean // PasswordEncoder Bean 정의를 SvrgateApplication 으로 이동
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
