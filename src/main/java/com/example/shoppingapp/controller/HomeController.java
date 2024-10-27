package com.example.shoppingapp.controller;

import com.example.shoppingapp.model.Order;
import com.example.shoppingapp.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class HomeController {

    private final ProductRepository productRepository;

    public HomeController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/home")
    public String home(Model model, @AuthenticationPrincipal Principal principal, HttpSession session) {
        model.addAttribute("products", productRepository.findAll());

        Order order;
        if (principal != null) {
            // Użytkownik zalogowany
            order = (Order) session.getAttribute("cart");
        } else {
            // Użytkownik niezalogowany
            order = (Order) session.getAttribute("guestCart");
        }
        model.addAttribute("order", order);

        return "home";
    }
}
