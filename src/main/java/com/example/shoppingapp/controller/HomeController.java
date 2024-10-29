package com.example.shoppingapp.controller;

import com.example.shoppingapp.model.Order;
import com.example.shoppingapp.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashSet;

@Controller
public class HomeController {

    private final ProductRepository productRepository;

    public HomeController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/home")
    public String home(Model model, HttpSession session) {
        model.addAttribute("products", productRepository.findAll());

        // Pobierz koszyk z sesji lub utwórz nowy, jeśli jeszcze nie istnieje
        Order order = (Order) session.getAttribute("cart");
        if (order == null) {
            order = new Order();
            order.setOrderItems(new HashSet<>());
            session.setAttribute("cart", order);
        }

        // Oblicz liczbę produktów w koszyku
        int itemCount = (order.getOrderItems() != null) ? order.getOrderItems().size() : 0;

        model.addAttribute("order", order);
        model.addAttribute("itemCount", itemCount); // Przekazanie liczby produktów do widoku

        return "home";
    }
    @GetMapping("/contact")
    public String showContactPage() {
        return "contact"; // Nazwa widoku (contact.html)
    }
}
