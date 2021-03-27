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
import java.util.*;

@SpringBootApplication
public class RepositoriesApplication {

	public static void main(String[] args) {
		SpringApplication.run(RepositoriesApplication.class, args);
	}

	private Long generateId() {
		long tmp = new Random().nextLong();
		return Math.max(tmp, tmp * -1);
	}

	@Bean
	ApplicationRunner repositories(
		OrderRepository orderRepository,
		LineItemRepository lineItemRepository) {

		return rgs -> {

			Long orderId = generateId();

			List<LineItem> itemList = Arrays.asList(
				new LineItem(orderId, generateId(), "plunger"),
				new LineItem(orderId, generateId(), "soup"),
				new LineItem(orderId, generateId(), "coffee mug"));
			itemList
				.stream()
				.map(lineItemRepository::save)
				.forEach(li -> System.out.println( li.toString()));

			Order order = new Order(orderId, new Date(), itemList);
			orderRepository.save(order);

			Collection<Order> found = orderRepository.findByWhen(order.getWhen());
			found.forEach(o -> System.out.println( "found: " + o));
		};
	}

}


interface OrderRepository extends CrudRepository<Order, Long> {
	Collection<Order> findByWhen(Date d);
}


interface LineItemRepository extends CrudRepository<LineItem, Long> {
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

@RedisHash("lineItems")
@Data
@AllArgsConstructor
@NoArgsConstructor
class LineItem implements Serializable {

	@Indexed
	private Long orderId;

	@Id
	private Long id;

	private String description;
}


