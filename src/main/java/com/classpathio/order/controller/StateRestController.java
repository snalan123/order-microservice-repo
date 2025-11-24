package com.classpathio.order.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/state")
@RequiredArgsConstructor
public class StateRestController {
	
	private final ApplicationAvailability applicationAvailability;
	private final ApplicationEventPublisher applicationEvent;
	
	@PostMapping("/liveness")
	public Map<String, Object> livenessState(){
		LivenessState currentLivenessState = this.applicationAvailability.getLivenessState();
		LivenessState updatedLivenessState = 
				currentLivenessState == LivenessState.CORRECT ? LivenessState.BROKEN: LivenessState.CORRECT;
		
		String state = updatedLivenessState == LivenessState.CORRECT ? "System is functioning": "Application is not functioning";
		AvailabilityChangeEvent.publish(applicationEvent, state, updatedLivenessState);
		
		Map<String, Object> responseMap = new LinkedHashMap<>();
		responseMap.put("liveness", updatedLivenessState);
		responseMap.put("state", state);
		return responseMap;
	}

	@PostMapping("/readiness")
	public Map<String, Object> readinessState(){
		ReadinessState currentReadinessStateState = this.applicationAvailability.getReadinessState();
		ReadinessState updatedReadinessStateState = 
				currentReadinessStateState == ReadinessState.ACCEPTING_TRAFFIC ? ReadinessState.REFUSING_TRAFFIC: ReadinessState.ACCEPTING_TRAFFIC;
		
		String state = updatedReadinessStateState == ReadinessState.ACCEPTING_TRAFFIC ? "System is functioning": "Application is not functioning";
		
		AvailabilityChangeEvent.publish(applicationEvent, state, updatedReadinessStateState);
		
		Map<String, Object> responseMap = new LinkedHashMap<>();
		responseMap.put("readiness", updatedReadinessStateState);
		responseMap.put("state", state);
		return responseMap;

	}
	
}
