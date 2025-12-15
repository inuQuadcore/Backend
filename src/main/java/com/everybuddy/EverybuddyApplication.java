package com.everybuddy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class EverybuddyApplication {

	public static void main(String[] args) {
		SpringApplication.run(EverybuddyApplication.class, args);
	}

}

