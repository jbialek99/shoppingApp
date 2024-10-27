package com.example.shoppingapp.controller;

import com.example.shoppingapp.model.Order;
import com.example.shoppingapp.model.OrderItem;
import com.example.shoppingapp.model.Product;
import com.example.shoppingapp.model.User;
import com.example.shoppingapp.repository.OrderRepository;
import com.example.shoppingapp.repository.ProductRepository;
import com.example.shoppingapp.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.HashSet;
import java.util.Optional;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public CartController(ProductRepository productRepository, OrderRepository orderRepository, UserRepository userRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    // Endpoint do dodawania produktu do koszyka
    @PostMapping("/add/{productId}")
    public String addToCart(@PathVariable Long productId, @AuthenticationPrincipal Principal principal, HttpSession session, RedirectAttributes redirectAttributes) {
        Order order;

        if (principal != null) {
            // Zalogowany użytkownik
            User user = userRepository.findByUsername(principal.getName()).orElseThrow(() -> new RuntimeException("User not found"));
            Optional<Order> cart = orderRepository.findByUserAndStatus(user, "PENDING");

            order = cart.orElseGet(() -> {
                Order newOrder = new Order();
                newOrder.setUser(user);
                newOrder.setStatus("PENDING");
                newOrder.setTotalPrice(BigDecimal.ZERO);
                return orderRepository.save(newOrder);
            });
        } else {
            // Niezalogowany użytkownik - koszyk w sesji
            order = (Order) session.getAttribute("cart");
            if (order == null) {
                order = new Order();
                order.setOrderItems(new HashSet<>());
                order.setTotalPrice(BigDecimal.ZERO);
                session.setAttribute("cart", order);
            }
        }

        Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Product not found"));

        // Sprawdzenie dostępności produktu
        if (product.getStock() <= 0) {
            redirectAttributes.addFlashAttribute("message", "Przepraszamy, ale obecnie nie mamy tego produktu na stanie.");
            return "redirect:/home";
        }

        // Dodawanie produktu do koszyka
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setQuantity(1);
        orderItem.setPrice(product.getPrice());

        order.getOrderItems().add(orderItem);
        order.setTotalPrice(order.getTotalPrice().add(product.getPrice()));

        // Zmniejszenie stocku produktu o 1
        product.setStock(product.getStock() - 1);
        productRepository.save(product);

        if (principal != null) {
            // Zapisz koszyk w bazie dla zalogowanego użytkownika
            orderRepository.save(order);
        } else {
            // Zapisz koszyk w sesji dla niezalogowanego użytkownika
            session.setAttribute("cart", order);
        }

        redirectAttributes.addFlashAttribute("message", "Produkt został dodany do koszyka!");
        return "redirect:/home";
    }

    // Endpoint do wyświetlania koszyka
    @GetMapping
    public String viewCart(Model model, @AuthenticationPrincipal Principal principal, HttpSession session) {
        Order order;

        if (principal != null) {
            // Zalogowany użytkownik
            User user = userRepository.findByUsername(principal.getName()).orElseThrow(() -> new RuntimeException("User not found"));
            order = orderRepository.findByUserAndStatus(user, "PENDING").orElse(new Order());
        } else {
            // Niezalogowany użytkownik - koszyk w sesji
            order = (Order) session.getAttribute("cart");
            if (order == null) {
                order = new Order();
                order.setOrderItems(new HashSet<>());
                order.setTotalPrice(BigDecimal.ZERO);
                session.setAttribute("cart", order);
            }
        }

        model.addAttribute("order", order);
        return "cart";
    }
}
