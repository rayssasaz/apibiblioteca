package com.apim.apibiblioteca.controller;

import com.apim.apibiblioteca.dto.auth.LoginRequestDTO;
import com.apim.apibiblioteca.service.AuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class BaseControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected AuthenticationService authenticationService;

    protected final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    protected String getBibliotecarioToken() {
        return authenticationService.login(new LoginRequestDTO("bibliotecario@biblioteca.edu.br", "senha1234")).getToken();
    }

    protected String getLeitorToken() {
        return authenticationService.login(new LoginRequestDTO("leitor@biblioteca.edu.br", "senha1234")).getToken();
    }

    protected String getMariaToken() {
        return authenticationService.login(new LoginRequestDTO("maria@biblioteca.edu.br", "senha1234")).getToken();
    }
}
