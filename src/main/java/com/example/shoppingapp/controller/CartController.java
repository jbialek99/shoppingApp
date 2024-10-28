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
        Order order = getOrderFromSessionOrDatabase(principal, session);
        model.addAttribute("order", order);
        return "cart";
    }

    @Transactional
    @PostMapping("/add/{productId}")
    public String addToCart(@PathVariable Long productId, @AuthenticationPrincipal Principal principal, HttpSession session, RedirectAttributes redirectAttributes) {
        Order order = getOrderFromSessionOrDatabase(principal, session);
        Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Produkt nie znaleziony"));

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

        updateTotalPrice(order);
        saveOrder(principal, session, order);

        redirectAttributes.addFlashAttribute("message", "Produkt został dodany do koszyka!");
        return "redirect:/home";
    }

    @Transactional
    @PostMapping("/placeOrder")
    public String placeOrder(@AuthenticationPrincipal Principal principal, HttpSession session, RedirectAttributes redirectAttributes, Model model) {
        Order order = getOrderFromSessionOrDatabase(principal, session);

        // Sprawdzenie danych wysyłki dla zalogowanego użytkownika
        if (principal != null) {
            User user = userRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

            if (user.getAddress() == null || user.getPhone() == null || user.getFirstName() == null || user.getLastName() == null) {
                model.addAttribute("user", user);
                redirectAttributes.addFlashAttribute("message", "Proszę uzupełnić dane do wysyłki.");
                return "redirect:/cart/checkout";
            }
        } else {
            return "redirect:/cart/checkout";
        }

        boolean success = finalizeOrder(order, redirectAttributes);

        if (!success) {
            return "redirect:/cart";
        }

        // Usunięcie koszyka z sesji lub zapis statusu zamówienia jako "CONFIRMED"
        if (principal != null) {
            order.setStatus("CONFIRMED");
            orderRepository.save(order);
        } else {
            session.removeAttribute("cart");
        }

        redirectAttributes.addFlashAttribute("message", "Zamówienie zostało złożone.");
        return "redirect:/home";
    }

    @GetMapping("/checkout")
    public String checkout(Model model, @AuthenticationPrincipal Principal principal) {
        User user = (principal != null) ?
                userRepository.findByUsername(principal.getName()).orElse(new User()) :
                new User();

        // Przekazanie obiektu użytkownika do formularza
        model.addAttribute("user", user);
        return "checkout";
    }

    @Transactional
    @PostMapping("/checkout/submit")
    public String submitCheckout(@ModelAttribute User user, @AuthenticationPrincipal Principal principal, HttpSession session, RedirectAttributes redirectAttributes) {
        Order order = getOrderFromSessionOrDatabase(principal, session);

        if (principal != null) {
            User existingUser = userRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

            existingUser.setFirstName(user.getFirstName());
            existingUser.setLastName(user.getLastName());
            existingUser.setAddress(user.getAddress());
            existingUser.setPhone(user.getPhone());
            userRepository.save(existingUser);
        }

        boolean success = finalizeOrder(order, redirectAttributes);
        if (!success) {
            return "redirect:/cart";
        }

        if (principal != null) {
            order.setStatus("CONFIRMED");
            orderRepository.save(order);
        } else {
            session.removeAttribute("cart");
        }

        redirectAttributes.addFlashAttribute("message", "Dane do wysyłki zapisane. Zamówienie złożone!");
        return "redirect:/home";
    }

    @Transactional
    @PostMapping("/cancel")
    public String cancelOrder(@AuthenticationPrincipal Principal principal, HttpSession session, RedirectAttributes redirectAttributes) {
        Order order = getOrderFromSessionOrDatabase(principal, session);

        if (order != null) {
            if (principal != null) {
                orderRepository.delete(order);
            } else {
                session.removeAttribute("cart");
            }
            redirectAttributes.addFlashAttribute("message", "Zamówienie zostało anulowane.");
        }

        return "redirect:/cart";
    }

    @Transactional
    @PostMapping("/decreaseQuantity/{productId}")
    public String decreaseQuantity(@PathVariable Long productId, @AuthenticationPrincipal Principal principal, HttpSession session, RedirectAttributes redirectAttributes) {
        Order order = getOrderFromSessionOrDatabase(principal, session);

        order.getOrderItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .ifPresent(item -> {
                    if (item.getQuantity() > 1) {
                        item.setQuantity(item.getQuantity() - 1);
                        item.setTotalItemPrice(item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                    } else {
                        order.getOrderItems().remove(item);
                    }
                    updateTotalPrice(order);
                });

        saveOrder(principal, session, order);
        return "redirect:/cart";
    }

    @Transactional
    @PostMapping("/increaseQuantity/{productId}")
    public String increaseQuantity(@PathVariable Long productId, @AuthenticationPrincipal Principal principal, HttpSession session, RedirectAttributes redirectAttributes) {
        Order order = getOrderFromSessionOrDatabase(principal, session);

        order.getOrderItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .ifPresent(item -> {
                    item.setQuantity(item.getQuantity() + 1);
                    item.setTotalItemPrice(item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                });

        updateTotalPrice(order);
        saveOrder(principal, session, order);

        redirectAttributes.addFlashAttribute("message", "Produkt został dodany.");
        return "redirect:/cart";
    }

    @Transactional
    @PostMapping("/removeItem/{productId}")
    public String removeItem(@PathVariable Long productId, @AuthenticationPrincipal Principal principal, HttpSession session, RedirectAttributes redirectAttributes) {
        Order order = getOrderFromSessionOrDatabase(principal, session);

        order.getOrderItems().removeIf(item -> item.getProduct().getId().equals(productId));
        updateTotalPrice(order);

        saveOrder(principal, session, order);
        return "redirect:/cart";
    }

    private Order getOrderFromSessionOrDatabase(Principal principal, HttpSession session) {
        if (principal != null) {
            User user = userRepository.findByUsername(principal.getName()).orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));
            return orderRepository.findByUserAndStatus(user, "PENDING").orElseGet(() -> {
                Order newOrder = new Order();
                newOrder.setUser(user);
                newOrder.setStatus("PENDING");
                newOrder.setTotalPrice(BigDecimal.ZERO);
                newOrder.setOrderItems(new HashSet<>());
                return orderRepository.save(newOrder);
            });
        } else {
            Order order = (Order) session.getAttribute("cart");
            if (order == null) {
                order = new Order();
                order.setStatus("PENDING");
                order.setTotalPrice(BigDecimal.ZERO);
                order.setOrderItems(new HashSet<>());
                session.setAttribute("cart", order);
            }
            return order;
        }
    }

    private void saveOrder(Principal principal, HttpSession session, Order order) {
        if (principal != null) {
            orderRepository.save(order);
        } else {
            session.setAttribute("cart", order);
        }
    }

    private void updateTotalPrice(Order order) {
        order.setTotalPrice(order.getOrderItems().stream()
                .map(OrderItem::getTotalItemPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private boolean finalizeOrder(Order order, RedirectAttributes redirectAttributes) {
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            if (product.getStock() < item.getQuantity()) {
                redirectAttributes.addFlashAttribute("message", "Przepraszamy, ale produkt " + product.getName() +
                        " jest obecnie dostępny w ilości " + product.getStock() + " sztuk.");
                return false;
            }
            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);
        }
        order.setStatus("CONFIRMED");
        orderRepository.save(order);
        return true;
    }
}
