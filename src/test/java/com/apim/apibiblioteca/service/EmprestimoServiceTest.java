package com.apim.apibiblioteca.service;

import com.apim.apibiblioteca.dto.emprestimo.EmprestimoRequestDTO;
import com.apim.apibiblioteca.dto.emprestimo.EmprestimoResponseDTO;
import com.apim.apibiblioteca.exception.BusinessRuleException;
import com.apim.apibiblioteca.exception.UnauthorizedOperationException;
import com.apim.apibiblioteca.model.Emprestimo;
import com.apim.apibiblioteca.model.Exemplar;
import com.apim.apibiblioteca.model.Livro;
import com.apim.apibiblioteca.model.Multa;
import com.apim.apibiblioteca.model.Reserva;
import com.apim.apibiblioteca.model.Usuario;
import com.apim.apibiblioteca.model.enumerations.Role;
import com.apim.apibiblioteca.model.enumerations.StatusEmprestimo;
import com.apim.apibiblioteca.model.enumerations.StatusExemplar;
import com.apim.apibiblioteca.model.enumerations.StatusMulta;
import com.apim.apibiblioteca.model.enumerations.StatusReserva;
import com.apim.apibiblioteca.repository.EmprestimoRepository;
import com.apim.apibiblioteca.repository.ExemplarRepository;
import com.apim.apibiblioteca.repository.MultaRepository;
import com.apim.apibiblioteca.repository.ReservaRepository;
import com.apim.apibiblioteca.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmprestimoServiceTest {

    @Mock
    private EmprestimoRepository emprestimoRepository;

    @Mock
    private ExemplarRepository exemplarRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private MultaRepository multaRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @InjectMocks
    private EmprestimoService emprestimoService;

    private Usuario leitor;
    private Livro livro;
    private Exemplar exemplar;
    private Emprestimo emprestimoAtivo;

    @BeforeEach
    void setUp() {
        leitor = Usuario.builder()
                .id(1L)
                .nome("João Leitor")
                .email("joao@biblioteca.com")
                .role(Role.LEITOR)
                .build();

        livro = Livro.builder()
                .id(10L)
                .titulo("Refactoring")
                .autor("Martin Fowler")
                .isbn("978-0134757599")
                .categoria("Engenharia de Software")
                .anoPublicacao(2018)
                .build();

        exemplar = Exemplar.builder()
                .id(100L)
                .codigoPatrimonio("PAT-100")
                .status(StatusExemplar.DISPONIVEL)
                .livro(livro)
                .build();

        emprestimoAtivo = Emprestimo.builder()
                .id(500L)
                .exemplar(exemplar)
                .leitor(leitor)
                .dataEmprestimo(LocalDate.now())
                .dataPrevistaDevolucao(LocalDate.now().plusDays(14))
                .renovado(false)
                .status(StatusEmprestimo.ATIVO)
                .build();
    }

    @Test
    @DisplayName("RF06 - Deve registrar empréstimo com prazo de 14 dias e mudar exemplar para EMPRESTADO")
    void deveRegistrarEmprestimoComPrazoDe14Dias() {
        EmprestimoRequestDTO request = EmprestimoRequestDTO.builder().exemplarId(100L).leitorId(1L).build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(leitor));
        when(exemplarRepository.findById(100L)).thenReturn(Optional.of(exemplar));
        when(emprestimoRepository.existsByLeitorIdAndStatusAndDataPrevistaDevolucaoBefore(
                eq(1L), eq(StatusEmprestimo.ATIVO), any(LocalDate.class))).thenReturn(false);
        when(multaRepository.existsByEmprestimoLeitorIdAndStatus(1L, StatusMulta.PENDENTE)).thenReturn(false);
        when(emprestimoRepository.countByLeitorIdAndStatus(1L, StatusEmprestimo.ATIVO)).thenReturn(1L);
        when(reservaRepository.existsByLivroIdAndStatus(10L, StatusReserva.AGUARDANDO)).thenReturn(false);
        when(emprestimoRepository.save(any(Emprestimo.class))).thenReturn(emprestimoAtivo);

        EmprestimoResponseDTO resultado = emprestimoService.registrarEmprestimo(request);

        assertNotNull(resultado);
        assertEquals(StatusExemplar.EMPRESTADO, exemplar.getStatus());
        assertEquals(LocalDate.now().plusDays(14), resultado.getDataPrevistaDevolucao());
        verify(exemplarRepository).save(exemplar);
        verify(emprestimoRepository).save(any(Emprestimo.class));
    }

    @Test
    @DisplayName("RF06 - Deve impedir empréstimo para leitor bloqueado por empréstimo vencido")
    void deveImpedirEmprestimoParaLeitorComEmprestimoVencido() {
        EmprestimoRequestDTO request = EmprestimoRequestDTO.builder().exemplarId(100L).leitorId(1L).build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(leitor));
        when(exemplarRepository.findById(100L)).thenReturn(Optional.of(exemplar));
        when(emprestimoRepository.existsByLeitorIdAndStatusAndDataPrevistaDevolucaoBefore(
                eq(1L), eq(StatusEmprestimo.ATIVO), any(LocalDate.class))).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> emprestimoService.registrarEmprestimo(request));
        verify(emprestimoRepository, never()).save(any());
    }

    @Test
    @DisplayName("RF06 - Deve impedir empréstimo para leitor bloqueado por multa pendente")
    void deveImpedirEmprestimoParaLeitorComMultaPendente() {
        EmprestimoRequestDTO request = EmprestimoRequestDTO.builder().exemplarId(100L).leitorId(1L).build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(leitor));
        when(exemplarRepository.findById(100L)).thenReturn(Optional.of(exemplar));
        when(emprestimoRepository.existsByLeitorIdAndStatusAndDataPrevistaDevolucaoBefore(
                eq(1L), eq(StatusEmprestimo.ATIVO), any(LocalDate.class))).thenReturn(false);
        when(multaRepository.existsByEmprestimoLeitorIdAndStatus(1L, StatusMulta.PENDENTE)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> emprestimoService.registrarEmprestimo(request));
        verify(emprestimoRepository, never()).save(any());
    }

    @Test
    @DisplayName("RF06 - Deve impedir empréstimo quando leitor já possui 3 empréstimos ativos")
    void deveImpedirEmprestimoQuandoLeitorJaPossui3EmprestimosAtivos() {
        EmprestimoRequestDTO request = EmprestimoRequestDTO.builder().exemplarId(100L).leitorId(1L).build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(leitor));
        when(exemplarRepository.findById(100L)).thenReturn(Optional.of(exemplar));
        when(emprestimoRepository.existsByLeitorIdAndStatusAndDataPrevistaDevolucaoBefore(
                eq(1L), eq(StatusEmprestimo.ATIVO), any(LocalDate.class))).thenReturn(false);
        when(multaRepository.existsByEmprestimoLeitorIdAndStatus(1L, StatusMulta.PENDENTE)).thenReturn(false);
        when(emprestimoRepository.countByLeitorIdAndStatus(1L, StatusEmprestimo.ATIVO)).thenReturn(3L);

        assertThrows(BusinessRuleException.class, () -> emprestimoService.registrarEmprestimo(request));
        verify(emprestimoRepository, never()).save(any());
    }

    @Test
    @DisplayName("RF07 - Deve devolver no prazo sem gerar multa e tornar exemplar DISPONIVEL quando não há fila")
    void deveRegistrarDevolucaoNoPrazoSemMultaETornarExemplarDisponivel() {
        exemplar.setStatus(StatusExemplar.EMPRESTADO);
        when(emprestimoRepository.findById(500L)).thenReturn(Optional.of(emprestimoAtivo));
        when(reservaRepository.findFirstByLivroIdAndStatusOrderByDataReservaAsc(10L, StatusReserva.AGUARDANDO))
                .thenReturn(Optional.empty());
        when(emprestimoRepository.save(emprestimoAtivo)).thenReturn(emprestimoAtivo);

        EmprestimoResponseDTO resultado = emprestimoService.registrarDevolucao(500L);

        assertEquals(StatusEmprestimo.DEVOLVIDO, resultado.getStatus());
        assertEquals(StatusExemplar.DISPONIVEL, exemplar.getStatus());
        assertNull(resultado.getValorMulta());
        verify(multaRepository, never()).save(any());
    }

    @Test
    @DisplayName("RF07 - Deve gerar multa de R$ 1,00 por dia de atraso na devolução")
    void deveRegistrarDevolucaoComAtrasoGerandoMultaDeUmRealPorDia() {
        exemplar.setStatus(StatusExemplar.EMPRESTADO);
        emprestimoAtivo.setDataPrevistaDevolucao(LocalDate.now().minusDays(5));

        when(emprestimoRepository.findById(500L)).thenReturn(Optional.of(emprestimoAtivo));
        when(multaRepository.save(any(Multa.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservaRepository.findFirstByLivroIdAndStatusOrderByDataReservaAsc(10L, StatusReserva.AGUARDANDO))
                .thenReturn(Optional.empty());
        when(emprestimoRepository.save(emprestimoAtivo)).thenReturn(emprestimoAtivo);

        EmprestimoResponseDTO resultado = emprestimoService.registrarDevolucao(500L);

        assertEquals(StatusEmprestimo.DEVOLVIDO, resultado.getStatus());
        assertEquals(new BigDecimal("5.00"), resultado.getValorMulta());
        verify(multaRepository).save(any(Multa.class));
    }

    @Test
    @DisplayName("RF07 - Deve separar exemplar para o 1º da fila de reserva por 3 dias ao devolver")
    void deveSepararExemplarParaPrimeiroDaFilaAoDevolverLivroComReservaAguardando() {
        exemplar.setStatus(StatusExemplar.EMPRESTADO);
        Reserva reservaFila = Reserva.builder()
                .id(77L)
                .livro(livro)
                .leitor(leitor)
                .dataReserva(LocalDateTime.now().minusDays(2))
                .status(StatusReserva.AGUARDANDO)
                .build();

        when(emprestimoRepository.findById(500L)).thenReturn(Optional.of(emprestimoAtivo));
        when(reservaRepository.findFirstByLivroIdAndStatusOrderByDataReservaAsc(10L, StatusReserva.AGUARDANDO))
                .thenReturn(Optional.of(reservaFila));
        when(emprestimoRepository.save(emprestimoAtivo)).thenReturn(emprestimoAtivo);

        emprestimoService.registrarDevolucao(500L);

        assertEquals(StatusExemplar.SEPARADO, exemplar.getStatus());
        assertEquals(StatusReserva.PRONTA_PARA_RETIRADA, reservaFila.getStatus());
        assertEquals(exemplar, reservaFila.getExemplarSeparado());
        assertEquals(LocalDate.now().plusDays(3), reservaFila.getDataLimiteRetirada());
        verify(reservaRepository).save(reservaFila);
    }

    @Test
    @DisplayName("RF08 - Deve renovar empréstimo ativo por mais 7 dias uma única vez")
    void deveRenovarEmprestimoPorMais7DiasUmaUnicaVez() {
        LocalDate dataOriginal = emprestimoAtivo.getDataPrevistaDevolucao();

        when(emprestimoRepository.findById(500L)).thenReturn(Optional.of(emprestimoAtivo));
        when(emprestimoRepository.existsByLeitorIdAndStatusAndDataPrevistaDevolucaoBefore(
                eq(1L), eq(StatusEmprestimo.ATIVO), any(LocalDate.class))).thenReturn(false);
        when(multaRepository.existsByEmprestimoLeitorIdAndStatus(1L, StatusMulta.PENDENTE)).thenReturn(false);
        when(reservaRepository.existsByLivroIdAndStatusIn(
                eq(10L), eq(List.of(StatusReserva.AGUARDANDO, StatusReserva.PRONTA_PARA_RETIRADA)))).thenReturn(false);
        when(emprestimoRepository.save(emprestimoAtivo)).thenReturn(emprestimoAtivo);

        EmprestimoResponseDTO resultado = emprestimoService.renovarEmprestimo(500L, leitor);

        assertTrue(resultado.isRenovado());
        assertEquals(dataOriginal.plusDays(7), resultado.getDataPrevistaDevolucao());
    }

    @Test
    @DisplayName("RF08 - Deve impedir segunda renovação do mesmo empréstimo")
    void deveImpedirSegundaRenovacaoDoMesmoEmprestimo() {
        emprestimoAtivo.setRenovado(true);
        when(emprestimoRepository.findById(500L)).thenReturn(Optional.of(emprestimoAtivo));

        assertThrows(BusinessRuleException.class, () -> emprestimoService.renovarEmprestimo(500L, leitor));
    }

    @Test
    @DisplayName("RF08 - Deve impedir renovação por outro leitor que não seja o titular do empréstimo")
    void deveImpedirRenovacaoPorOutroLeitorQueNaoSejaODono() {
        Usuario outroLeitor = Usuario.builder().id(99L).nome("Outro").role(Role.LEITOR).build();
        when(emprestimoRepository.findById(500L)).thenReturn(Optional.of(emprestimoAtivo));

        assertThrows(UnauthorizedOperationException.class, () -> emprestimoService.renovarEmprestimo(500L, outroLeitor));
    }
}
