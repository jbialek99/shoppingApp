package com.example.shoppingapp.controller;

import com.example.shoppingapp.model.Order;
import com.example.shoppingapp.model.OrderItem;
import com.example.shoppingapp.model.Product;
import com.example.shoppingapp.model.User;
import com.example.shoppingapp.repository.OrderRepository;
import com.example.shoppingapp.repository.ProductRepository;
import com.example.shoppingapp.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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
    public String placeOrder(@AuthenticationPrincipal Principal principal, HttpSession session, RedirectAttributes redirectAttributes) {
        Order order = getOrderFromSessionOrDatabase(principal, session);

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
    public String checkout(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user;

        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserDetails) {

            String username = ((UserDetails) authentication.getPrincipal()).getUsername();
            Optional<User> userOptional = userRepository.findByUsername(username);
            user = userOptional.orElse(new User());

        } else {
            // Użytkownik nie jest zalogowany
            user = new User();
        }

        model.addAttribute("user", user);
        return "checkout";
    }

    // Obsługa przesyłania formularza checkout
    @Transactional
    @PostMapping("/checkout/submit")
    public String submitCheckout(@ModelAttribute User user, @AuthenticationPrincipal Principal principal, HttpSession session, RedirectAttributes redirectAttributes) {
        Order order = getOrderFromSessionOrDatabase(principal, session);

        // Sprawdzamy, czy zamówienie istnieje i czy koszyk nie jest pusty
        if (order == null || order.getOrderItems().isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Koszyk jest pusty, dodaj produkty przed złożeniem zamówienia.");
            return "redirect:/cart";
        }

        // Jeśli użytkownik jest zalogowany, pobieramy go z bazy danych i przypisujemy do zamówienia
        if (principal != null) {
            String username = principal.getName();

            // Pobieramy obiekt User na podstawie nazwy użytkownika
            User existingUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));


            // Przypisujemy user_id oraz dane kontaktowe zalogowanego użytkownika
            order.setUser(existingUser);  // Przypisanie user_id do zamówienia
            order.setContactName(existingUser.getFirstName() + " " + existingUser.getLastName());
            order.setContactPhone(existingUser.getPhone());
            order.setContactAddress(existingUser.getAddress());
        } else {
            // Ustawiamy dane kontaktowe dla niezalogowanego użytkownika na podstawie formularza
            order.setContactName(user.getFirstName() + " " + user.getLastName());
            order.setContactPhone(user.getPhone());
            order.setContactAddress(user.getAddress());
        }

        // Aktualizacja stanu magazynowego i zatwierdzenie zamówienia
        for (OrderItem item : order.getOrderItems()) {
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Produkt nie znaleziony"));
            if (product.getStock() < item.getQuantity()) {
                redirectAttributes.addFlashAttribute("message", "Przepraszamy, ale produkt " + product.getName() + " jest dostępny w ograniczonej ilości.");
                return "redirect:/cart";
            }
            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);
        }

        // Ustawienie statusu zamówienia na "CONFIRMED" i zapis w bazie
        order.setStatus("CONFIRMED");


        // Zapisujemy zamówienie do bazy danych
        orderRepository.save(order);

        // Czyszczenie koszyka z sesji dla niezalogowanych
        if (principal == null) {
            session.removeAttribute("cart");
        }

        redirectAttributes.addFlashAttribute("message", "Zamówienie zostało złożone i zostanie zrealizowane.");
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

    // Obsługa zmniejszania ilości produktu
    @Transactional
    @PostMapping("/decreaseQuantity/{productId}")
    public String decreaseQuantity(@PathVariable Long productId, @AuthenticationPrincipal Principal principal, HttpSession session) {
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

    // Obsługa zwiększania ilości produktu
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
    public String removeItem(@PathVariable Long productId, @AuthenticationPrincipal Principal principal, HttpSession session) {
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

}
