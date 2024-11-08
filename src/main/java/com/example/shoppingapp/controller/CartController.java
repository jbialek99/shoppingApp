package com.example.shoppingapp.controller;

import com.example.shoppingapp.model.*;
import com.example.shoppingapp.repository.OrderRepository;
import com.example.shoppingapp.repository.ProductRepository;
import com.example.shoppingapp.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
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

    @GetMapping
    public String viewCart(Model model, HttpSession session, @AuthenticationPrincipal UserDetails userDetails) {
        Order order = getOrder(userDetails, session);

        int itemCount = order.getOrderItems().size();
        session.setAttribute("itemCount", itemCount);
        model.addAttribute("itemCount", itemCount);
        model.addAttribute("order", order);

        return "cart";
    }

    @Transactional
    @PostMapping("/add/{productId}")
    public String addToCart(@PathVariable Long productId,
                            @AuthenticationPrincipal UserDetails userDetails,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {

        Order order = getOrder(userDetails, session);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produkt nie znaleziony"));

        if (product.getStock() <= 0) {
            redirectAttributes.addFlashAttribute("message", "Przepraszamy, produkt nie jest dostępny.");
            return "redirect:/home";
        }

        addOrUpdateOrderItem(order, product);
        updateTotalPrice(order);
        saveOrder(userDetails, session, order);

        int itemCount = order.getOrderItems().size();
        session.setAttribute("itemCount", itemCount);
        redirectAttributes.addFlashAttribute("itemCount", itemCount);
        redirectAttributes.addFlashAttribute("message", "Produkt został dodany do koszyka!");

        return "redirect:/home";
    }

    @Transactional
    @PostMapping("/placeOrder")
    public String placeOrder(@AuthenticationPrincipal UserDetails userDetails, HttpSession session, RedirectAttributes redirectAttributes) {
        Order order = getOrder(userDetails, session);

        if (order.getOrderItems().isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Koszyk jest pusty, dodaj produkty przed złożeniem zamówienia.");
            return "redirect:/cart";
        }

        for (OrderItem item : order.getOrderItems()) {
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Produkt nie znaleziony"));

            if (product.getStock() < item.getQuantity()) {
                redirectAttributes.addFlashAttribute("message",
                        "Przepraszamy, ale produkt '" + product.getName() + "' jest dostępny w ilości " + product.getStock() + " sztuk.");
                return "redirect:/cart";
            }
        }

        return "redirect:/cart/checkout";
    }

    @GetMapping("/checkout")
    public String checkout(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userDetails != null ? userRepository.findByUsername(userDetails.getUsername()).orElse(null) : null;
        model.addAttribute("user", user != null ? user : new User());
        return "checkout";
    }

    @Transactional
    @PostMapping("/checkout/submit")
    public String submitCheckout(@Validated(ValidationGroups.Update.class) @ModelAttribute User user,
                                 BindingResult result,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("user", user);
            return "checkout";
        }

        Order order = getOrder(userDetails, session);

        order.setContactName(user.getFirstName() + " " + user.getLastName());
        order.setContactPhone(user.getPhone());
        order.setContactAddress(user.getAddress());

        if (order.getOrderItems().isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Koszyk jest pusty, dodaj produkty przed złożeniem zamówienia.");
            return "redirect:/cart";
        }

        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            int newStock = product.getStock() - item.getQuantity();
            if (newStock < 0) {
                redirectAttributes.addFlashAttribute("message", "Niewystarczający stan magazynowy dla produktu " + product.getName());
                return "redirect:/cart";
            }
            product.setStock(newStock);
            productRepository.save(product);
        }

        order.setStatus("CONFIRMED");
        orderRepository.save(order);
        session.removeAttribute("itemCount");

        if (userDetails == null) {
            session.removeAttribute("cart");
        }

        redirectAttributes.addFlashAttribute("message", "Zamówienie zostało złożone.");
        return "redirect:/home";
    }

    @Transactional
    @PostMapping("/cancel")
    public String cancelOrder(@AuthenticationPrincipal UserDetails userDetails, HttpSession session, RedirectAttributes redirectAttributes) {
        Order order = getOrder(userDetails, session);

        if (order != null) {
            if (userDetails != null) {
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
    public String decreaseQuantity(@PathVariable Long productId, @AuthenticationPrincipal UserDetails userDetails, HttpSession session) {
        Order order = getOrder(userDetails, session);

        order.getOrderItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .ifPresent(item -> {
                    if (item.getQuantity() > 1) {
                        item.setQuantity(item.getQuantity() - 1);
                        item.setTotalItemPrice(item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                    }
                    updateTotalPrice(order);
                });

        saveOrder(userDetails, session, order);
        return "redirect:/cart";
    }

    @Transactional
    @PostMapping("/increaseQuantity/{productId}")
    public String increaseQuantity(@PathVariable Long productId, @AuthenticationPrincipal UserDetails userDetails, HttpSession session, RedirectAttributes redirectAttributes) {
        Order order = getOrder(userDetails, session);

        order.getOrderItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .ifPresent(item -> {
                    item.setQuantity(item.getQuantity() + 1);
                    item.setTotalItemPrice(item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                });

        updateTotalPrice(order);
        saveOrder(userDetails, session, order);

        redirectAttributes.addFlashAttribute("message", "Produkt został dodany.");
        return "redirect:/cart";
    }

    @Transactional
    @PostMapping("/removeItem/{productId}")
    public String removeItem(@PathVariable Long productId, @AuthenticationPrincipal UserDetails userDetails, HttpSession session) {
        Order order = getOrder(userDetails, session);

        order.getOrderItems().removeIf(item -> item.getProduct().getId().equals(productId));
        updateTotalPrice(order);

        saveOrder(userDetails, session, order);
        return "redirect:/cart";
    }

    private void saveOrder(UserDetails userDetails, HttpSession session, Order order) {
        if (userDetails != null) {
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

    private Order getOrder(UserDetails userDetails, HttpSession session) {
        return userDetails != null ? getOrderFromDatabase(userDetails.getUsername()) : getOrderFromSession(session);
    }

    private Order getOrderFromDatabase(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

        return orderRepository.findByUserAndStatus(user, "PENDING").orElseGet(() -> {
            Order newOrder = new Order();
            newOrder.setUser(user);
            newOrder.setStatus("PENDING");
            newOrder.setTotalPrice(BigDecimal.ZERO);
            newOrder.setOrderItems(new HashSet<>());
            return orderRepository.save(newOrder);
        });
    }

    private Order getOrderFromSession(HttpSession session) {
        Order order = (Order) session.getAttribute("cart");
        if (order == null) {
            order = createAndSaveOrderInSession(session);
        }
        return order;
    }

    private Order createAndSaveOrderInSession(HttpSession session) {
        Order order = new Order();
        order.setStatus("PENDING");
        order.setTotalPrice(BigDecimal.ZERO);
        order.setOrderItems(new HashSet<>());
        session.setAttribute("cart", order);
        return order;
    }

    private void addOrUpdateOrderItem(Order order, Product product) {
        Optional<OrderItem> existingItem = order.getOrderItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
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
    }
}
