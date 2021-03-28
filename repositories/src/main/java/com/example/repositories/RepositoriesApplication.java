package com.example.repositories;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Reference;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;
import org.springframework.data.repository.CrudRepository;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Random;

@SpringBootApplication
public class RepositoriesApplication {

    public static void main(String[] args) {
        SpringApplication.run(RepositoriesApplication.class, args);
    }


    private static Long generateId() {
        var tmp = new Random()
                .nextLong();
        return Math.max(tmp, tmp * -1);
    }

    @Bean
    ApplicationRunner runner(OrderRepository or, LineItemRepository lir) {
        return args -> {

            var orderId = generateId();
            var items = List.of(
                    new LineItem(orderId, generateId(), "plunger"),
                    new LineItem(orderId, generateId(), "soup"),
                    new LineItem(orderId, generateId(), "coffee mug"));
            items.stream().map(lir::save).forEach(System.out::println);

            var order = new Order(orderId, new Date(), items);
            or.save(order);

            var results = or.findByWhen(order.getWhen());
            results.forEach(System.out::println);

        };
    }

}

interface OrderRepository extends CrudRepository<Order, Long> {

    Collection<Order> findByWhen(Date d);
}

interface LineItemRepository extends CrudRepository<LineItem, Long> {
}

@RedisHash("lineItems")
@Data
@AllArgsConstructor
@NoArgsConstructor
class LineItem implements Serializable {


    @Id
    private Long id;

    @Indexed
    private Long orderId;

    private String description;

}

@Data
@AllArgsConstructor
@NoArgsConstructor
@RedisHash("orders")
class Order implements Serializable {

    @Id
    private Long id;

    @Indexed
    private Date when;

    @Reference
    private List<LineItem> lineItems;


}