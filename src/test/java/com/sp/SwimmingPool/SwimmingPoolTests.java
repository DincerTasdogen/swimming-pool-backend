package com.sp.SwimmingPool;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class SwimmingPoolTests {

	@Test
	void contextLoads() {
	}

//	@Test
//	public void generateEncodedPassword() {
//		PasswordEncoder encoder = new BCryptPasswordEncoder();
//		String encodedPassword = encoder.encode("admin123");
//		System.out.println("Encoded password: " + encodedPassword);
//	}
}
