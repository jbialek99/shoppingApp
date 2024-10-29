package com.example.shoppingapp.security;

import com.example.shoppingapp.model.User;
import com.example.shoppingapp.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Wyszukiwanie użytkownika w bazie danych na podstawie nazwy użytkownika
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found")); // Wyjątek, jeśli użytkownik nie istnieje

        // Zwrócenie niestandardowego obiektu UserDetails
        return new CustomUserDetails(user); // Zwracamy obiekt CustomUserDetails
    }
}
