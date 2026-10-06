package com.hrbotproject.hr_bot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

   @SpringBootTest(properties = "app.jwt.secret=test-only-secret-used-by-unit-tests-never-in-production")class HrBotApplicationTests {

	@Test
	void contextLoads() {
	}

}
