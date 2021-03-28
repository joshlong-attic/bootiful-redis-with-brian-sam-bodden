package com.example.cache;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.io.Serializable;
import java.time.Instant;

@EnableCaching
@SpringBootApplication
public class CacheApplication {

    public static void main(String[] args) {
        SpringApplication.run(CacheApplication.class, args);
    }

    @Bean
    ApplicationRunner applicationRunner(ExpensiveService es) {
        return event -> {

            var sw = new StopWatch();
            var input = 42;
            time(es, sw, input);
            time(es, sw, input);

        };
    }

    private static Response time(ExpensiveService es,
                                 StopWatch sw, double input) {
        sw.start();
        Response response = es.performExpensiveCalculation(input);
        sw.stop();
        System.out.println("got response " + response.toString() + " after " + sw.getLastTaskTimeMillis());
        return response;
    }

}


@Service
class ExpensiveService {

    @SneakyThrows
    @Cacheable("expensive")
    public Response performExpensiveCalculation(double input) {
        Thread.sleep(10 * 1000);
        return new Response("response from input " + input + " @ " + Instant.now());
    }

}

@Data
@RequiredArgsConstructor
class Response implements Serializable {
    private final String message;
}