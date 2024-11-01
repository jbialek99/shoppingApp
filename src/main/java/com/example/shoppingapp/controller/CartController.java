package com.example.shoppingapp.controller;

import com.example.shoppingapp.model.*;
import com.example.shoppingapp.repository.OrderRepository;
import com.example.shoppingapp.repository.ProductRepository;
import com.example.shoppingapp.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
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
    public String viewCart(Model model, HttpSession session, @AuthenticationPrincipal UserDetails userDetails) {
        Order order = getOrderFromSessionOrDatabase(userDetails, session);

        // Ustawienie itemCount w sesji i modelu
        int itemCount = (order.getOrderItems() != null) ? order.getOrderItems().size() : 0;
        session.setAttribute("itemCount", itemCount);
        model.addAttribute("itemCount", itemCount);
        model.addAttribute("order", order);

        return "cart";
    }

    private Order getOrderFromSessionOrDatabase(UserDetails userDetails, HttpSession session) {
        if (userDetails != null) {
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

            // Sprawdzenie, czy zamówienie istnieje w statusie PENDING
            return orderRepository.findByUserAndStatus(user, "PENDING").orElseGet(() -> {
                Order newOrder = new Order();
                newOrder.setUser(user); // Przypisanie użytkownika
                newOrder.setStatus("PENDING");
                newOrder.setTotalPrice(BigDecimal.ZERO);
                newOrder.setOrderItems(new HashSet<>());
                return orderRepository.save(newOrder); // Zapisz nowe zamówienie
            });
        } else {
            // Obsługa niezalogowanego użytkownika
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

    @Transactional
    @PostMapping("/add/{productId}")
    public String addToCart(@PathVariable Long productId,
                            @AuthenticationPrincipal UserDetails userDetails,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {

        Order order = getOrderFromSessionOrDatabase(userDetails, session);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produkt nie znaleziony"));

        if (product.getStock() <= 0) {
            redirectAttributes.addFlashAttribute("message", "Przepraszamy, produkt nie jest dostępny.");
            return "redirect:/home";
        }

        Optional<OrderItem> existingItem = order.getOrderItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();

        if (existingItem.isPresent()) {
            OrderItem orderItem = existingItem.get();
            orderItem.setQuantity(orderItem.getQuantity() + 1);
            orderItem.setTotalItemPrice(orderItem.getProduct().getPrice()
                    .multiply(BigDecimal.valueOf(orderItem.getQuantity())));
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
        saveOrder(userDetails, session, order);

        // Ustaw `itemCount` w sesji i dodaj do `RedirectAttributes`
        int itemCount = order.getOrderItems().size();
        session.setAttribute("itemCount", itemCount);
        redirectAttributes.addFlashAttribute("itemCount", itemCount);
        redirectAttributes.addFlashAttribute("message", "Produkt został dodany do koszyka!");

        return "redirect:/home";
    }

    @Transactional
    @PostMapping("/placeOrder")
    public String placeOrder(@AuthenticationPrincipal UserDetails userDetails, HttpSession session, RedirectAttributes redirectAttributes) {
        Order order = getOrderFromSessionOrDatabase(userDetails, session);

        // Debugowanie
        System.out.println("Zamówienie dla użytkownika: " + (userDetails != null ? userDetails.getUsername() : "niezalogowany"));
        System.out.println("Zawartość zamówienia: " + order.getOrderItems());

        // Sprawdzamy, czy zamówienie ma produkty
        if (order.getOrderItems().isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Koszyk jest pusty, dodaj produkty przed złożeniem zamówienia.");
            return "redirect:/cart";
        }

        // Weryfikacja stanów magazynowych dla każdego produktu
        for (OrderItem item : order.getOrderItems()) {
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Produkt nie znaleziony"));

            if (product.getStock() < item.getQuantity()) {
                redirectAttributes.addFlashAttribute("message",
                        "Przepraszamy, ale produkt '" + product.getName() + "' jest dostępny w ilości " + product.getStock() + " sztuk.");
                return "redirect:/cart"; // Pozostajemy na stronie koszyka
            }
        }

        // Jeśli ilości są wystarczające, przekierowujemy do strony checkout
        return "redirect:/cart/checkout";
    }

    // Wyświetlenie formularza checkout z automatycznym wypełnieniem danych użytkownika
    @GetMapping("/checkout")
    public String checkout(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = null;

        if (userDetails != null) {
            user = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        }

        model.addAttribute("user", user != null ? user : new User());
        return "checkout";
    }

    // Obsługa przesyłania formularza checkout
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

        String username = null;
        if (userDetails != null) {
            username = userDetails.getUsername();
        }

        Order order = getOrderFromSessionOrDatabaseByUsername(username, session);

        if (order == null || order.getOrderItems().isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Koszyk jest pusty, dodaj produkty przed złożeniem zamówienia.");
            return "redirect:/cart";
        }

        // Przypisz dane kontaktowe do zamówienia
        if (username != null) {
            User existingUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));
            order.setUser(existingUser);
            order.setContactName(existingUser.getFirstName() + " " + existingUser.getLastName());
            order.setContactPhone(existingUser.getPhone());
            order.setContactAddress(existingUser.getAddress());
        } else {
            order.setContactName(user.getFirstName() + " " + user.getLastName());
            order.setContactPhone(user.getPhone());
            order.setContactAddress(user.getAddress());
        }

        // Finalizacja zamówienia
        order.setStatus("CONFIRMED");
        orderRepository.save(order);

        // Usuwanie liczby elementów z koszyka z sesji
        session.removeAttribute("itemCount");

        if (username == null) {
            session.removeAttribute("cart");
        }

        redirectAttributes.addFlashAttribute("message", "Zamówienie zostało złożone.");
        return "redirect:/home";
    }




    @Transactional
    @PostMapping("/cancel")
    public String cancelOrder(@AuthenticationPrincipal UserDetails userDetails, HttpSession session, RedirectAttributes redirectAttributes) {
        Order order = getOrderFromSessionOrDatabase(userDetails, session);

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

    // Obsługa zmniejszania ilości produktu
    @Transactional
    @PostMapping("/decreaseQuantity/{productId}")
    public String decreaseQuantity(@PathVariable Long productId, @AuthenticationPrincipal UserDetails userDetails, HttpSession session) {
        Order order = getOrderFromSessionOrDatabase(userDetails, session);

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

        saveOrder(userDetails, session, order);
        return "redirect:/cart";
    }

    // Obsługa zwiększania ilości produktu
    @Transactional
    @PostMapping("/increaseQuantity/{productId}")
    public String increaseQuantity(@PathVariable Long productId, @AuthenticationPrincipal UserDetails userDetails, HttpSession session, RedirectAttributes redirectAttributes) {
        Order order = getOrderFromSessionOrDatabase(userDetails, session);

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
        Order order = getOrderFromSessionOrDatabase(userDetails, session);

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

    private Order getOrderFromSession(HttpSession session) {
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
    private Order getOrderFromSessionOrDatabaseByUsername(String username, HttpSession session) {
        if (username != null) {
            // Dla zalogowanego użytkownika, pobierz zamówienie z bazy danych
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

            // Sprawdzenie, czy zamówienie istnieje w statusie PENDING
            return orderRepository.findByUserAndStatus(user, "PENDING").orElseGet(() -> {
                Order newOrder = new Order();
                newOrder.setUser(user); // Przypisanie użytkownika
                newOrder.setStatus("PENDING");
                newOrder.setTotalPrice(BigDecimal.ZERO);
                newOrder.setOrderItems(new HashSet<>());
                return orderRepository.save(newOrder); // Zapisz nowe zamówienie
            });
        } else {
            // Dla niezalogowanego użytkownika, pobierz zamówienie z sesji
            return getOrderFromSession(session);
        }
    }

}

