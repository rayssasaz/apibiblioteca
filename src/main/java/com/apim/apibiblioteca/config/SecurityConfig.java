package com.apim.apibiblioteca.config;

import com.apim.apibiblioteca.security.CustomJwtAuthenticationConverter;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${jwt.public.key}")
    private RSAPublicKey key;

    @Value("${jwt.private.key}")
    private RSAPrivateKey priv;

    @Bean
    public CustomJwtAuthenticationConverter customJwtAuthenticationConverter() {
        return new CustomJwtAuthenticationConverter();
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            CustomJwtAuthenticationConverter customJwtAuthenticationConverter) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Endpoints publicos: Autenticacao (RF01, RF02), Consulta do Acervo (RF05) e Swagger
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/livros", "/api/livros/*").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Endpoints exclusivos do BIBLIOTECARIO (RF03, RF04, RF06, RF07, RF09, RF10b, RF12, RF14, RF15-17)
                        .requestMatchers(HttpMethod.POST, "/api/livros", "/api/livros/*/exemplares").hasRole("BIBLIOTECARIO")
                        .requestMatchers(HttpMethod.PUT, "/api/livros/*").hasRole("BIBLIOTECARIO")
                        .requestMatchers(HttpMethod.DELETE, "/api/livros/*").hasRole("BIBLIOTECARIO")
                        .requestMatchers(HttpMethod.GET, "/api/livros/*/exemplares").hasRole("BIBLIOTECARIO")
                        .requestMatchers("/api/exemplares/**").hasRole("BIBLIOTECARIO")
                        .requestMatchers(HttpMethod.POST, "/api/emprestimos").hasRole("BIBLIOTECARIO")
                        .requestMatchers(HttpMethod.PATCH, "/api/emprestimos/*/devolucao").hasRole("BIBLIOTECARIO")
                        .requestMatchers(HttpMethod.GET, "/api/emprestimos").hasRole("BIBLIOTECARIO")
                        .requestMatchers(HttpMethod.PATCH, "/api/multas/*/pagar").hasRole("BIBLIOTECARIO")
                        .requestMatchers(HttpMethod.POST, "/api/reservas/*/retirar", "/api/reservas/processar-expiradas").hasRole("BIBLIOTECARIO")
                        .requestMatchers(HttpMethod.GET, "/api/reservas/livro/*/fila").hasRole("BIBLIOTECARIO")
                        .requestMatchers("/api/relatorios/**").hasRole("BIBLIOTECARIO")

                        // Endpoints exclusivos do LEITOR (RF08, RF10a, RF11)
                        .requestMatchers(HttpMethod.PATCH, "/api/emprestimos/*/renovar").hasRole("LEITOR")
                        .requestMatchers(HttpMethod.GET, "/api/emprestimos/meus", "/api/multas/minhas", "/api/reservas/minhas").hasRole("LEITOR")
                        .requestMatchers(HttpMethod.POST, "/api/reservas").hasRole("LEITOR")

                        // Demais endpoints autenticados (ex.: RF13 cancelamento de reserva por LEITOR ou BIBLIOTECARIO)
                        .anyRequest().authenticated())
                .oauth2ResourceServer(conf -> conf
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(customJwtAuthenticationConverter)))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withPublicKey(this.key).build();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        var jwk = new RSAKey.Builder(this.key).privateKey(this.priv).build();
        var jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwks);
    }
}
