
package com.sneakerhouse.backend.config;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    // Mã hóa mật khẩu bằng BCrypt
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Lưu thông tin đăng nhập vào Session
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    // Provider xác thực email và mật khẩu
    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(passwordEncoder);

        provider.setUserDetailsService(userDetailsService);

        return provider;
    }

    // AuthenticationManager sử dụng trực tiếp DaoAuthenticationProvider
    @Bean
    public AuthenticationManager authenticationManager(
            DaoAuthenticationProvider authenticationProvider
    ) {
        return new ProviderManager(List.of(authenticationProvider));
    }

    // Cấu hình bảo mật
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DaoAuthenticationProvider authenticationProvider,
            SecurityContextRepository securityContextRepository
    ) throws Exception {

        CookieCsrfTokenRepository csrfRepository =
                CookieCsrfTokenRepository.withHttpOnlyFalse();

        CsrfTokenRequestAttributeHandler csrfHandler =
                new CsrfTokenRequestAttributeHandler();

        http
            // CORS
            .cors(cors -> cors
                    .configurationSource(corsConfigurationSource())
            )

            // CSRF: giữ bảo vệ cho các API thông thường
            .csrf(csrf -> csrf
                    .csrfTokenRepository(csrfRepository)
                    .csrfTokenRequestHandler(csrfHandler)
                    .ignoringRequestMatchers(
                            "/api/payments/payos/webhook"
                    )
            )

            // Provider xác thực
            .authenticationProvider(authenticationProvider)

            // Lưu SecurityContext vào HTTP Session
            .securityContext(context -> context
                    .securityContextRepository(securityContextRepository)
            )

            // Session
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )

            // Xử lý lỗi bảo mật
            .exceptionHandling(exception -> exception
                    .authenticationEntryPoint(
                            new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                    )
                    .accessDeniedHandler((request, response, ex) ->
                            response.sendError(
                                    HttpServletResponse.SC_FORBIDDEN
                            )
                    )
            )

            // Phân quyền API
            .authorizeHttpRequests(auth -> auth

                    // PayOS webhook
                    .requestMatchers(
                            HttpMethod.POST,
                            "/api/payments/payos/webhook"
                    ).permitAll()

                    // API xác thực công khai
                    .requestMatchers(
                            "/api/auth/csrf",
                            "/api/auth/register",
                            "/api/auth/login",
                            "/api/auth/google",
                            "/api/auth/forgot-password",
                            "/api/auth/verify-otp",
                            "/api/auth/resend-otp",
                            "/api/auth/reset-password"
                    ).permitAll()

                    // Thông tin tài khoản
                    .requestMatchers(
                            HttpMethod.GET,
                            "/api/auth/me"
                    ).authenticated()

                    // Đăng xuất
                    .requestMatchers(
                            HttpMethod.POST,
                            "/api/auth/logout"
                    ).authenticated()

                    // API quản trị
                    .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")

                    // API công khai
                    .requestMatchers(
                            HttpMethod.GET,
                            "/api/coupons/**",
                            "/api/banners/**",
                            "/api/home/**",
                            "/api/categories/**",
                            "/api/brands/**",
                            "/api/products/**",
                            "/api/flash-sales/**",
                            "/api/collections/**",
                            "/api/news/**"
                    ).permitAll()

                    // Những API còn lại yêu cầu đăng nhập
                    .anyRequest().authenticated()
            )

            // Không sử dụng form login và HTTP Basic
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    // Cấu hình CORS
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://localhost:5500",
                "http://127.0.0.1:5500",
                "http://localhost:5501",
                "http://127.0.0.1:5501"
        ));

        config.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        config.setAllowedHeaders(List.of(
                "Content-Type",
                "X-XSRF-TOKEN",
                "X-Requested-With",
                "Accept",
                "Authorization"
        ));

        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
