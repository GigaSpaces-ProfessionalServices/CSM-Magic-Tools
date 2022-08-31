package com.gigaspaces.retentionmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;



@SpringBootApplication(exclude = {HibernateJpaAutoConfiguration.class, SecurityAutoConfiguration.class})
@EnableScheduling
public class RetentionManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(RetentionManagerApplication.class, args);
	}

}
