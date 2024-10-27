package com.example.shoppingapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;

@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username is mandatory")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    @Column(nullable = false, unique = true)
    private String username;

    @NotBlank(message = "Password is mandatory")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Column(nullable = false)
    private String password;

    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is mandatory")
    @Column(nullable = false, unique = true)
    private String email;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Order> orders;

    // Konstruktor bezargumentowy (wymagany przez JPA)
    public User() {}

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<Order> getOrders() {
        return orders;
    }

    public void setOrders(Set<Order> orders) {
        this.orders = orders;
    }

    // Implementacje metod z interfejsu UserDetails

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Możesz zwrócić listę ról (na przykład), na razie pusta implementacja
        return null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Zawsze zwracaj true, chyba że dodasz obsługę wygasania kont
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Zawsze zwracaj true, chyba że dodasz blokowanie kont
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Zawsze zwracaj true, chyba że dodasz obsługę wygasania poświadczeń
    }

    @Override
    public boolean isEnabled() {
        return true; // Zwróć true, chyba że chcesz dodawać logikę aktywacji konta
    }
}
