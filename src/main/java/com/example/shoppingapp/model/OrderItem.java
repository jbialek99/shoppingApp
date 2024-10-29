package com.example.shoppingapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private BigDecimal price; // Cena jednostkowa produktu

    @Column(nullable = false)
    private BigDecimal totalItemPrice; // Łączna cena za dany produkt (quantity * price)

    // Konstruktor bezargumentowy
    public OrderItem() {
    }

    // Gettery i settery
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
        // Upewniamy się, że cena jednostkowa jest ustawiona zgodnie z produktem
        this.price = product.getPrice();
        // Aktualizujemy całkowitą cenę
        updateTotalItemPrice();
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        // Aktualizujemy całkowitą cenę, gdy zmienia się ilość
        updateTotalItemPrice();
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
        // Aktualizujemy całkowitą cenę, gdy zmienia się cena jednostkowa
        updateTotalItemPrice();
    }

    public BigDecimal getTotalItemPrice() {
        return totalItemPrice;
    }

    public void setTotalItemPrice(BigDecimal totalItemPrice) {
        this.totalItemPrice = totalItemPrice;
    }

    // Metoda pomocnicza do aktualizacji totalItemPrice
    private void updateTotalItemPrice() {
        if (this.price != null) {
            this.totalItemPrice = this.price.multiply(BigDecimal.valueOf(this.quantity));
        } else {
            this.totalItemPrice = BigDecimal.ZERO;
        }
    }
}
