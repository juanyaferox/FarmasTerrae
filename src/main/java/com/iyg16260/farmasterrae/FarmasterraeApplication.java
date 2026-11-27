package com.iyg16260.farmasterrae;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FarmasterraeApplication {

	public static void main(String[] args) {
		SpringApplication.run(FarmasterraeApplication.class, args);
	}

}
