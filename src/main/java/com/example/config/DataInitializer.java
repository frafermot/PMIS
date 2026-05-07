package com.example.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.user.Role;
import com.example.user.User;
import com.example.user.UserService;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Set System Admin context for initialization
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "system", "system",
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_SYSTEM_ADMIN"))));

        User coordinador = new User();
        coordinador.setName("Juan Manuel Cordero Valle");
        coordinador.setUvus("jmcordero");
        coordinador.setRole(Role.ADMIN);
        coordinador.setPassword(passwordEncoder.encode("password"));
        userService.createOrUpdate(coordinador);
    }
}