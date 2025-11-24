package com.classpathio.order.service;

import java.time.LocalDateTime;
import java.util.Set;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.classpathio.order.event.OrderEvent;
import com.classpathio.order.event.OrderStatus;
import com.classpathio.order.model.Order;
import com.classpathio.order.repository.OrderJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

	private final OrderJpaRepository orderRepository;
	private final WebClient webClient;
	private final ApplicationEventPublisher applicationEvent;
	@Value("${aws.topic-arn}")
	private String topicArn;
	private final SnsClient snsClient;
	private final ObjectMapper objectMapper = new ObjectMapper();

	//@Transactional
	//@CircuitBreaker(name = "inventoryservice", fallbackMethod = "fallback")
	public Order saveOrder(Order order) throws Exception {
		Order savedOrder = this.orderRepository.save(order);
		log.info("Calling the inventory microservice :: ");
		// make the rest call and update the inventory
		 //long orderCount = this.webClient
		 	//					 .post() 
			//					 .uri("/api/inventory")
			//					 .retrieve()
			//					 .bodyToMono(Long.class)
			//					 .block();
		long orderCount = 1000;
		log.info("Response received from inventory microservice, order count :: {}", orderCount);
		 
		// create an order event and publish to the broker
		OrderEvent orderEvent = new OrderEvent(savedOrder, OrderStatus.ORDER_ACCEPTED, LocalDateTime.now());
		
		String jsonPayload = objectMapper.writeValueAsString(orderEvent);

		snsClient.publish(PublishRequest.builder().topicArn(topicArn).message(jsonPayload).build());
		log.info("published the ordermicroservice event", orderEvent);
		return savedOrder;
	}

	private Order fallback(Order order, Throwable exception) {
		log.error("Exception while making a POST request :: {}", exception.getMessage());
		AvailabilityChangeEvent.publish(applicationEvent, "inventory-service is not operational",
				ReadinessState.REFUSING_TRAFFIC);
		return Order.builder().build();
	}

	public Set<Order> fetchOrders() {
		return Set.copyOf(orderRepository.findAll());
	}

	public Order fetchOrderById(long id) {
		return this.orderRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("invalid order id"));
	}

	public void deleteOrderById(long id) {
		this.orderRepository.deleteById(id);
	}

}
