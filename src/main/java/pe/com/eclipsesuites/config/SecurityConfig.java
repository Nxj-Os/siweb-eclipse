package pe.com.eclipsesuites.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import pe.com.eclipsesuites.service.IEmpleadoAuthService;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Cadena REST para /api/**: responde JSON (401/403) y mantiene CSRF activo.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http,
                                                      IEmpleadoAuthService empleadoAuthService)
            throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/client/login",
                                "/api/auth/client/register",
                                "/api/auth/client/disponibilidad",
                                "/api/auth/csrf").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(apiAuthenticationEntryPoint())
                        .accessDeniedHandler(apiAccessDeniedHandler()))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) ->
                                escribirJson(response, HttpServletResponse.SC_OK,
                                        "{\"mensaje\":\"Sesión cerrada\"}")))
                .userDetailsService(empleadoAuthService);
        return http.build();
    }

    /**
     * Cadena web (Thymeleaf): form login con página /login y CSRF activo.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http,
                                                      IEmpleadoAuthService empleadoAuthService)
            throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/login", "/registro", "/error", "/css/**", "/js/**",
                                "/images/**", "/img/**", "/webjars/**", "/favicon.ico").permitAll()
                        .requestMatchers("/cliente/**").hasRole("CLIENTE")
                        .requestMatchers("/admin/**", "/reportes/**").hasRole("ADMIN")
                        .requestMatchers("/recepcion/**").hasAnyRole("RECEPCION", "ADMIN")
                        .requestMatchers("/roomservice/**").hasAnyRole("ROOMSERVICE", "ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/login").permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout"))
                .userDetailsService(empleadoAuthService);
        return http.build();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    private static final class SpaCsrfTokenRequestHandler
            implements org.springframework.security.web.csrf.CsrfTokenRequestHandler {

        private final org.springframework.security.web.csrf.CsrfTokenRequestHandler plainHandler =
                new org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler();

        @Override
        public void handle(jakarta.servlet.http.HttpServletRequest request,
                           jakarta.servlet.http.HttpServletResponse response,
                           java.util.function.Supplier<org.springframework.security.web.csrf.CsrfToken>
                                   deferredCsrfToken) {
            plainHandler.handle(request, response, deferredCsrfToken);
            deferredCsrfToken.get().getToken();
        }

        @Override
        public String resolveCsrfTokenValue(jakarta.servlet.http.HttpServletRequest request,
                                             org.springframework.security.web.csrf.CsrfToken csrfToken) {
            return plainHandler.resolveCsrfTokenValue(request, csrfToken);
        }
    }

    private AuthenticationEntryPoint apiAuthenticationEntryPoint() {
        return (request, response, exception) ->
                escribirJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "{\"timestamp\":\"" + OffsetDateTime.now() + "\",\"status\":401,"
                                + "\"error\":\"No autenticado\","
                                + "\"mensaje\":\"Debe iniciar sesión para acceder a este recurso\"}");
    }

    private AccessDeniedHandler apiAccessDeniedHandler() {
        return (request, response, exception) ->
                escribirJson(response, HttpServletResponse.SC_FORBIDDEN,
                        "{\"timestamp\":\"" + OffsetDateTime.now() + "\",\"status\":403,"
                                + "\"error\":\"Acceso denegado\","
                                + "\"mensaje\":\"No tiene permisos para acceder a este recurso\"}");
    }

    private void escribirJson(HttpServletResponse response, int status, String json)
            throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(json);
    }
}
