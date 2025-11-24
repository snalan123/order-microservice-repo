package com.classpathio.order.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.classpathio.order.model.Order;
import com.classpathio.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import java.util.*;


@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderRestController {
	
	private final OrderService orderService;
	 private static final Logger logger = LoggerFactory.getLogger(OrderRestController.class);

	@PreAuthorize("hasAuthority('SCOPE_orders.reader') and hasAnyRole('CUSTOMER','SELLER','ADMIN')")
	@GetMapping
	public Set<Order> fetchAllOrders(){
		String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
		Set<Order> orders = this.orderService.fetchOrders();
		logger.info("Successfully retrieved {} orders", orders.size());
		logger.info("Successfully retrieved {} orders", orders.size());
		logger.info("Successfully retrieved {} orders", orders.size());
		logger.info("Successfully retrieved {} orders", orders.size());
		logger.info("Successfully retrieved {} orders", orders.size());
        MDC.clear();
		logger.info("cleared the MDC");
		return orders;
	}

	@PreAuthorize("hasAuthority('SCOPE_orders.reader') and hasAnyRole('SELLER','ADMIN')")
	@GetMapping("/{id}")
	public Order fetchOrderById(@PathVariable("id") long id) {
		String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("orderId", id+"");
        
        logger.info("Fetching order details for orderid: {}", id);
		logger.info("Successfully retrieved order details for: {}", id);
        MDC.clear();
		return this.orderService.fetchOrderById(id);
	}
	
	@PreAuthorize("hasAuthority('SCOPE_orders.create') and hasAnyRole('SELLER','ADMIN')")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Order saveOrder(@RequestBody Order order) throws Exception{
		return this.orderService.saveOrder(order);
	}
	
	@PreAuthorize("hasAuthority('SCOPE_orders.delete') and hasRole('ADMIN')")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteOrderById(@PathVariable long id) {
		this.orderService.deleteOrderById(id);
	}

}
