package in.sb.pinac.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*", "*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        configuration.setExposedHeaders(Arrays.asList("Access-Control-Allow-Origin", "Access-Control-Allow-Credentials", "Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Allow static resources & preflight OPTIONS
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Specific authenticated endpoints under /api/auth and /api/admin/auth
                .requestMatchers("/api/auth/me", "/api/admin/auth/me", "/api/admin/auth/change-password").authenticated()
                // Public auth & OTP endpoints (User & Admin Auth)
                .requestMatchers(
                    "/api/admin/auth/login",
                    "/api/admin/auth/signup",
                    "/api/admin/auth/send-otp",
                    "/api/admin/auth/verify-otp",
                    "/api/admin/auth/forgot-password",
                    "/api/admin/auth/reset-password",
                    "/api/admin/auth/**",
                    "/api/auth/send-otp",
                    "/api/auth/verify-otp",
                    "/api/auth/signup",
                    "/api/auth/login",
                    "/api/auth/reset-password",
                    "/api/auth/**",
                    "/auth/**",
                    "/users/**",
                    "/otp/**",
                    "/api/otp/**"
                ).permitAll()
                // Public course catalog & reviews
                .requestMatchers("/courses/**", "/api/courses/**", "/categories/**", "/api/categories/**", "/api/reviews/**").permitAll()
                // Public payments & checkout
                .requestMatchers("/api/payments/**", "/payments/**").permitAll()
                // Student dashboard, LMS player, Live Sessions & Certificates
                .requestMatchers("/api/student/**", "/api/live-sessions/**", "/api/admin/live-sessions/**", "/api/certificates/**", "/api/certificate/**").permitAll()
                // Admin management (Strictly ADMIN only)
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().permitAll()
            );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
