package redis;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Reference;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.index.Indexed;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.repository.CrudRepository;
import org.springframework.nativex.hint.ProxyHint;
import org.springframework.nativex.hint.TypeHint;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

//@RequiredArgsConstructor
@ProxyHint(types = {
	OrderService.class,
	org.springframework.aop.SpringProxy.class,
	org.springframework.aop.framework.Advised.class,
	org.springframework.core.DecoratingProxy.class
})
@ProxyHint(types = {
	io.lettuce.core.api.sync.RedisCommands.class,
	io.lettuce.core.cluster.api.sync.RedisClusterCommands.class
})
@EnableCaching
//@EnableRedisHttpSession
@SpringBootApplication
public class RedisApplication {
//
//	private final Foo foo ;

	private final String topic = "chat";

	private ApplicationRunner titledRunner(String title, ApplicationRunner rr) {
		return args -> {
			System.out.println(title.toUpperCase() + ":");
			rr.run(args);
		};
	}

	@Bean
	CacheManager redisCache(RedisConnectionFactory cf) {
		return RedisCacheManager
			.builder(cf)
			.build();
	}

	@Bean
	ApplicationRunner geography(RedisTemplate<String, String> rt) {
		return titledRunner("geography", args -> {

			GeoOperations<String, String> geo = rt.opsForGeo();
			geo.add("Sicily", new Point(13.361389, 38.1155556), "Arigento");
			geo.add("Sicily", new Point(15.087269, 37.502669), "Catania");
			geo.add("Sicily", new Point(13.583333, 37.316667), "Palermo");

			Circle circle = new Circle(new Point(13.583333, 37.316667),
				new Distance(100, org.springframework.data.redis.connection.RedisGeoCommands.DistanceUnit.KILOMETERS));

			GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults = geo.radius("Sicily", circle);
			geoResults
				.getContent()
				.forEach(c -> System.out.println(c.toString()));
		});
	}

/*
	@Bean
	ApplicationRunner pubSub(RedisTemplate<String, String> rt) {
		return titledRunner("publish/subscribe", args -> {
			rt.convertAndSend(topic, "Hello, world @ " + Instant.now().toString());
		});
	}
*/

	@Bean
	ApplicationRunner repositories(
		OrderRepository orderRepository,
		LineItemRepository lineItemRepository) {

		return titledRunner("repositories", args -> {

			Long orderId = generateId();

			List<LineItem> itemList = Arrays.asList(
				new LineItem(orderId, generateId(), "plunger"),
				new LineItem(orderId, generateId(), "soup"),
				new LineItem(orderId, generateId(), "coffee mug"));
			itemList
				.stream()
				.map(lineItemRepository::save)
				.forEach(li -> System.out.println(li.toString()));

			Order order = new Order(orderId, new Date(), itemList);
			orderRepository.save(order);

			Collection<Order> found = orderRepository.findByWhen(order.getWhen());
			found.forEach(o -> System.out.println("found: " + o.toString()));
		});
	}

/*
	@Bean
	RedisMessageListenerContainer listener(RedisConnectionFactory cf) {
		MessageListener ml = (message, pattern) -> {
			String str = new String(message.getBody());
			System.out.println("message from '" + topic + "': " + str);
		};
		RedisMessageListenerContainer mlc = new RedisMessageListenerContainer();
		mlc.addMessageListener(ml, new PatternTopic(this.topic));
		mlc.setConnectionFactory(cf);
		return mlc;
	}
*/

	private Long generateId() {
		long tmp = new Random().nextLong();
		return Math.max(tmp, tmp * -1);
	}

	private long measure(Runnable r) {
		long start = System.currentTimeMillis();
		r.run();
		long stop = System.currentTimeMillis();
		return stop - start;
	}

	@Bean
	ApplicationRunner cache(OrderService orderService) {
		return titledRunner("caching", a -> {
			Runnable measure = () -> orderService.byId(1L);
			System.out.println("first " + measure(measure));
			System.out.println("two " + measure(measure));
			System.out.println("three " + measure(measure));
		});
	}

	public static void main(String[] args) {
		SpringApplication.run(RedisApplication.class, args);
	}
}

interface OrderService {


	Order byId(Long id);
}

@Service
class SimpleOrderService implements OrderService {

	@Cacheable("order-by-id")
	public Order byId(Long id) {
		//@formatter:off
		try {
			Thread.sleep(1000 * 10);
		}
		catch (Throwable e) {
			throw new RuntimeException(e);
		}
		//@formatter:on
		return new Order(id, new Date(), Collections.emptyList());
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

/*
class ShoppingCart implements Serializable {

	private final Collection<Order> orders = new ArrayList<>();

	public void addOrder(Order order) {
		this.orders.add(order);
	}

	public Collection<Order> getOrders() {
		return this.orders;
	}
}

@Controller
@SessionAttributes("cart")
class CartSessionController {

	private final AtomicLong ids = new AtomicLong();

	@ModelAttribute("cart")
	ShoppingCart cart() {
		System.out.println("creating new cart");
		return new ShoppingCart();
	}

	@GetMapping("/orders")
	String orders(@ModelAttribute("cart") ShoppingCart cart,
															Model model) {
		cart.addOrder(new Order(ids.incrementAndGet(), new Date(), Collections.emptyList()));
		model.addAttribute("orders", cart.getOrders());
		return "orders";
	}
}*/
