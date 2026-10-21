package com.apim.apibiblioteca.controller;

import com.apim.apibiblioteca.dto.emprestimo.EmprestimoRequestDTO;
import com.apim.apibiblioteca.model.Emprestimo;
import com.apim.apibiblioteca.model.Exemplar;
import com.apim.apibiblioteca.model.Livro;
import com.apim.apibiblioteca.model.Usuario;
import com.apim.apibiblioteca.model.enumerations.StatusEmprestimo;
import com.apim.apibiblioteca.model.enumerations.StatusExemplar;
import com.apim.apibiblioteca.repository.EmprestimoRepository;
import com.apim.apibiblioteca.repository.ExemplarRepository;
import com.apim.apibiblioteca.repository.LivroRepository;
import com.apim.apibiblioteca.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EmprestimoControllerTest extends BaseControllerTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LivroRepository livroRepository;

    @Autowired
    private ExemplarRepository exemplarRepository;

    @Autowired
    private EmprestimoRepository emprestimoRepository;

    @Test
    @DisplayName("RF06 - Bibliotecário deve conseguir registrar empréstimo de exemplar disponível (201 Created)")
    void deveRegistrarEmprestimoQuandoBibliotecario() throws Exception {
        Usuario leitor = usuarioRepository.findByEmail("leitor@biblioteca.edu.br").orElseThrow();

        Livro livro = livroRepository.save(Livro.builder()
                .titulo("Domain Driven Design")
                .autor("Eric Evans")
                .isbn("978-0321125218")
                .categoria("TI")
                .anoPublicacao(2003)
                .build());

        Exemplar exemplar = exemplarRepository.save(Exemplar.builder()
                .livro(livro)
                .codigoPatrimonio("PAT-EMP-01")
                .status(StatusExemplar.DISPONIVEL)
                .build());

        EmprestimoRequestDTO request = new EmprestimoRequestDTO(exemplar.getId(), leitor.getId());

        mockMvc.perform(post("/api/emprestimos")
                        .header("Authorization", "Bearer " + getBibliotecarioToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.status", is("ATIVO")))
                .andExpect(jsonPath("$.dataPrevistaDevolucao", notNullValue()));
    }

    @Test
    @DisplayName("RF06 - Leitor deve receber 403 Forbidden ao tentar registrar empréstimo")
    void deveRetornarForbiddenAoRegistrarEmprestimoComoLeitor() throws Exception {
        EmprestimoRequestDTO request = new EmprestimoRequestDTO(1L, 1L);

        mockMvc.perform(post("/api/emprestimos")
                        .header("Authorization", "Bearer " + getLeitorToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RF08 - Leitor dono deve conseguir renovar seu próprio empréstimo ativo (200 OK)")
    void deveRenovarEmprestimoComoDono() throws Exception {
        Usuario leitor = usuarioRepository.findByEmail("leitor@biblioteca.edu.br").orElseThrow();

        Livro livro = livroRepository.save(Livro.builder()
                .titulo("Microservices Patterns")
                .autor("Chris Richardson")
                .isbn("978-1617294549")
                .categoria("TI")
                .anoPublicacao(2018)
                .build());

        Exemplar exemplar = exemplarRepository.save(Exemplar.builder()
                .livro(livro)
                .codigoPatrimonio("PAT-EMP-02")
                .status(StatusExemplar.EMPRESTADO)
                .build());

        Emprestimo emprestimo = emprestimoRepository.save(Emprestimo.builder()
                .leitor(leitor)
                .exemplar(exemplar)
                .dataEmprestimo(LocalDate.now())
                .dataPrevistaDevolucao(LocalDate.now().plusDays(14))
                .status(StatusEmprestimo.ATIVO)
                .renovado(false)
                .build());

        mockMvc.perform(patch("/api/emprestimos/{id}/renovar", emprestimo.getId())
                        .header("Authorization", "Bearer " + getLeitorToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.renovado", is(true)))
                .andExpect(jsonPath("$.dataPrevistaDevolucao", is(LocalDate.now().plusDays(21).toString())));
    }

    @Test
    @DisplayName("RF08 - Outro leitor deve receber 403 Forbidden ao tentar renovar empréstimo que não é seu")
    void deveRetornarForbiddenAoRenovarEmprestimoDeOutroLeitor() throws Exception {
        Usuario leitor = usuarioRepository.findByEmail("leitor@biblioteca.edu.br").orElseThrow();

        Livro livro = livroRepository.save(Livro.builder()
                .titulo("Building Microservices")
                .autor("Sam Newman")
                .isbn("978-1492034025")
                .categoria("TI")
                .anoPublicacao(2021)
                .build());

        Exemplar exemplar = exemplarRepository.save(Exemplar.builder()
                .livro(livro)
                .codigoPatrimonio("PAT-EMP-03")
                .status(StatusExemplar.EMPRESTADO)
                .build());

        Emprestimo emprestimo = emprestimoRepository.save(Emprestimo.builder()
                .leitor(leitor)
                .exemplar(exemplar)
                .dataEmprestimo(LocalDate.now())
                .dataPrevistaDevolucao(LocalDate.now().plusDays(14))
                .status(StatusEmprestimo.ATIVO)
                .renovado(false)
                .build());

        // Maria tenta renovar empréstimo do João
        mockMvc.perform(patch("/api/emprestimos/{id}/renovar", emprestimo.getId())
                        .header("Authorization", "Bearer " + getMariaToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RF07 - Bibliotecário deve conseguir registrar devolução de empréstimo (200 OK)")
    void deveRegistrarDevolucaoQuandoBibliotecario() throws Exception {
        Usuario leitor = usuarioRepository.findByEmail("leitor@biblioteca.edu.br").orElseThrow();

        Livro livro = livroRepository.save(Livro.builder()
                .titulo("Cloud Native Patterns")
                .autor("Cornelia Davis")
                .isbn("978-1617294297")
                .categoria("TI")
                .anoPublicacao(2019)
                .build());

        Exemplar exemplar = exemplarRepository.save(Exemplar.builder()
                .livro(livro)
                .codigoPatrimonio("PAT-EMP-04")
                .status(StatusExemplar.EMPRESTADO)
                .build());

        Emprestimo emprestimo = emprestimoRepository.save(Emprestimo.builder()
                .leitor(leitor)
                .exemplar(exemplar)
                .dataEmprestimo(LocalDate.now().minusDays(10))
                .dataPrevistaDevolucao(LocalDate.now().plusDays(4))
                .status(StatusEmprestimo.ATIVO)
                .renovado(false)
                .build());

        mockMvc.perform(patch("/api/emprestimos/{id}/devolucao", emprestimo.getId())
                        .header("Authorization", "Bearer " + getBibliotecarioToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DEVOLVIDO")))
                .andExpect(jsonPath("$.dataDevolucao", notNullValue()));
    }

    @Test
    @DisplayName("RF10 - Leitor deve conseguir consultar apenas os seus próprios empréstimos")
    void deveListarMeusEmprestimos() throws Exception {
        mockMvc.perform(get("/api/emprestimos/meus")
                        .header("Authorization", "Bearer " + getLeitorToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("RF10 - Bibliotecário deve conseguir consultar empréstimos com filtros")
    void deveListarTodosEmprestimosQuandoBibliotecario() throws Exception {
        mockMvc.perform(get("/api/emprestimos")
                        .header("Authorization", "Bearer " + getBibliotecarioToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("RF10 - Leitor deve receber 403 Forbidden ao tentar acessar lista geral de empréstimos")
    void deveRetornarForbiddenAoAcessarListaGeralComoLeitor() throws Exception {
        mockMvc.perform(get("/api/emprestimos")
                        .header("Authorization", "Bearer " + getLeitorToken()))
                .andExpect(status().isForbidden());
    }
}
