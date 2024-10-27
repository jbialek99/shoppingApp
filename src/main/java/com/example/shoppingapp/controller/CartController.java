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

        product.setStock(product.getStock() - 1);
        productRepository.save(product);

        if (principal != null) {
            orderRepository.save(order);
        } else {
            session.setAttribute("cart", order);
        }

        redirectAttributes.addFlashAttribute("message", "Produkt został dodany do koszyka!");
        return "redirect:/home";
    }

    @GetMapping
    public String viewCart(Model model, @AuthenticationPrincipal Principal principal, HttpSession session) {
        Order order;

        if (principal != null) {
            User user = userRepository.findByUsername(principal.getName()).orElseThrow(() -> new RuntimeException("User not found"));
            order = orderRepository.findByUserAndStatus(user, "PENDING").orElse(new Order());
        } else {
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

    @PostMapping("/cancel")
    public String cancelOrder(@AuthenticationPrincipal Principal principal, HttpSession session, RedirectAttributes redirectAttributes) {
        Order order;

        if (principal != null) {
            User user = userRepository.findByUsername(principal.getName()).orElseThrow(() -> new RuntimeException("User not found"));
            order = orderRepository.findByUserAndStatus(user, "PENDING").orElse(null);
            if (order != null) {
                for (OrderItem item : order.getOrderItems()) {
                    Product product = item.getProduct();
                    product.setStock(product.getStock() + item.getQuantity());
                    productRepository.save(product);
                }
                orderRepository.delete(order);
            }
        } else {
            order = (Order) session.getAttribute("cart");
            if (order != null) {
                for (OrderItem item : order.getOrderItems()) {
                    Product product = item.getProduct();
                    product.setStock(product.getStock() + item.getQuantity());
                    productRepository.save(product);
                }
                session.removeAttribute("cart");
            }
        }

        redirectAttributes.addFlashAttribute("message", "Zamówienie zostało anulowane.");
        return "redirect:/cart";
    }

    @PostMapping("/placeOrder")
    public String placeOrder(@AuthenticationPrincipal Principal principal, HttpSession session, RedirectAttributes redirectAttributes) {
        Order order;

        if (principal != null) {
            User user = userRepository.findByUsername(principal.getName()).orElseThrow(() -> new RuntimeException("User not found"));
            order = orderRepository.findByUserAndStatus(user, "PENDING").orElse(null);
            if (order != null) {
                order.setStatus("CONFIRMED");
                orderRepository.save(order);
            }
        } else {
            order = (Order) session.getAttribute("cart");
            if (order != null) {
                order.setStatus("CONFIRMED");
                orderRepository.save(order);
                session.removeAttribute("cart");
            }
        }

        redirectAttributes.addFlashAttribute("message", "Zamówienie zostało złożone.");
        return "redirect:/home";
    }
}
