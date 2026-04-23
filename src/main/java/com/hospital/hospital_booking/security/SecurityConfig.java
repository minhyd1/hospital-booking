package com.hospital.hospital_booking.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. Công khai (Permit All)
                        .requestMatchers("/api/users/login", "/api/users/register", "/api/users/forgot-password").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/doctors", "/api/users/doctors/**", "/api/schedule/available", "/api/specialties").permitAll()

                        // 2. Các API cá nhân (PHẢI ƯU TIÊN TRƯỚC ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/users/change-password", "/api/users/refresh-token").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/users/profile").authenticated()

                        // 3. Phân quyền cho Bác sĩ
                        .requestMatchers(HttpMethod.PUT, "/api/users/doctor-profile").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.POST, "/api/schedule").hasAnyRole("DOCTOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/schedule/**").hasAnyRole("DOCTOR", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/appointments/*/status").hasAnyRole("DOCTOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/appointments/upcoming/doctor/**", "/api/appointments/history/doctor/**").hasAnyRole("DOCTOR", "ADMIN")

                        // 4. Phân quyền cho Bệnh nhân
                        .requestMatchers(HttpMethod.POST, "/api/appointments/book").hasAnyRole("PATIENT", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/appointments/**").hasAnyRole("PATIENT", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/appointments/upcoming/patient/**", "/api/appointments/history/patient/**").hasAnyRole("PATIENT", "ADMIN")

                        // 5. Phân quyền cho Admin (Quản lý User theo ID)
                        .requestMatchers("/api/statistics/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/appointments").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/specialties/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/specialties/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/schedule/batch").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/users", "/api/users/{id}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/users/{id}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/{id}").hasRole("ADMIN")

                        // 6. Còn lại phải đăng nhập
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}