package com.apim.apibiblioteca.controller;

import com.apim.apibiblioteca.dto.auth.LoginRequestDTO;
import com.apim.apibiblioteca.dto.auth.LoginResponseDTO;
import com.apim.apibiblioteca.dto.auth.RegisterRequestDTO;
import com.apim.apibiblioteca.dto.auth.UsuarioResponseDTO;
import com.apim.apibiblioteca.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "1. Autenticação", description = "Endpoints para cadastro de leitores (RF01) e login com emissão de token JWT (RF02)")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Operation(summary = "Cadastrar novo leitor (RF01)", description = "Cadastra um novo usuário com perfil LEITOR, e-mail único e senha criptografada com BCrypt (mínimo 8 caracteres).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Leitor cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos"),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado no sistema")
    })
    @PostMapping("/register")
    public ResponseEntity<UsuarioResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        UsuarioResponseDTO response = authenticationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Autenticar usuário / Login (RF02)", description = "Autentica leitor ou bibliotecário por e-mail e senha e retorna o token JWT.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Autenticação realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Campos obrigatórios ausentes ou inválidos"),
            @ApiResponse(responseCode = "401", description = "E-mail ou senha inválidos")
    })
    @PostMapping({"/login", "/authenticate"})
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        LoginResponseDTO response = authenticationService.login(request);
        return ResponseEntity.ok(response);
    }
}
