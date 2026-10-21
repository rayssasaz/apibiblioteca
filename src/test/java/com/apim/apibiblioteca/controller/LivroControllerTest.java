package com.apim.apibiblioteca.controller;

import com.apim.apibiblioteca.dto.livro.LivroRequestDTO;
import com.apim.apibiblioteca.model.Livro;
import com.apim.apibiblioteca.repository.LivroRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LivroControllerTest extends BaseControllerTest {

    @Autowired
    private LivroRepository livroRepository;

    @Test
    @DisplayName("RF05 - Deve permitir busca paginada de livros sem autenticação (endpoint público)")
    void deveBuscarLivrosPublicamente() throws Exception {
        mockMvc.perform(get("/api/livros")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()))
                .andExpect(jsonPath("$.page", is(0)));
    }

    @Test
    @DisplayName("RF05 - Deve buscar livro existente por ID publicamente")
    void deveBuscarLivroPorIdPublicamente() throws Exception {
        Livro livro = livroRepository.save(Livro.builder()
                .titulo("Engenharia de Software Moderna")
                .autor("Marco Tulio Valente")
                .isbn("978-65-00-01950-6")
                .categoria("Computação")
                .anoPublicacao(2020)
                .build());

        mockMvc.perform(get("/api/livros/{id}", livro.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(livro.getId().intValue())))
                .andExpect(jsonPath("$.titulo", is("Engenharia de Software Moderna")))
                .andExpect(jsonPath("$.isbn", is("978-65-00-01950-6")));
    }

    @Test
    @DisplayName("RF05 - Deve retornar 404 ao buscar livro inexistente")
    void deveRetornarNotFoundAoBuscarLivroInexistente() throws Exception {
        mockMvc.perform(get("/api/livros/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("RF03 - Bibliotecário deve conseguir cadastrar novo livro com sucesso (201 Created)")
    void deveCriarLivroQuandoBibliotecario() throws Exception {
        LivroRequestDTO request = new LivroRequestDTO(
                "Clean Code", "Robert C. Martin", "978-0132350884", "Tecnologia", 2008);

        mockMvc.perform(post("/api/livros")
                        .header("Authorization", "Bearer " + getBibliotecarioToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.titulo", is("Clean Code")))
                .andExpect(jsonPath("$.isbn", is("978-0132350884")));
    }

    @Test
    @DisplayName("RF03 - Leitor deve receber 403 Forbidden ao tentar cadastrar livro")
    void deveRetornarForbiddenAoCriarLivroComoLeitor() throws Exception {
        LivroRequestDTO request = new LivroRequestDTO(
                "Domain-Driven Design", "Eric Evans", "978-0321125217", "Tecnologia", 2003);

        mockMvc.perform(post("/api/livros")
                        .header("Authorization", "Bearer " + getLeitorToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RF03 - Requisição anônima deve receber 401 Unauthorized ao cadastrar livro")
    void deveRetornarUnauthorizedAoCriarLivroSemToken() throws Exception {
        LivroRequestDTO request = new LivroRequestDTO(
                "Refactoring", "Martin Fowler", "978-0201485677", "Tecnologia", 1999);

        mockMvc.perform(post("/api/livros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RF03 - Bibliotecário deve conseguir atualizar livro com sucesso (200 OK)")
    void deveAtualizarLivroQuandoBibliotecario() throws Exception {
        Livro livro = livroRepository.save(Livro.builder()
                .titulo("Java Como Programar Antigo")
                .autor("Deitel")
                .isbn("978-8576050193")
                .categoria("Computação")
                .anoPublicacao(2003)
                .build());

        LivroRequestDTO request = new LivroRequestDTO(
                "Java Como Programar 10ª Edição", "Paul Deitel", "978-8576050193", "Computação", 2016);

        mockMvc.perform(put("/api/livros/{id}", livro.getId())
                        .header("Authorization", "Bearer " + getBibliotecarioToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo", is("Java Como Programar 10ª Edição")))
                .andExpect(jsonPath("$.autor", is("Paul Deitel")));
    }

    @Test
    @DisplayName("RF03 - Leitor deve receber 403 Forbidden ao tentar atualizar livro")
    void deveRetornarForbiddenAoAtualizarLivroComoLeitor() throws Exception {
        Livro livro = livroRepository.save(Livro.builder()
                .titulo("Livro Teste Atualizacao")
                .autor("Autor Teste")
                .isbn("978-1111111111")
                .categoria("Geral")
                .anoPublicacao(2021)
                .build());

        LivroRequestDTO request = new LivroRequestDTO(
                "Livro Modificado", "Autor Teste", "978-1111111111", "Geral", 2021);

        mockMvc.perform(put("/api/livros/{id}", livro.getId())
                        .header("Authorization", "Bearer " + getLeitorToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RF03 - Bibliotecário deve conseguir remover livro sem pendências (204 No Content)")
    void deveRemoverLivroSemPendenciasQuandoBibliotecario() throws Exception {
        Livro livro = livroRepository.save(Livro.builder()
                .titulo("Livro a Ser Deletado")
                .autor("Autor Temporario")
                .isbn("978-2222222222")
                .categoria("Temporario")
                .anoPublicacao(2020)
                .build());

        mockMvc.perform(delete("/api/livros/{id}", livro.getId())
                        .header("Authorization", "Bearer " + getBibliotecarioToken()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("RF03 - Leitor deve receber 403 Forbidden ao tentar remover livro")
    void deveRetornarForbiddenAoRemoverLivroComoLeitor() throws Exception {
        Livro livro = livroRepository.save(Livro.builder()
                .titulo("Livro Protegido")
                .autor("Autor Protegido")
                .isbn("978-3333333333")
                .categoria("Protegido")
                .anoPublicacao(2020)
                .build());

        mockMvc.perform(delete("/api/livros/{id}", livro.getId())
                        .header("Authorization", "Bearer " + getLeitorToken()))
                .andExpect(status().isForbidden());
    }
}
