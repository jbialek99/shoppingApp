package com.example.shoppingapp.controller;

import com.example.shoppingapp.model.User;
import com.example.shoppingapp.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Optional;

@Controller
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Wyświetlenie formularza rejestracji
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    // Obsługa rejestracji
    @PostMapping("/register")
    public String register(@Valid User user, BindingResult result,
                           @RequestParam("confirm_password") String confirmPassword,
                           RedirectAttributes redirectAttributes) {

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

        // Kodowanie hasła i zapis użytkownika
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);

        // Komunikat o sukcesie rejestracji
        redirectAttributes.addFlashAttribute("success", "Konto utworzone pomyślnie. Możesz się teraz zalogować.");
        return "redirect:/login";
    }

    // Wyświetlenie formularza logowania
    @GetMapping("/login")
    public String showLoginForm(Model model,
                                @RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getName().equals("anonymousUser")) {
            return "redirect:/home";
        }

        model.addAttribute("user", new User());

        if (error != null) {
            model.addAttribute("error", "Nieprawidłowa nazwa użytkownika lub hasło.");
        }

        if (logout != null) {
            model.addAttribute("message", "Wylogowano pomyślnie.");
        }

        if (model.containsAttribute("success")) {
            model.addAttribute("message", model.asMap().get("success"));
        }

        return "login";
    }

    // Formularz danych osobistych użytkownika
// Wyświetlenie formularza "Moje dane"
    @GetMapping("/my-data")
    public String showMyDataForm(Model model, @AuthenticationPrincipal Principal principal) {
        if (principal != null) {
            Optional<User> userOptional = userRepository.findByUsername(principal.getName());
            if (userOptional.isPresent()) {
                model.addAttribute("user", userOptional.get()); // Dodajemy obiekt `user` do modelu
            } else {
                model.addAttribute("user", new User()); // Dla bezpieczeństwa dodajemy pusty obiekt
            }
        } else {
            model.addAttribute("user", new User()); // Dla użytkowników niezalogowanych
        }
        return "my-data";
    }


    // Obsługa aktualizacji danych użytkownika
    @PostMapping("/my-data")
    public String updateMyData(User user, BindingResult result, Principal principal, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "my-data"; // powrót do formularza przy błędzie
        }

        // Sprawdzenie, czy użytkownik jest zalogowany
        if (principal != null) {
            Optional<User> existingUserOptional = userRepository.findByUsername(principal.getName());
            if (existingUserOptional.isPresent()) {
                User existingUser = existingUserOptional.get();
                // Aktualizowanie danych osobistych użytkownika
                existingUser.setFirstName(user.getFirstName());
                existingUser.setLastName(user.getLastName());
                existingUser.setAddress(user.getAddress());
                existingUser.setPhone(user.getPhone());
                userRepository.save(existingUser); // Zapisanie zmian w bazie danych
            }
        }

        redirectAttributes.addFlashAttribute("message", "Pomyślnie ustawiono Twoje dane.");
        return "redirect:/home"; // Przekierowanie na stronę główną
    }
}
