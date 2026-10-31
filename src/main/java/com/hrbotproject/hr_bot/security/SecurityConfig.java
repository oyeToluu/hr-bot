package com.hrbotproject.hr_bot.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final EmployeeUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // No/invalid token -> 401 (not 403), so the web and Android
                // clients know to send the user back to the login screen.
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        // /me needs a logged-in staff member. It must come BEFORE
                        // the /api/auth/** permitAll, because the first match wins.
                        .requestMatchers("/api/auth/me").hasAnyRole("EMPLOYEE", "HR_ADMIN")
                        .requestMatchers("/api/auth/**").permitAll()

                        // ---- Recruitment / CV screening ----
                        // Order matters: the first matching rule wins, so the
                        // specific HR-only GETs come before the public job board.
                        .requestMatchers(HttpMethod.GET, "/api/jobs/manage", "/api/jobs/*/applications").hasRole("HR_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/jobs", "/api/jobs/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/jobs/*/apply").hasRole("CANDIDATE")
                        .requestMatchers("/api/jobs/**").hasRole("HR_ADMIN")
                        .requestMatchers("/api/applications/mine").hasRole("CANDIDATE")
                        .requestMatchers("/api/applications/**").hasRole("HR_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/candidates/register", "/api/candidates/login").permitAll()
                        .requestMatchers("/api/candidates/**").hasRole("CANDIDATE")

                        .requestMatchers("/api/employees/**").hasRole("HR_ADMIN")
                        .requestMatchers("/api/todos/assign", "/api/todos/employee/**").hasRole("HR_ADMIN")
                        .requestMatchers("/api/dashboard/admin").hasRole("HR_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/leave").hasRole("HR_ADMIN")
                        .requestMatchers("/api/leave/*/decision").hasRole("HR_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/onboarding/faqs").hasRole("HR_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/policy/documents").hasRole("HR_ADMIN")

                        // Everything else (leave, dashboard, onboarding, policy)
                        // is staff-only: a logged-in candidate gets 403 here.
                        .requestMatchers("/api/**").hasAnyRole("EMPLOYEE", "HR_ADMIN")
                        .anyRequest().permitAll()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}