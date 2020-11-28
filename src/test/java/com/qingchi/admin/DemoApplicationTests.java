package com.qingchi.admin;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@EntityScan("com.qingchi")
@EnableJpaRepositories("com.qingchi")
@ComponentScan(value = "com.qingchi")
public class DemoApplicationTests {

	@Test
	public void contextLoads() {
	}

}
