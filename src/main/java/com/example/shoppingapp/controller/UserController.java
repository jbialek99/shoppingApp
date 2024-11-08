package com.example.shoppingapp.controller;

import com.example.shoppingapp.model.User;
import com.example.shoppingapp.model.ValidationGroups;
import com.example.shoppingapp.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
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
    public String register(@Validated(ValidationGroups.Registration.class) User user,
                           BindingResult result,
                           @RequestParam("confirm_password") String confirmPassword,
                           RedirectAttributes redirectAttributes) {

        if (!user.getPassword().equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Hasła muszą być identyczne.");
            return "redirect:/register";
        }

        if (result.hasErrors()) {
            if (result.getFieldError("password") != null) {
                redirectAttributes.addFlashAttribute("error", "Hasło nie spełnia wymagań.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Formularz zawiera bledy");
            }
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
    @GetMapping("/my-data")
    public String showMyDataForm(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String username;
            if (authentication.getPrincipal() instanceof UserDetails) {
                username = ((UserDetails) authentication.getPrincipal()).getUsername();
            } else {
                username = authentication.getPrincipal().toString();
            }

            Optional<User> userOptional = userRepository.findByUsername(username);
            if (userOptional.isPresent()) {
                model.addAttribute("user", userOptional.get());
            } else {
                model.addAttribute("user", new User());
            }
        } else {
            model.addAttribute("user", new User());
        }
        return "my-data";
    }

    // Obsługa aktualizacji danych użytkownika



    @PostMapping("/my-data")
    public String updateMyData(@Validated(ValidationGroups.Update.class) User user,
                               BindingResult result,
                               Principal principal,
                               Model model,
                               RedirectAttributes redirectAttributes) {


        // Sprawdzenie błędów walidacji
        if (result.hasErrors()) {

            model.addAttribute("user", user); // Przekazanie użytkownika z błędami do modelu
            return "my-data"; // Powrót do widoku my-data z błędami walidacji
        }

        // Upewnienie się, że użytkownik jest zalogowany
        if (principal != null) {
            String username = principal.getName();
            Optional<User> existingUserOptional = userRepository.findByUsername(username);

            if (existingUserOptional.isPresent()) {
                User existingUser = existingUserOptional.get();
                // Aktualizacja pól użytkownika
                existingUser.setFirstName(user.getFirstName());
                existingUser.setLastName(user.getLastName());
                existingUser.setAddress(user.getAddress());
                existingUser.setPhone(user.getPhone());

                // Zapis do bazy danych
                userRepository.save(existingUser);


                // Ustawienie komunikatu o sukcesie
                redirectAttributes.addFlashAttribute("message", "Pomyślnie ustawiono Twoje dane.");
                return "redirect:/home"; // Przekierowanie na `home`
            }
        }
        // W razie problemów zwróć formularz z komunikatem o błędzie
        model.addAttribute("error", "Wystąpił problem z aktualizacją danych użytkownika.");
        return "my-data";
    }

    // Wyświetlenie formularza checkout z automatycznym wypełnieniem danych
    @GetMapping("/checkout")
    public String showCheckoutForm(Model model, @AuthenticationPrincipal Principal principal) {
        if (principal != null) {
            Optional<User> userOptional = userRepository.findByUsername(principal.getName());
            if (userOptional.isPresent()) {
                model.addAttribute("user", userOptional.get());
            } else {
                model.addAttribute("user", new User());
            }
        } else {
            model.addAttribute("user", new User());
        }
        return "checkout";
    }
}
