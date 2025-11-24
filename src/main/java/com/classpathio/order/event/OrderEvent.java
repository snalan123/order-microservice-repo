package com.classpathio.order.event; 

import java.time.LocalDateTime;

import com.classpathio.order.model.Order;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class OrderEvent {

	private Order order;
	private OrderStatus status;
	private LocalDateTime timestamp;

}
