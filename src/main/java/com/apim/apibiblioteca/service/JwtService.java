package com.apim.apibiblioteca.service;

import com.apim.apibiblioteca.model.Usuario;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;

    public JwtService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public String generateToken(Usuario usuario) {
        Instant now = Instant.now();
        long expire = 3600L;

        var claims = JwtClaimsSet.builder()
                .issuer("spring-security")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expire))
                .subject(usuario.getEmail())
                .claim("userId", usuario.getId())
                .claim("role", usuario.getRole().name())
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
