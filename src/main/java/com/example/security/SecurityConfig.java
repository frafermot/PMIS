package com.example.security;

import com.example.examplefeature.ui.LoginView;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@EnableWebSecurity
@Configuration
public class SecurityConfig extends VaadinWebSecurity {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(
                auth -> auth.requestMatchers(new AntPathRequestMatcher("/images/*.png")).permitAll());

        // Icons from the line-awesome addon
        http.authorizeHttpRequests(
                auth -> auth.requestMatchers(new AntPathRequestMatcher("/line-awesome/**/*.svg")).permitAll());

        // H2 Console - permitir acceso sin autenticación (solo para desarrollo)
        http.authorizeHttpRequests(
                auth -> auth.requestMatchers(new AntPathRequestMatcher("/h2-console/**")).permitAll());
        
        // Deshabilitar CSRF para H2 Console
        http.csrf(csrf -> csrf.ignoringRequestMatchers(new AntPathRequestMatcher("/h2-console/**")));

        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()));

        super.configure(http);

        setLoginView(http, LoginView.class);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
