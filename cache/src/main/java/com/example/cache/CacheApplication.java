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

@EnableCaching
@SpringBootApplication
public class CacheApplication {

    public static void main(String[] args) {
        SpringApplication.run(CacheApplication.class, args);
    }

    @Bean
    ApplicationRunner runner(ExpensiveService expensiveService) {
        return args -> {
            var sw = new StopWatch();
            var input = Math.random() * 10000;
            time(expensiveService, sw, input);
            time(expensiveService, sw, input);
        };
    }

    private static Response time(
            ExpensiveService expensiveService,
            StopWatch sw, double input) {
        sw.start();
        var result = expensiveService.doLongRunningTaskGivenInput(input);
        sw.stop();
        System.out.println("the result is " + result + " and it took " + sw.getLastTaskTimeMillis());
        return result;
    }

}

interface ExpensiveService {
    Response doLongRunningTaskGivenInput(double d);
}

@Service
class DefaultExpensiveService implements ExpensiveService {

    @SneakyThrows
    @Cacheable("expensive")
    public Response doLongRunningTaskGivenInput(double input) {
        Thread.sleep(1000 * 10);
        return new Response("result for " + input + " is " + System.currentTimeMillis());
    }
}


@Data
@RequiredArgsConstructor
class Response implements Serializable {
    private final String message;
}
