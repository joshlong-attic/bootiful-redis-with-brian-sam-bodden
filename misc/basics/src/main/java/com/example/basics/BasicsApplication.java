package com.example.basics;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

@SpringBootApplication
public class BasicsApplication {

	public static void main(String[] args) {
		SpringApplication.run(BasicsApplication.class, args);
	}

	@Bean
	ApplicationRunner runner(ReactiveRedisTemplate<String, String> template) {
		return event -> {
			var listName = "spring-team";
			var operations = template.opsForList();
			var push = operations.leftPushAll(listName,
				"Madhura", "Stéphane", "Dr. Syer", "Spencer",
				"Olga", "Andy", "Yuxin", "Costin", "Violetta"
			);
			push
				.thenMany(push)
				.thenMany(operations.leftPop(listName))
				.thenMany(operations.leftPop(listName))
				.subscribe(System.out::println);
		};
	}
}
