package com.classpathio.order.config;

import java.time.ZoneId;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import com.classpathio.order.model.LineItem;
import com.classpathio.order.model.Order;
import com.classpathio.order.repository.OrderJpaRepository;
import net.datafaker.Faker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Configuration
@Slf4j
@RequiredArgsConstructor
public class BootstrapAppConfig {
	
	private final OrderJpaRepository orderRepository;
	private final Faker faker = new Faker();
	
	private int orderCount = 10;
	
	@EventListener(ApplicationReadyEvent.class)
	public void onReady(ApplicationReadyEvent event) {
		log.info("On Application ready event");
		
		IntStream.range(0, orderCount).forEach(index -> {
			String firstName = faker.name().firstName();
			Order order = Order.builder()
								.email(firstName+"@"+faker.internet().domainName())
								.date(faker.date().past(4, TimeUnit.DAYS).toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
								.name(firstName)
								.build();
			
			IntStream.range(0, faker.number().numberBetween(1, 3)).forEach(value -> {
				//LineItem lineItem = new LineItem("abc", 12, 34.5, true, false, "Ramesh", "Arvind");
				LineItem lineItem = LineItem.builder()
										.name(faker.commerce().productName())
										.price(faker.number().randomDouble(2, 400, 600))
										.qty(faker.number().numberBetween(2, 4))
										.build();
				order.addLineItem(lineItem);
			});
			double totalOrderPrice = order
										.getLineItems()
										.stream()
										.map(lineItem -> lineItem.getQty() * lineItem.getPrice())
										.reduce(Double::sum)
										.orElse(0d);
			order.setPrice(totalOrderPrice);
			this.orderRepository.save(order);
		});
	}
}
