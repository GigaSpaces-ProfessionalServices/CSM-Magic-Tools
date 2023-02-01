package com.web.dihx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class DihxApplication {

	public static void main(String[] args) {
		SpringApplication.run(DihxApplication.class, args);
	}

}
