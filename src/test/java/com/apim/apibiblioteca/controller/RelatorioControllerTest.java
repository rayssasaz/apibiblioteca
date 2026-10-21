package com.apim.apibiblioteca.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RelatorioControllerTest extends BaseControllerTest {

    @Test
    @DisplayName("RF15 - Bibliotecário deve acessar relatório dos livros mais emprestados (200 OK)")
    void deveAcessarRelatorioLivrosMaisEmprestadosQuandoBibliotecario() throws Exception {
        mockMvc.perform(get("/api/relatorios/livros-mais-emprestados")
                        .header("Authorization", "Bearer " + getBibliotecarioToken())
                        .param("inicio", LocalDate.now().minusMonths(1).toString())
                        .param("fim", LocalDate.now().toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("RF15 - Leitor deve receber 403 Forbidden ao tentar acessar relatório de livros mais emprestados")
    void deveRetornarForbiddenEmLivrosMaisEmprestadosComoLeitor() throws Exception {
        mockMvc.perform(get("/api/relatorios/livros-mais-emprestados")
                        .header("Authorization", "Bearer " + getLeitorToken())
                        .param("inicio", LocalDate.now().minusMonths(1).toString())
                        .param("fim", LocalDate.now().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RF15 - Requisição sem token deve receber 401 Unauthorized ao acessar relatório")
    void deveRetornarUnauthorizedEmLivrosMaisEmprestadosSemToken() throws Exception {
        mockMvc.perform(get("/api/relatorios/livros-mais-emprestados")
                        .param("inicio", LocalDate.now().minusMonths(1).toString())
                        .param("fim", LocalDate.now().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RF16 - Bibliotecário deve acessar relatório de empréstimos em atraso (200 OK)")
    void deveAcessarRelatorioEmprestimosAtrasadosQuandoBibliotecario() throws Exception {
        mockMvc.perform(get("/api/relatorios/emprestimos-atrasados")
                        .header("Authorization", "Bearer " + getBibliotecarioToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("RF16 - Leitor deve receber 403 Forbidden ao tentar acessar relatório de empréstimos em atraso")
    void deveRetornarForbiddenEmEmprestimosAtrasadosComoLeitor() throws Exception {
        mockMvc.perform(get("/api/relatorios/emprestimos-atrasados")
                        .header("Authorization", "Bearer " + getLeitorToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RF17 - Bibliotecário deve acessar relatório de multas pendentes por leitor (200 OK)")
    void deveAcessarRelatorioMultasPendentesQuandoBibliotecario() throws Exception {
        mockMvc.perform(get("/api/relatorios/multas-pendentes-por-leitor")
                        .header("Authorization", "Bearer " + getBibliotecarioToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("RF17 - Leitor deve receber 403 Forbidden ao tentar acessar relatório de multas pendentes por leitor")
    void deveRetornarForbiddenEmMultasPendentesComoLeitor() throws Exception {
        mockMvc.perform(get("/api/relatorios/multas-pendentes-por-leitor")
                        .header("Authorization", "Bearer " + getLeitorToken()))
                .andExpect(status().isForbidden());
    }
}
