package com.apim.apibiblioteca.service;

import com.apim.apibiblioteca.dto.emprestimo.EmprestimoResponseDTO;
import com.apim.apibiblioteca.dto.reserva.ReservaRequestDTO;
import com.apim.apibiblioteca.dto.reserva.ReservaResponseDTO;
import com.apim.apibiblioteca.exception.BusinessRuleException;
import com.apim.apibiblioteca.exception.UnauthorizedOperationException;
import com.apim.apibiblioteca.model.Emprestimo;
import com.apim.apibiblioteca.model.Exemplar;
import com.apim.apibiblioteca.model.Livro;
import com.apim.apibiblioteca.model.Reserva;
import com.apim.apibiblioteca.model.Usuario;
import com.apim.apibiblioteca.model.enumerations.Role;
import com.apim.apibiblioteca.model.enumerations.StatusEmprestimo;
import com.apim.apibiblioteca.model.enumerations.StatusExemplar;
import com.apim.apibiblioteca.model.enumerations.StatusReserva;
import com.apim.apibiblioteca.repository.EmprestimoRepository;
import com.apim.apibiblioteca.repository.ExemplarRepository;
import com.apim.apibiblioteca.repository.LivroRepository;
import com.apim.apibiblioteca.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private LivroRepository livroRepository;

    @Mock
    private ExemplarRepository exemplarRepository;

    @Mock
    private EmprestimoRepository emprestimoRepository;

    @Mock
    private EmprestimoService emprestimoService;

    @InjectMocks
    private ReservaService reservaService;

    private Usuario leitor;
    private Livro livro;
    private Exemplar exemplarSeparado;

    @BeforeEach
    void setUp() {
        leitor = Usuario.builder()
                .id(1L)
                .nome("Maria Leitora")
                .email("maria@biblioteca.com")
                .role(Role.LEITOR)
                .build();

        livro = Livro.builder()
                .id(10L)
                .titulo("Effective Java")
                .autor("Joshua Bloch")
                .isbn("978-0134685991")
                .categoria("Programação")
                .anoPublicacao(2018)
                .build();

        exemplarSeparado = Exemplar.builder()
                .id(100L)
                .codigoPatrimonio("PAT-100")
                .status(StatusExemplar.SEPARADO)
                .livro(livro)
                .build();
    }

    @Test
    @DisplayName("RF11 - Deve criar reserva com status AGUARDANDO quando não há exemplares disponíveis")
    void deveCriarReservaQuandoNaoHaExemplaresDisponiveis() {
        ReservaRequestDTO request = new ReservaRequestDTO(10L);
        Reserva reservaSalva = Reserva.builder()
                .id(1L)
                .livro(livro)
                .leitor(leitor)
                .dataReserva(LocalDateTime.now())
                .status(StatusReserva.AGUARDANDO)
                .build();

        when(livroRepository.findById(10L)).thenReturn(Optional.of(livro));
        when(exemplarRepository.countByLivroIdAndStatusNot(10L, StatusExemplar.INDISPONIVEL)).thenReturn(2L);
        when(exemplarRepository.countByLivroIdAndStatus(10L, StatusExemplar.DISPONIVEL)).thenReturn(0L);
        doNothing().when(emprestimoService).validarLeitorNaoBloqueado(1L);
        when(reservaRepository.existsByLeitorIdAndLivroIdAndStatusIn(
                eq(1L), eq(10L), eq(List.of(StatusReserva.AGUARDANDO, StatusReserva.PRONTA_PARA_RETIRADA))))
                .thenReturn(false);
        when(emprestimoRepository.existsByLeitorIdAndExemplarLivroIdAndStatus(1L, 10L, StatusEmprestimo.ATIVO))
                .thenReturn(false);
        when(reservaRepository.save(any(Reserva.class))).thenReturn(reservaSalva);
        when(reservaRepository.findByLivroIdAndStatusInOrderByDataReservaAsc(
                eq(10L), eq(List.of(StatusReserva.PRONTA_PARA_RETIRADA, StatusReserva.AGUARDANDO))))
                .thenReturn(List.of(reservaSalva));

        ReservaResponseDTO resultado = reservaService.criarReserva(request, leitor);

        assertNotNull(resultado);
        assertEquals(StatusReserva.AGUARDANDO, resultado.getStatus());
        assertEquals(1, resultado.getPosicaoFila());
    }

    @Test
    @DisplayName("RF11 - Deve impedir reserva quando há exemplar disponível para empréstimo imediato")
    void deveImpedirReservaQuandoHaExemplarDisponivelNaEstante() {
        ReservaRequestDTO request = new ReservaRequestDTO(10L);

        when(livroRepository.findById(10L)).thenReturn(Optional.of(livro));
        when(exemplarRepository.countByLivroIdAndStatusNot(10L, StatusExemplar.INDISPONIVEL)).thenReturn(2L);
        when(exemplarRepository.countByLivroIdAndStatus(10L, StatusExemplar.DISPONIVEL)).thenReturn(1L);

        assertThrows(BusinessRuleException.class, () -> reservaService.criarReserva(request, leitor));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("RF12 - Deve converter reserva PRONTA_PARA_RETIRADA em empréstimo e marcar como ATENDIDA")
    void deveConverterReservaProntaParaRetiradaEmEmprestimo() {
        Reserva reservaPronta = Reserva.builder()
                .id(1L)
                .livro(livro)
                .leitor(leitor)
                .exemplarSeparado(exemplarSeparado)
                .dataReserva(LocalDateTime.now().minusDays(1))
                .dataLimiteRetirada(LocalDate.now().plusDays(2))
                .status(StatusReserva.PRONTA_PARA_RETIRADA)
                .build();

        Emprestimo emprestimoCriado = Emprestimo.builder()
                .id(500L)
                .exemplar(exemplarSeparado)
                .leitor(leitor)
                .dataEmprestimo(LocalDate.now())
                .dataPrevistaDevolucao(LocalDate.now().plusDays(14))
                .status(StatusEmprestimo.ATIVO)
                .build();

        EmprestimoResponseDTO responseEsperado = EmprestimoResponseDTO.builder()
                .id(500L)
                .status(StatusEmprestimo.ATIVO)
                .build();

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reservaPronta));
        doNothing().when(emprestimoService).validarLeitorNaoBloqueado(1L);
        doNothing().when(emprestimoService).validarLimiteEmprestimosAtivos(1L);
        when(emprestimoRepository.save(any(Emprestimo.class))).thenReturn(emprestimoCriado);
        when(emprestimoService.toResponseDTO(emprestimoCriado)).thenReturn(responseEsperado);

        EmprestimoResponseDTO resultado = reservaService.retirarReserva(1L);

        assertNotNull(resultado);
        assertEquals(StatusReserva.ATENDIDA, reservaPronta.getStatus());
        assertEquals(StatusExemplar.EMPRESTADO, exemplarSeparado.getStatus());
        verify(exemplarRepository).save(exemplarSeparado);
        verify(reservaRepository).save(reservaPronta);
    }

    @Test
    @DisplayName("RF13 - Deve cancelar reserva pronta e repassar exemplar separado ao próximo da fila")
    void deveCancelarReservaProntaERepassarExemplarAoProximoDaFila() {
        Reserva reservaPronta = Reserva.builder()
                .id(1L)
                .livro(livro)
                .leitor(leitor)
                .exemplarSeparado(exemplarSeparado)
                .dataReserva(LocalDateTime.now().minusDays(2))
                .dataLimiteRetirada(LocalDate.now().plusDays(1))
                .status(StatusReserva.PRONTA_PARA_RETIRADA)
                .build();

        Usuario segundoLeitor = Usuario.builder().id(2L).nome("Carlos").email("carlos@bib.com").role(Role.LEITOR).build();
        Reserva proximaReserva = Reserva.builder()
                .id(2L)
                .livro(livro)
                .leitor(segundoLeitor)
                .dataReserva(LocalDateTime.now().minusDays(1))
                .status(StatusReserva.AGUARDANDO)
                .build();

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reservaPronta));
        when(reservaRepository.findFirstByLivroIdAndStatusOrderByDataReservaAsc(10L, StatusReserva.AGUARDANDO))
                .thenReturn(Optional.of(proximaReserva));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservaResponseDTO resultado = reservaService.cancelarReserva(1L, leitor);

        assertEquals(StatusReserva.CANCELADA, resultado.getStatus());
        assertNull(reservaPronta.getExemplarSeparado());
        assertEquals(StatusReserva.PRONTA_PARA_RETIRADA, proximaReserva.getStatus());
        assertEquals(exemplarSeparado, proximaReserva.getExemplarSeparado());
        assertEquals(StatusExemplar.SEPARADO, exemplarSeparado.getStatus());
    }

    @Test
    @DisplayName("RF13 - Deve impedir que um leitor cancele a reserva de outro leitor")
    void deveImpedirLeitorDeCancelarReservaDeOutroLeitor() {
        Usuario outroLeitor = Usuario.builder().id(99L).nome("Outro").role(Role.LEITOR).build();
        Reserva reserva = Reserva.builder()
                .id(1L)
                .livro(livro)
                .leitor(leitor)
                .status(StatusReserva.AGUARDANDO)
                .build();

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

        assertThrows(UnauthorizedOperationException.class, () -> reservaService.cancelarReserva(1L, outroLeitor));
    }

    @Test
    @DisplayName("RF14 - Deve processar reservas expiradas em lote e devolver exemplar para DISPONIVEL quando fila vazia")
    void deveProcessarReservasExpiradasEmLoteEDisponibilizarExemplar() {
        Reserva reservaVencida = Reserva.builder()
                .id(1L)
                .livro(livro)
                .leitor(leitor)
                .exemplarSeparado(exemplarSeparado)
                .dataReserva(LocalDateTime.now().minusDays(5))
                .dataLimiteRetirada(LocalDate.now().minusDays(1))
                .status(StatusReserva.PRONTA_PARA_RETIRADA)
                .build();

        when(reservaRepository.findByStatusAndDataLimiteRetiradaBeforeOrderByDataReservaAsc(
                eq(StatusReserva.PRONTA_PARA_RETIRADA), any(LocalDate.class)))
                .thenReturn(List.of(reservaVencida));
        when(reservaRepository.findFirstByLivroIdAndStatusOrderByDataReservaAsc(10L, StatusReserva.AGUARDANDO))
                .thenReturn(Optional.empty());
        when(reservaRepository.save(reservaVencida)).thenReturn(reservaVencida);

        List<ReservaResponseDTO> processadas = reservaService.processarReservasExpiradas();

        assertEquals(1, processadas.size());
        assertEquals(StatusReserva.EXPIRADA, processadas.get(0).getStatus());
        assertEquals(StatusExemplar.DISPONIVEL, exemplarSeparado.getStatus());
    }
}
