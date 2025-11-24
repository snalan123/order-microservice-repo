package com.classpathio.order.util;

import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import lombok.extern.slf4j.Slf4j;


@Configuration
@Slf4j
public class StateChangeHandler {
	
	@EventListener(AvailabilityChangeEvent.class)
	public void livenessStateHandler(AvailabilityChangeEvent<LivenessState> event) {
		log.info("Liveness:: Application change event :: {}", event.getState());
	}
}