package com.example.shoppingapp.controller;

import com.example.shoppingapp.model.User;
import com.example.shoppingapp.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Rejestracja GET
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    // Rejestracja POST
    @PostMapping("/register")
    public String register(@Valid User user, BindingResult result,
                           @RequestParam("confirm_password") String confirmPassword,
                           RedirectAttributes redirectAttributes) {

        // Walidacja czy hasła się zgadzają
        if (!user.getPassword().equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Hasła muszą być identyczne.");
            return "redirect:/register";
        }

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Formularz zawiera błędy.");
            return "redirect:/register";
        }

        if (userRepository.existsByUsername(user.getUsername())) {
            redirectAttributes.addFlashAttribute("error", "Nazwa użytkownika jest już zajęta.");
            return "redirect:/register";
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            redirectAttributes.addFlashAttribute("error", "Adres e-mail jest już używany.");
            return "redirect:/register";
        }

        // Kodowanie hasła i zapis
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);

        // Przekazanie komunikatu sukcesu
        redirectAttributes.addFlashAttribute("success", "Konto utworzone pomyślnie. Możesz się teraz zalogować.");
        return "redirect:/login";
    }

    // Logowanie GET
    @GetMapping("/login")
    public String showLoginForm(Model model,
                                @RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout) {

        // Sprawdzamy, czy użytkownik jest już zalogowany
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getName().equals("anonymousUser")) {
            return "redirect:/home";  // Przekieruj na stronę główną, jeśli użytkownik jest zalogowany
        }

        model.addAttribute("user", new User());

        // Komunikaty dla logowania i wylogowania
        if (error != null) {
            model.addAttribute("error", "Nieprawidłowa nazwa użytkownika lub hasło.");
        }

        if (logout != null) {
            model.addAttribute("message", "Wylogowano pomyślnie.");
        }

        // Sprawdzamy, czy przekazano komunikat o sukcesie rejestracji
        if (model.containsAttribute("success")) {
            model.addAttribute("message", model.asMap().get("success"));
        }

        return "login";
    }
}
