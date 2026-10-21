package com.apim.apibiblioteca.controller;

import com.apim.apibiblioteca.dto.exemplar.ExemplarRequestDTO;
import com.apim.apibiblioteca.dto.exemplar.IndisponibilizarExemplarDTO;
import com.apim.apibiblioteca.model.Exemplar;
import com.apim.apibiblioteca.model.Livro;
import com.apim.apibiblioteca.model.enumerations.StatusExemplar;
import com.apim.apibiblioteca.repository.ExemplarRepository;
import com.apim.apibiblioteca.repository.LivroRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExemplarControllerTest extends BaseControllerTest {

    @Autowired
    private LivroRepository livroRepository;

    @Autowired
    private ExemplarRepository exemplarRepository;

    @Test
    @DisplayName("RF04 - Bibliotecário deve conseguir cadastrar novo exemplar com sucesso (201 Created)")
    void deveCadastrarExemplarQuandoBibliotecario() throws Exception {
        Livro livro = livroRepository.save(Livro.builder()
                .titulo("Design Patterns")
                .autor("GoF")
                .isbn("978-0201633610")
                .categoria("Tecnologia")
                .anoPublicacao(1994)
                .build());

        ExemplarRequestDTO request = new ExemplarRequestDTO("PAT-EX-001");

        mockMvc.perform(post("/api/livros/{livroId}/exemplares", livro.getId())
                        .header("Authorization", "Bearer " + getBibliotecarioToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoPatrimonio", is("PAT-EX-001")))
                .andExpect(jsonPath("$.status", is("DISPONIVEL")));
    }

    @Test
    @DisplayName("RF04 - Leitor deve receber 403 Forbidden ao tentar cadastrar exemplar")
    void deveRetornarForbiddenAoCadastrarExemplarComoLeitor() throws Exception {
        Livro livro = livroRepository.save(Livro.builder()
                .titulo("Algorithms")
                .autor("Sedgewick")
                .isbn("978-0321573513")
                .categoria("Tecnologia")
                .anoPublicacao(2011)
                .build());

        ExemplarRequestDTO request = new ExemplarRequestDTO("PAT-EX-002");

        mockMvc.perform(post("/api/livros/{livroId}/exemplares", livro.getId())
                        .header("Authorization", "Bearer " + getLeitorToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RF04 - Bibliotecário deve listar todos os exemplares de um livro")
    void deveListarExemplaresDeUmLivro() throws Exception {
        Livro livro = livroRepository.save(Livro.builder()
                .titulo("Estruturas de Dados")
                .autor("Goodrich")
                .isbn("978-8582600184")
                .categoria("Tecnologia")
                .anoPublicacao(2013)
                .build());

        exemplarRepository.save(Exemplar.builder()
                .livro(livro)
                .codigoPatrimonio("PAT-ED-01")
                .status(StatusExemplar.DISPONIVEL)
                .build());

        mockMvc.perform(get("/api/livros/{livroId}/exemplares", livro.getId())
                        .header("Authorization", "Bearer " + getBibliotecarioToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].codigoPatrimonio", is("PAT-ED-01")));
    }

    @Test
    @DisplayName("RF04 - Bibliotecário deve conseguir alterar status para INDISPONIVEL")
    void deveIndisponibilizarExemplar() throws Exception {
        Livro livro = livroRepository.save(Livro.builder()
                .titulo("Redes de Computadores")
                .autor("Tanenbaum")
                .isbn("978-8576059240")
                .categoria("Tecnologia")
                .anoPublicacao(2011)
                .build());

        Exemplar exemplar = exemplarRepository.save(Exemplar.builder()
                .livro(livro)
                .codigoPatrimonio("PAT-REDES-01")
                .status(StatusExemplar.DISPONIVEL)
                .build());

        IndisponibilizarExemplarDTO dto = new IndisponibilizarExemplarDTO("Páginas rasgadas em reparo");

        mockMvc.perform(patch("/api/exemplares/{id}/indisponibilizar", exemplar.getId())
                        .header("Authorization", "Bearer " + getBibliotecarioToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("INDISPONIVEL")))
                .andExpect(jsonPath("$.motivoIndisponibilidade", is("Páginas rasgadas em reparo")));
    }

    @Test
    @DisplayName("RF04 - Bibliotecário deve conseguir retornar exemplar INDISPONIVEL para DISPONIVEL")
    void deveDisponibilizarExemplar() throws Exception {
        Livro livro = livroRepository.save(Livro.builder()
                .titulo("Sistemas Operacionais Modernos")
                .autor("Tanenbaum")
                .isbn("978-8543005676")
                .categoria("Tecnologia")
                .anoPublicacao(2015)
                .build());

        Exemplar exemplar = exemplarRepository.save(Exemplar.builder()
                .livro(livro)
                .codigoPatrimonio("PAT-SO-01")
                .status(StatusExemplar.INDISPONIVEL)
                .motivoIndisponibilidade("Em manutenção")
                .build());

        mockMvc.perform(patch("/api/exemplares/{id}/disponibilizar", exemplar.getId())
                        .header("Authorization", "Bearer " + getBibliotecarioToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DISPONIVEL")));
    }

    @Test
    @DisplayName("RF04 - Bibliotecário deve conseguir buscar exemplar por ID")
    void deveBuscarExemplarPorIdQuandoBibliotecario() throws Exception {
        Livro livro = livroRepository.save(Livro.builder()
                .titulo("Compiladores")
                .autor("Aho")
                .isbn("978-8577800889")
                .categoria("Tecnologia")
                .anoPublicacao(2008)
                .build());

        Exemplar exemplar = exemplarRepository.save(Exemplar.builder()
                .livro(livro)
                .codigoPatrimonio("PAT-COMP-01")
                .status(StatusExemplar.DISPONIVEL)
                .build());

        mockMvc.perform(get("/api/exemplares/{id}", exemplar.getId())
                        .header("Authorization", "Bearer " + getBibliotecarioToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(exemplar.getId().intValue())))
                .andExpect(jsonPath("$.codigoPatrimonio", is("PAT-COMP-01")));
    }
}
