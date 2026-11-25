package com.pulsepass.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;

// PRD Seguridad §7.1 y §14.3: declara cómo se protege cada ruta. No contiene negocio.
// @EnableMethodSecurity habilita @PreAuthorize/@PostAuthorize, que se usarán para
// propiedad y visibilidad de borradores (etapa 5, §11.1).
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   RestAuthenticationEntryPoint authenticationEntryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                // CSRF protege sesiones por cookie. Esta API usa Bearer y no cookies, así que
                // no aplica (§14.3 advertencia 6). Si algún día se usan cookies, hay que reactivarlo
                .csrf(AbstractHttpConfigurer::disable)
                // Stateless: no se crea HttpSession ni JSESSIONID (FR-SEC-AUTH-008)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Sin formulario de login, sin HTTP Basic y sin /logout: el login es POST /api/auth/login
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                // 401 y 403 con el mismo ErrorResponse del resto de la API (SEC-009)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                // Matriz del §11. Se evalúa de arriba abajo y gana la PRIMERA regla que coincide,
                // por eso las rutas específicas van antes que los patrones generales (§14.3 adv. 4).
                // "*" = un segmento de la URL; "**" = cualquier cantidad de segmentos.
                .authorizeHttpRequests(auth -> auth
                        // Registro y login públicos (FR-SEC-USR-01, §10.1)
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/users").permitAll()

                        // Lectura pública de venues y artistas (FR-SEC-VEN-01/02, FR-SEC-ART-01..03)
                        .requestMatchers(HttpMethod.GET, "/api/venues/**", "/api/artists/**").permitAll()

                        // Eventos
                        .requestMatchers(HttpMethod.GET, "/api/events/published").permitAll()                    // EVT-03
                        .requestMatchers(HttpMethod.GET, "/api/events/*/tickets/paid").hasAnyRole("STAFF", "ADMIN") // TKT-04
                        // EVT-02 y EVT-06 son públicas solo para eventos publicados. Esa condición depende
                        // del estado del evento, no de la URL: se aplica a nivel de método en la etapa 5
                        .requestMatchers(HttpMethod.GET, "/api/events/by-artist", "/api/events/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/events").hasRole("ADMIN")                       // EVT-01
                        .requestMatchers(HttpMethod.PATCH, "/api/events/*/publish").hasRole("ADMIN")            // EVT-04
                        .requestMatchers(HttpMethod.POST, "/api/events/*/artists/*").hasRole("ADMIN")           // EVT-05

                        // Tickets
                        .requestMatchers(HttpMethod.POST, "/api/tickets").hasRole("USER")                       // TKT-01
                        .requestMatchers(HttpMethod.PATCH, "/api/tickets/*/use").hasAnyRole("STAFF", "ADMIN")   // TKT-06

                        // Todo lo demás exige identidad: TKT-02, TKT-03, TKT-05, USR-02, USR-03 y /api/auth/me.
                        // La comprobación de propietario se agrega a nivel de método (etapa 5, §11.1).
                        // Denegar por defecto: una ruta nueva o mal escrita nunca queda pública (SEC-001)
                        .anyRequest().authenticated());

        return http.build();
    }

    // BCrypt con factor de costo 10 (valor por defecto). Genera una sal aleatoria por contraseña,
    // así que dos usuarios con la misma contraseña tienen hashes distintos (SEC-003).
    // Límite de BCrypt: solo usa los primeros 72 bytes (por eso el máximo del §9.3).
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Las tres clases siguientes no tienen @Component: se registran aquí para que importar
    // SecurityConfig (por ejemplo en un @WebMvcTest) traiga todo lo necesario de una vez
    @Bean
    public SecurityErrorWriter securityErrorWriter(JsonMapper jsonMapper) {
        return new SecurityErrorWriter(jsonMapper);
    }

    @Bean
    public RestAuthenticationEntryPoint restAuthenticationEntryPoint(SecurityErrorWriter securityErrorWriter) {
        return new RestAuthenticationEntryPoint(securityErrorWriter);
    }

    @Bean
    public RestAccessDeniedHandler restAccessDeniedHandler(SecurityErrorWriter securityErrorWriter) {
        return new RestAccessDeniedHandler(securityErrorWriter);
    }
}
