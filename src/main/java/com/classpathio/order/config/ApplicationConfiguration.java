package com.classpathio.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient; 
 
 
@Configuration
public class ApplicationConfiguration {
	
	@Bean 
	public WebClient webClient() {
		return WebClient
				.builder()
				//svc name should be configured 
				.baseUrl("http://inventory-microservice-svc")
				.build();
				
	}
}
