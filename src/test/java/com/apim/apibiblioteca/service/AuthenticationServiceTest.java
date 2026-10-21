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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private AuthenticationService authenticationService;

    private Usuario usuarioLeitor;

    @BeforeEach
    void setUp() {
        usuarioLeitor = Usuario.builder()
                .id(1L)
                .nome("Ana Leitora")
                .email("ana@biblioteca.com")
                .senha("$2a$10$hashSenha")
                .role(Role.LEITOR)
                .build();
    }

    @Test
    @DisplayName("RF01 - Deve cadastrar leitor com sucesso e perfil LEITOR")
    void deveCadastrarLeitorComSucesso() {
        RegisterRequestDTO request = RegisterRequestDTO.builder()
                .nome("Ana Leitora")
                .email("ana@biblioteca.com")
                .senha("senha1234")
                .build();

        UsuarioResponseDTO responseEsperado = UsuarioResponseDTO.builder()
                .id(1L)
                .nome("Ana Leitora")
                .email("ana@biblioteca.com")
                .role(Role.LEITOR)
                .build();

        when(usuarioRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getSenha())).thenReturn("$2a$10$hashSenha");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioLeitor);
        when(modelMapper.map(usuarioLeitor, UsuarioResponseDTO.class)).thenReturn(responseEsperado);

        UsuarioResponseDTO resultado = authenticationService.register(request);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals(Role.LEITOR, resultado.getRole());
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    @DisplayName("RF01 - Deve impedir cadastro quando e-mail já estiver em uso")
    void deveLancarExcecaoAoCadastrarEmailDuplicado() {
        RegisterRequestDTO request = RegisterRequestDTO.builder()
                .nome("Ana Leitora")
                .email("ana@biblioteca.com")
                .senha("senha1234")
                .build();

        when(usuarioRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> authenticationService.register(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("RF02 - Deve autenticar com sucesso e retornar token JWT")
    void deveRealizarLoginComSucessoERetornarTokenJwt() {
        LoginRequestDTO request = LoginRequestDTO.builder()
                .email("ana@biblioteca.com")
                .senha("senha1234")
                .build();

        when(usuarioRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(usuarioLeitor));
        when(passwordEncoder.matches(request.getSenha(), usuarioLeitor.getSenha())).thenReturn(true);
        when(jwtService.generateToken(usuarioLeitor)).thenReturn("jwt-token-valido");

        LoginResponseDTO resultado = authenticationService.login(request);

        assertNotNull(resultado);
        assertEquals("jwt-token-valido", resultado.getToken());
        assertEquals("Bearer", resultado.getTipo());
        assertEquals(Role.LEITOR, resultado.getRole());
    }

    @Test
    @DisplayName("RF02 - Deve retornar mensagem genérica quando e-mail não existir no login")
    void deveLancarExcecaoComMensagemGenericaQuandoEmailNaoEncontradoNoLogin() {
        LoginRequestDTO request = LoginRequestDTO.builder()
                .email("inexistente@biblioteca.com")
                .senha("senha1234")
                .build();

        when(usuarioRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        InvalidCredentialsException ex = assertThrows(
                InvalidCredentialsException.class,
                () -> authenticationService.login(request));
        assertEquals("E-mail ou senha inválidos", ex.getMessage());
    }

    @Test
    @DisplayName("RF02 - Deve retornar a mesma mensagem genérica quando senha estiver incorreta")
    void deveLancarExcecaoComMensagemGenericaQuandoSenhaIncorretaNoLogin() {
        LoginRequestDTO request = LoginRequestDTO.builder()
                .email("ana@biblioteca.com")
                .senha("senhaErrada")
                .build();

        when(usuarioRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(usuarioLeitor));
        when(passwordEncoder.matches(request.getSenha(), usuarioLeitor.getSenha())).thenReturn(false);

        InvalidCredentialsException ex = assertThrows(
                InvalidCredentialsException.class,
                () -> authenticationService.login(request));
        assertEquals("E-mail ou senha inválidos", ex.getMessage());
    }
}
