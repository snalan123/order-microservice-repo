package com.classpathio.order.model;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name="orders")
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Order {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	private String name;
	private String email;
	private double price;
	private LocalDate date;
	
	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JsonManagedReference
	private Set<LineItem> lineItems;
	
	// Set the bidirectional relatioship
	// To add the lineitems to the order && set the order for the lineItem
	public void addLineItem(LineItem lineItem) {
		if(this.lineItems == null) {
			this.lineItems = new HashSet<>();
		}
		this.lineItems.add(lineItem);
		lineItem.setOrder(this);
		
	}

}
