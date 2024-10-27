package com.example.shoppingapp.repository;

import com.example.shoppingapp.model.Order;
import com.example.shoppingapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByUserAndStatus(User user, String status); // Dodana metoda
}
