package com.apim.apibiblioteca.service;

import com.apim.apibiblioteca.dto.multa.MultaResponseDTO;
import com.apim.apibiblioteca.exception.BusinessRuleException;
import com.apim.apibiblioteca.model.Emprestimo;
import com.apim.apibiblioteca.model.Exemplar;
import com.apim.apibiblioteca.model.Livro;
import com.apim.apibiblioteca.model.Multa;
import com.apim.apibiblioteca.model.Usuario;
import com.apim.apibiblioteca.model.enumerations.Role;
import com.apim.apibiblioteca.model.enumerations.StatusMulta;
import com.apim.apibiblioteca.repository.MultaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MultaServiceTest {

    @Mock
    private MultaRepository multaRepository;

    @InjectMocks
    private MultaService multaService;

    private Usuario leitor;
    private Multa multaPendente;

    @BeforeEach
    void setUp() {
        leitor = Usuario.builder()
                .id(1L)
                .nome("João Leitor")
                .email("joao@biblioteca.com")
                .role(Role.LEITOR)
                .build();

        Livro livro = Livro.builder().id(10L).titulo("Clean Code").build();
        Exemplar exemplar = Exemplar.builder().id(100L).codigoPatrimonio("PAT-100").livro(livro).build();
        Emprestimo emprestimo = Emprestimo.builder().id(500L).leitor(leitor).exemplar(exemplar).build();

        multaPendente = Multa.builder()
                .id(1L)
                .emprestimo(emprestimo)
                .valor(new BigDecimal("4.00"))
                .diasAtraso(4L)
                .status(StatusMulta.PENDENTE)
                .dataGeracao(LocalDate.now().minusDays(1))
                .build();
    }

    @Test
    @DisplayName("RF09 - Deve registrar pagamento de multa pendente atualizando status e dataPagamento")
    void deveRegistrarPagamentoDeMultaPendenteComSucesso() {
        when(multaRepository.findById(1L)).thenReturn(Optional.of(multaPendente));
        when(multaRepository.save(multaPendente)).thenReturn(multaPendente);

        MultaResponseDTO resultado = multaService.pagarMulta(1L);

        assertEquals(StatusMulta.PAGA, resultado.getStatus());
        assertNotNull(resultado.getDataPagamento());
        verify(multaRepository).save(multaPendente);
    }

    @Test
    @DisplayName("RF09 - Deve impedir pagamento duplicado de multa que já está PAGA")
    void deveImpedirPagamentoDeMultaJaPaga() {
        multaPendente.setStatus(StatusMulta.PAGA);
        when(multaRepository.findById(1L)).thenReturn(Optional.of(multaPendente));

        assertThrows(BusinessRuleException.class, () -> multaService.pagarMulta(1L));
        verify(multaRepository, never()).save(any());
    }

    @Test
    @DisplayName("RF10 - Deve listar multas do leitor autenticado")
    void deveListarMultasDoLeitorLogado() {
        when(multaRepository.findByEmprestimoLeitorIdOrderByDataGeracaoDesc(1L))
                .thenReturn(List.of(multaPendente));

        List<MultaResponseDTO> resultado = multaService.listarMinhasMultas(leitor, null);

        assertEquals(1, resultado.size());
        assertEquals(new BigDecimal("4.00"), resultado.get(0).getValor());
    }
}
