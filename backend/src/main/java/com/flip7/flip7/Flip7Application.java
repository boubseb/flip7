package com.flip7.flip7;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Flip7Application {

	public static void main(String[] args) {
		SpringApplication.run(Flip7Application.class, args);
	}

}
