package com.example.basics;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.ReactiveGeoOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Flux;

import java.util.Map;

@SpringBootApplication
public class BasicsApplication {

    public static void main(String[] args) {
        SpringApplication.run(BasicsApplication.class, args);
    }


    @Bean
    ReactiveGeoOperations<String, String> geoOperations(ReactiveRedisTemplate<String, String> reactiveRedisTemplate) {
        return reactiveRedisTemplate.opsForGeo();
    }

    @Bean
    ApplicationRunner geography(ReactiveGeoOperations<String, String> geographyTemplate) {
        return args -> {
            var sicily = "Sicily";
            var mapOfPoints = Map.of(
                    "Arigento", new Point(13.361389, 38.1155556),
                    "Catania", new Point(15.087269, 37.502669),
                    "Palermo", new Point(13.583333, 37.316667)
            );
            Flux
                    .fromIterable(mapOfPoints.entrySet())
                    .flatMap(e -> geographyTemplate.add(sicily, e.getValue(), e.getKey()))
                    .thenMany(geographyTemplate.radius(sicily, new Circle(
                                    new Point(13.583333, 37.316667),
                                    new Distance(10, RedisGeoCommands.DistanceUnit.KILOMETERS)
                            ))
                    )
                    .map(GeoResult::getContent)
                    .map(RedisGeoCommands.GeoLocation::getName)
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
