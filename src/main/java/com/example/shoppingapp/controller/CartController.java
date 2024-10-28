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
import org.springframework.transaction.annotation.Transactional;
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

    // Endpoint do wyświetlania koszyka
    @GetMapping
    public String viewCart(Model model, HttpSession session, @AuthenticationPrincipal Principal principal) {
        Order order;

        if (principal != null) {
            User user = userRepository.findByUsername(principal.getName()).orElseThrow(() -> new RuntimeException("User not found"));
            order = orderRepository.findByUserAndStatus(user, "PENDING").orElse(new Order());
            if (order.getOrderItems() == null) {
                order.setOrderItems(new HashSet<>());
            }
        } else {
            order = (Order) session.getAttribute("cart");
            if (order == null) {
                order = new Order();
                order.setOrderItems(new HashSet<>());
                session.setAttribute("cart", order);
            }
        }

        model.addAttribute("order", order); // Zmieniono na "order" dla spójności z widokiem
        return "cart"; // Nazwa widoku Thymeleaf, np. cart.html
    }

    @Transactional
    @PostMapping("/add/{productId}")
    public String addToCart(@PathVariable Long productId, @AuthenticationPrincipal Principal principal, HttpSession session, RedirectAttributes redirectAttributes) {
        Order order;

        if (principal != null) {
            User user = userRepository.findByUsername(principal.getName()).orElseThrow(() -> new RuntimeException("User not found"));
            Optional<Order> cart = orderRepository.findByUserAndStatus(user, "PENDING");

            order = cart.orElseGet(() -> {
                Order newOrder = new Order();
                newOrder.setUser(user);
                newOrder.setStatus("PENDING");
                newOrder.setTotalPrice(BigDecimal.ZERO);
                newOrder.setOrderItems(new HashSet<>());
                return orderRepository.save(newOrder);
            });
        } else {
            order = (Order) session.getAttribute("cart");
            if (order == null) {
                order = new Order();
                order.setOrderItems(new HashSet<>());
                order.setTotalPrice(BigDecimal.ZERO);
                session.setAttribute("cart", order);
            }
        }

        Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Product not found"));

        if (product.getStock() <= 0) {
            redirectAttributes.addFlashAttribute("message", "Przepraszamy, ale obecnie nie mamy tego produktu na stanie.");
            return "redirect:/home";
        }

        Optional<OrderItem> existingItem = order.getOrderItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();

        if (existingItem.isPresent()) {
            OrderItem orderItem = existingItem.get();
            orderItem.setQuantity(orderItem.getQuantity() + 1);
            orderItem.setTotalItemPrice(orderItem.getProduct().getPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity())));
        } else {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(1);
            orderItem.setPrice(product.getPrice());
            orderItem.setTotalItemPrice(product.getPrice());
            order.getOrderItems().add(orderItem);
        }

        order.setTotalPrice(order.getOrderItems().stream()
                .map(OrderItem::getTotalItemPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        if (principal != null) {
            orderRepository.save(order);
        } else {
            session.setAttribute("cart", order);
        }

        redirectAttributes.addFlashAttribute("message", "Produkt został dodany do koszyka!");
        return "redirect:/cart"; // Przekierowanie do koszyka zamiast na stronę główną
    }

    @Transactional
    @PostMapping("/placeOrder")
    public String placeOrder(@AuthenticationPrincipal Principal principal, HttpSession session, RedirectAttributes redirectAttributes) {
        Order order;

        if (principal != null) {
            User user = userRepository.findByUsername(principal.getName()).orElseThrow(() -> new RuntimeException("User not found"));
            order = orderRepository.findByUserAndStatus(user, "PENDING").orElse(null);
            if (order != null && !order.getOrderItems().isEmpty()) {
                for (OrderItem item : order.getOrderItems()) {
                    Product product = item.getProduct();
                    if (product.getStock() < item.getQuantity()) {
                        redirectAttributes.addFlashAttribute("message", "Przepraszamy, ale produkt " + product.getName() + " jest obecnie niedostępny w odpowiedniej ilości.");
                        return "redirect:/cart";
                    }
                    product.setStock(product.getStock() - item.getQuantity());
                    productRepository.save(product);
                }
                order.setStatus("CONFIRMED");
                orderRepository.save(order);
            }
        } else {
            order = (Order) session.getAttribute("cart");
            if (order != null && !order.getOrderItems().isEmpty()) {
                for (OrderItem item : order.getOrderItems()) {
                    Product product = item.getProduct();
                    if (product.getStock() < item.getQuantity()) {
                        redirectAttributes.addFlashAttribute("message", "Przepraszamy, ale produkt " + product.getName() + " jest obecnie niedostępny w odpowiedniej ilości.");
                        return "redirect:/cart";
                    }
                    product.setStock(product.getStock() - item.getQuantity());
                    productRepository.save(product);
                }
                order.setStatus("CONFIRMED");
                orderRepository.save(order);
                session.removeAttribute("cart");
            }
        }

        redirectAttributes.addFlashAttribute("message", "Zamówienie zostało złożone.");
        return "redirect:/home";
    }

    @Transactional
    @PostMapping("/cancel")
    public String cancelOrder(@AuthenticationPrincipal Principal principal, HttpSession session, RedirectAttributes redirectAttributes) {
        Order order;

        if (principal != null) {
            User user = userRepository.findByUsername(principal.getName()).orElseThrow(() -> new RuntimeException("User not found"));
            order = orderRepository.findByUserAndStatus(user, "PENDING").orElse(null);
            if (order != null) {
                orderRepository.delete(order);
            }
        } else {
            session.removeAttribute("cart");
        }

        redirectAttributes.addFlashAttribute("message", "Zamówienie zostało anulowane.");
        return "redirect:/cart";
    }
}
