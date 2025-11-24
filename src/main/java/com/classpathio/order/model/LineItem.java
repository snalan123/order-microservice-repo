package com.classpathio.order.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name="line_items")

@Data
@EqualsAndHashCode(exclude = "order")
@ToString(exclude = "order")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LineItem {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id;
	private String name;
	private int qty;
	private double price;
	
	@ManyToOne
	@JoinColumn(name="order_id", nullable = false)
	@JsonBackReference
	private Order order;
}
