package com.classpathio.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.classpathio.order.model.Order;

@Repository
public interface OrderJpaRepository extends JpaRepository<Order, Long>{

}
