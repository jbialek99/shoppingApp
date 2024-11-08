package com.example.shoppingapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable; // Importowanie Serializable
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.io.Serial; // Importowanie Serial

@Entity
@Table(name = "users")
public class User implements UserDetails, Serializable {

    @Serial // To wskazuje, że to pole jest przeznaczone do serializacji
    private static final long serialVersionUID = 1L; // serialVersionUID dla kontroli wersji

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username is mandatory", groups = ValidationGroups.Registration.class)
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters", groups = ValidationGroups.Registration.class)
    @Column(nullable = false, unique = true)
    private String username;

    @NotBlank(message = "Password is mandatory", groups = ValidationGroups.Registration.class)
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
            message = "Hasło musi mieć min. 8 znaków, zawierać dużą literę i cyfrę",
            groups = ValidationGroups.Registration.class
    )
    @Column(nullable = false)
    private String password;

    @Email(message = "Email should be valid", groups = ValidationGroups.Registration.class)
    @NotBlank(message = "Email is mandatory", groups = ValidationGroups.Registration.class)
    @Column(nullable = false, unique = true)
    private String email;

    @Pattern(regexp = "^[^0-9]*$", message = "Imię nie może zawierać cyfr", groups = ValidationGroups.Update.class)
    private String firstName;

    @Pattern(regexp = "^[^0-9]*$", message = "Nazwisko nie może zawierać cyfr", groups = ValidationGroups.Update.class)
    private String lastName;

    @Pattern(regexp = "^[0-9]{9}$", message = "Podaj poprawny nr telefonu", groups = ValidationGroups.Update.class)
    private String phone;

    @NotBlank(message = "Email nie moze byc pusty", groups = ValidationGroups.Update.class)
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Podaj poprawny adres", groups = ValidationGroups.Update.class)
    private String address;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Order> orders = new HashSet<>();

    public User() {
    }

    // Gettery i settery
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

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Set<Order> getOrders() {
        return orders;
    }

    public void setOrders(Set<Order> orders) {
        this.orders = orders;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Przykładowa rola, możesz rozszerzyć ją o więcej ról
        return Set.of(() -> "ROLE_USER");
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}