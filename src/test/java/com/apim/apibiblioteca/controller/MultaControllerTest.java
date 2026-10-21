package com.apim.apibiblioteca.controller;

import com.apim.apibiblioteca.model.Emprestimo;
import com.apim.apibiblioteca.model.Exemplar;
import com.apim.apibiblioteca.model.Livro;
import com.apim.apibiblioteca.model.Multa;
import com.apim.apibiblioteca.model.Usuario;
import com.apim.apibiblioteca.model.enumerations.Role;
import com.apim.apibiblioteca.model.enumerations.StatusEmprestimo;
import com.apim.apibiblioteca.model.enumerations.StatusExemplar;
import com.apim.apibiblioteca.model.enumerations.StatusMulta;
import com.apim.apibiblioteca.repository.EmprestimoRepository;
import com.apim.apibiblioteca.repository.ExemplarRepository;
import com.apim.apibiblioteca.repository.LivroRepository;
import com.apim.apibiblioteca.repository.MultaRepository;
import com.apim.apibiblioteca.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MultaControllerTest extends BaseControllerTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LivroRepository livroRepository;

    @Autowired
    private ExemplarRepository exemplarRepository;

    @Autowired
    private EmprestimoRepository emprestimoRepository;

    @Autowired
    private MultaRepository multaRepository;

    private Emprestimo criarEmprestimoParaMulta(String sufixo) {
        Usuario leitorMulta = usuarioRepository.save(Usuario.builder()
                .nome("Leitor Multa " + sufixo)
                .email("multa." + sufixo.toLowerCase().replaceAll("[^a-z0-9]", "") + "@biblioteca.edu.br")
                .senha("senha1234")
                .role(Role.LEITOR)
                .build());

        Livro livro = livroRepository.save(Livro.builder()
                .titulo("Livro Multa " + sufixo)
                .autor("Autor Teste")
                .isbn("978-99" + sufixo)
                .categoria("TI")
                .anoPublicacao(2020)
                .build());

        Exemplar exemplar = exemplarRepository.save(Exemplar.builder()
                .livro(livro)
                .codigoPatrimonio("PAT-MULTA-" + sufixo)
                .status(StatusExemplar.EMPRESTADO)
                .build());

        return emprestimoRepository.save(Emprestimo.builder()
                .leitor(leitorMulta)
                .exemplar(exemplar)
                .dataEmprestimo(LocalDate.now().minusDays(20))
                .dataPrevistaDevolucao(LocalDate.now().minusDays(6))
                .status(StatusEmprestimo.ATIVO)
                .renovado(false)
                .build());
    }

    @Test
    @DisplayName("RF10 - Leitor deve consultar apenas suas próprias multas (200 OK)")
    void deveListarMinhasMultasComoLeitor() throws Exception {
        mockMvc.perform(get("/api/multas/minhas")
                        .header("Authorization", "Bearer " + getLeitorToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("RF10 - Bibliotecário deve conseguir listar multas gerais (200 OK)")
    void deveListarTodasMultasComoBibliotecario() throws Exception {
        mockMvc.perform(get("/api/multas")
                        .header("Authorization", "Bearer " + getBibliotecarioToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("RF10 - Leitor deve receber 403 Forbidden ao tentar listar multas gerais")
    void deveRetornarForbiddenAoListarTodasMultasComoLeitor() throws Exception {
        mockMvc.perform(get("/api/multas")
                        .header("Authorization", "Bearer " + getLeitorToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RF09 - Bibliotecário deve conseguir registrar pagamento de multa (200 OK)")
    void devePagarMultaQuandoBibliotecario() throws Exception {
        Emprestimo emprestimo = criarEmprestimoParaMulta("PAGAR01");

        Multa multa = multaRepository.save(Multa.builder()
                .emprestimo(emprestimo)
                .valor(BigDecimal.valueOf(5.00))
                .diasAtraso(5)
                .status(StatusMulta.PENDENTE)
                .dataGeracao(LocalDate.now().minusDays(5))
                .build());

        mockMvc.perform(patch("/api/multas/{id}/pagar", multa.getId())
                        .header("Authorization", "Bearer " + getBibliotecarioToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PAGA")))
                .andExpect(jsonPath("$.dataPagamento", notNullValue()));
    }

    @Test
    @DisplayName("RF09 - Leitor deve receber 403 Forbidden ao tentar registrar pagamento de multa")
    void deveRetornarForbiddenAoPagarMultaComoLeitor() throws Exception {
        Emprestimo emprestimo = criarEmprestimoParaMulta("PAGAR02");

        Multa multa = multaRepository.save(Multa.builder()
                .emprestimo(emprestimo)
                .valor(BigDecimal.valueOf(3.00))
                .diasAtraso(3)
                .status(StatusMulta.PENDENTE)
                .dataGeracao(LocalDate.now().minusDays(3))
                .build());

        mockMvc.perform(patch("/api/multas/{id}/pagar", multa.getId())
                        .header("Authorization", "Bearer " + getLeitorToken()))
                .andExpect(status().isForbidden());
    }
}
