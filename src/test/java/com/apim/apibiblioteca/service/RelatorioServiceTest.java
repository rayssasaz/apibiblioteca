package com.apim.apibiblioteca.service;

import com.apim.apibiblioteca.dto.relatorio.EmprestimoAtrasadoDTO;
import com.apim.apibiblioteca.dto.relatorio.LivroMaisEmprestadoDTO;
import com.apim.apibiblioteca.dto.relatorio.MultaPorLeitorDTO;
import com.apim.apibiblioteca.exception.BusinessRuleException;
import com.apim.apibiblioteca.model.Emprestimo;
import com.apim.apibiblioteca.model.Exemplar;
import com.apim.apibiblioteca.model.Livro;
import com.apim.apibiblioteca.model.Usuario;
import com.apim.apibiblioteca.model.enumerations.Role;
import com.apim.apibiblioteca.model.enumerations.StatusEmprestimo;
import com.apim.apibiblioteca.model.enumerations.StatusMulta;
import com.apim.apibiblioteca.repository.EmprestimoRepository;
import com.apim.apibiblioteca.repository.MultaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

    @Mock
    private EmprestimoRepository emprestimoRepository;

    @Mock
    private MultaRepository multaRepository;

    @InjectMocks
    private RelatorioService relatorioService;

    @Test
    @DisplayName("RF15 - Deve retornar ranking dos livros mais emprestados no período")
    void deveRetornarTop10LivrosMaisEmprestadosNoPeriodo() {
        LocalDate inicio = LocalDate.of(2026, 1, 1);
        LocalDate fim = LocalDate.of(2026, 12, 31);

        Livro livro = Livro.builder()
                .id(1L)
                .titulo("Clean Code")
                .autor("Robert C. Martin")
                .isbn("978-0132350884")
                .categoria("Engenharia de Software")
                .build();

        List<Object[]> linhas = List.<Object[]>of(new Object[]{livro, 12L});
        when(emprestimoRepository.findLivrosMaisEmprestadosNoPeriodo(inicio, fim, PageRequest.of(0, 10)))
                .thenReturn(linhas);

        List<LivroMaisEmprestadoDTO> resultado = relatorioService.relatorioLivrosMaisEmprestados(inicio, fim);

        assertEquals(1, resultado.size());
        assertEquals("Clean Code", resultado.get(0).getTitulo());
        assertEquals(12L, resultado.get(0).getTotalEmprestimos());
    }

    @Test
    @DisplayName("RF15 - Deve lançar BusinessRuleException quando data início for posterior à data fim")
    void deveLancarExcecaoQuandoDataInicioPosteriorADataFimNoRelatorio() {
        LocalDate inicio = LocalDate.of(2026, 10, 20);
        LocalDate fim = LocalDate.of(2026, 10, 10);

        assertThrows(BusinessRuleException.class,
                () -> relatorioService.relatorioLivrosMaisEmprestados(inicio, fim));
    }

    @Test
    @DisplayName("RF16 - Deve retornar empréstimos atrasados com dias de atraso e multa estimada de R$ 1,00/dia")
    void deveRetornarEmprestimosAtrasadosComDiasEMultaEstimada() {
        Usuario leitor = Usuario.builder().id(1L).nome("Pedro").email("pedro@bib.com").role(Role.LEITOR).build();
        Livro livro = Livro.builder().id(10L).titulo("Design Patterns").build();
        Exemplar exemplar = Exemplar.builder().id(100L).codigoPatrimonio("PAT-100").livro(livro).build();

        Emprestimo emprestimoAtrasado = Emprestimo.builder()
                .id(500L)
                .leitor(leitor)
                .exemplar(exemplar)
                .dataEmprestimo(LocalDate.now().minusDays(20))
                .dataPrevistaDevolucao(LocalDate.now().minusDays(6))
                .status(StatusEmprestimo.ATIVO)
                .build();

        when(emprestimoRepository.findByStatusAndDataPrevistaDevolucaoBeforeOrderByDataPrevistaDevolucaoAsc(
                eq(StatusEmprestimo.ATIVO), any(LocalDate.class)))
                .thenReturn(List.of(emprestimoAtrasado));

        List<EmprestimoAtrasadoDTO> resultado = relatorioService.relatorioEmprestimosAtrasados();

        assertEquals(1, resultado.size());
        assertEquals(6L, resultado.get(0).getDiasAtraso());
        assertEquals(new BigDecimal("6.00"), resultado.get(0).getMultaEstimada());
    }

    @Test
    @DisplayName("RF17 - Deve retornar total de multas pendentes agrupado por leitor")
    void deveRetornarTotalDeMultasPendentesAgrupadoPorLeitor() {
        Usuario leitor = Usuario.builder().id(1L).nome("Pedro").email("pedro@bib.com").role(Role.LEITOR).build();
        List<Object[]> linhas = List.<Object[]>of(new Object[]{leitor, 2L, new BigDecimal("15.00")});

        when(multaRepository.findTotalMultasPorLeitor(StatusMulta.PENDENTE)).thenReturn(linhas);

        List<MultaPorLeitorDTO> resultado = relatorioService.relatorioMultasPendentesPorLeitor();

        assertEquals(1, resultado.size());
        assertEquals(1L, resultado.get(0).getLeitorId());
        assertEquals(2L, resultado.get(0).getQuantidadeMultasPendentes());
        assertEquals(new BigDecimal("15.00"), resultado.get(0).getValorTotalPendente());
    }
}
