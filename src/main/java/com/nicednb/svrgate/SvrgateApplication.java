package com.nicednb.svrgate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean; // Bean annotation 추가
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // BCryptPasswordEncoder import
import org.springframework.security.crypto.password.PasswordEncoder; // PasswordEncoder import

@SpringBootApplication
public class SvrgateApplication {

	public static void main(String[] args) {
		SpringApplication.run(SvrgateApplication.class, args);
	}

	@Bean // PasswordEncoder Bean 정의를 SvrgateApplication 으로 이동
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}