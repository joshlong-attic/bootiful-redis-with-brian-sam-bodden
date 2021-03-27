package com.example.basics;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Flux;

import java.util.Map;

// todo introduce geography!!
@SpringBootApplication
public class BasicsApplication {

    public static void main(String[] args) {
        SpringApplication.run(BasicsApplication.class, args);
    }


    @Bean
    ApplicationRunner geography(ReactiveRedisTemplate<String, String> template) {
        return args -> {
            var sicily = "Sicily";
            var geographyTemplate = template.opsForGeo();

            var mapOfPoints = Map.of(
                    "Arigento", new Point(13.361389, 38.1155556),
                    "Catania", new Point(15.087269, 37.502669),
                    "Palermo", new Point(13.583333, 37.316667)
            );
            mapOfPoints.forEach((k, v) -> geographyTemplate.add(sicily, v, k).block());
            var circle = new Circle(
                    new Point(13.583333, 37.316667),
                    new Distance(10, RedisGeoCommands.DistanceUnit.KILOMETERS)
            );
            geographyTemplate
                    .radius(sicily, circle)
                    .map(GeoResult::getContent)
                    .doOnNext(System.out::println)
                    .subscribe();


        };
    }

    @Bean
    ApplicationRunner list(ReactiveRedisTemplate<String, String> template) {
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
                    .doOnNext(s -> System.out.println("got " + s))
                    .thenMany(operations.leftPop(listName))
                    .doOnNext(s -> System.out.println("got " + s))
                    .subscribe();
        };
    }
}
