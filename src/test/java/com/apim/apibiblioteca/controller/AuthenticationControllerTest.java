package com.apim.apibiblioteca.controller;

import com.apim.apibiblioteca.dto.auth.LoginRequestDTO;
import com.apim.apibiblioteca.dto.auth.RegisterRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("RF01 - Deve cadastrar um novo leitor com sucesso e retornar status 201")
    void deveCadastrarNovoLeitorComSucesso() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO("Carlos Santos", "carlos.santos@biblioteca.edu.br", "senhaForte123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.nome", is("Carlos Santos")))
                .andExpect(jsonPath("$.email", is("carlos.santos@biblioteca.edu.br")))
                .andExpect(jsonPath("$.role", is("LEITOR")));
    }

    @Test
    @DisplayName("RF01 - Deve retornar 409 Conflict ao tentar cadastrar e-mail já existente")
    void deveRetornarConflictAoCadastrarEmailExistente() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO("João Repetido", "leitor@biblioteca.edu.br", "senha1234");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    @DisplayName("RF01 - Deve retornar 400 Bad Request ao informar dados inválidos ou senha curta")
    void deveRetornarBadRequestAoInformarDadosInvalidos() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO("", "emailinvalido", "123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("RF02 - Deve autenticar com credenciais válidas e retornar token JWT e status 200")
    void deveAutenticarComCredenciaisValidas() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("bibliotecario@biblioteca.edu.br", "senha1234");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.tipo", is("Bearer")))
                .andExpect(jsonPath("$.email", is("bibliotecario@biblioteca.edu.br")))
                .andExpect(jsonPath("$.role", is("BIBLIOTECARIO")));
    }

    @Test
    @DisplayName("RF02 - Deve retornar 401 Unauthorized ao informar senha incorreta")
    void deveRetornarUnauthorizedAoInformarSenhaIncorreta() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("bibliotecario@biblioteca.edu.br", "senhaErrada999");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    @DisplayName("RF02 - Deve retornar 401 Unauthorized ao informar e-mail inexistente")
    void deveRetornarUnauthorizedAoInformarEmailInexistente() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("inexistente@biblioteca.edu.br", "senha1234");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("RF02 - Deve retornar 400 Bad Request ao enviar corpo vazio no login")
    void deveRetornarBadRequestAoEnviarCorpoVazioNoLogin() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("", "");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }
}
