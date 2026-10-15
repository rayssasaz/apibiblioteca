package com.apim.apibiblioteca.config;

import com.apim.apibiblioteca.model.Usuario;
import com.apim.apibiblioteca.model.enumerations.Role;
import com.apim.apibiblioteca.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initUsers(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!usuarioRepository.existsByEmail("bibliotecario@biblioteca.edu.br")) {
                usuarioRepository.save(Usuario.builder()
                        .nome("Bibliotecário Chefe")
                        .email("bibliotecario@biblioteca.edu.br")
                        .senha(passwordEncoder.encode("senha1234"))
                        .role(Role.BIBLIOTECARIO)
                        .build());
            }

            if (!usuarioRepository.existsByEmail("leitor@biblioteca.edu.br")) {
                usuarioRepository.save(Usuario.builder()
                        .nome("João Leitor")
                        .email("leitor@biblioteca.edu.br")
                        .senha(passwordEncoder.encode("senha1234"))
                        .role(Role.LEITOR)
                        .build());
            }

            if (!usuarioRepository.existsByEmail("maria@biblioteca.edu.br")) {
                usuarioRepository.save(Usuario.builder()
                        .nome("Maria Leitora")
                        .email("maria@biblioteca.edu.br")
                        .senha(passwordEncoder.encode("senha1234"))
                        .role(Role.LEITOR)
                        .build());
            }
        };
    }
}
