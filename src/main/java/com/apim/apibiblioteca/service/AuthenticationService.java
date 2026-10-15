package com.apim.apibiblioteca.service;

import com.apim.apibiblioteca.dto.auth.LoginRequestDTO;
import com.apim.apibiblioteca.dto.auth.LoginResponseDTO;
import com.apim.apibiblioteca.dto.auth.RegisterRequestDTO;
import com.apim.apibiblioteca.dto.auth.UsuarioResponseDTO;
import com.apim.apibiblioteca.exception.BusinessRuleException;
import com.apim.apibiblioteca.exception.InvalidCredentialsException;
import com.apim.apibiblioteca.model.Usuario;
import com.apim.apibiblioteca.model.enumerations.Role;
import com.apim.apibiblioteca.repository.UsuarioRepository;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private static final String MENSAGEM_CREDENCIAIS_INVALIDAS = "E-mail ou senha inválidos";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ModelMapper modelMapper;

    public AuthenticationService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            ModelMapper modelMapper) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public UsuarioResponseDTO register(RegisterRequestDTO request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException("Já existe um usuário cadastrado com este e-mail");
        }

        Usuario novoUsuario = Usuario.builder()
                .nome(request.getNome())
                .email(request.getEmail())
                .senha(passwordEncoder.encode(request.getSenha()))
                .role(Role.LEITOR)
                .build();

        Usuario salvo = usuarioRepository.save(novoUsuario);
        return modelMapper.map(salvo, UsuarioResponseDTO.class);
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException(MENSAGEM_CREDENCIAIS_INVALIDAS));

        if (!passwordEncoder.matches(request.getSenha(), usuario.getSenha())) {
            throw new InvalidCredentialsException(MENSAGEM_CREDENCIAIS_INVALIDAS);
        }

        String token = jwtService.generateToken(usuario);

        return LoginResponseDTO.builder()
                .token(token)
                .tipo("Bearer")
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .role(usuario.getRole())
                .build();
    }
}
