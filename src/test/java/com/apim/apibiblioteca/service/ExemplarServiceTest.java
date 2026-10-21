package com.apim.apibiblioteca.service;

import com.apim.apibiblioteca.dto.exemplar.ExemplarRequestDTO;
import com.apim.apibiblioteca.dto.exemplar.ExemplarResponseDTO;
import com.apim.apibiblioteca.dto.exemplar.IndisponibilizarExemplarDTO;
import com.apim.apibiblioteca.exception.BusinessRuleException;
import com.apim.apibiblioteca.model.Exemplar;
import com.apim.apibiblioteca.model.Livro;
import com.apim.apibiblioteca.model.enumerations.StatusExemplar;
import com.apim.apibiblioteca.repository.ExemplarRepository;
import com.apim.apibiblioteca.repository.LivroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExemplarServiceTest {

    @Mock
    private ExemplarRepository exemplarRepository;

    @Mock
    private LivroRepository livroRepository;

    @InjectMocks
    private ExemplarService exemplarService;

    private Livro livro;
    private Exemplar exemplar;

    @BeforeEach
    void setUp() {
        livro = Livro.builder()
                .id(1L)
                .titulo("Domain-Driven Design")
                .autor("Eric Evans")
                .isbn("978-0321125217")
                .categoria("Engenharia de Software")
                .anoPublicacao(2003)
                .build();

        exemplar = Exemplar.builder()
                .id(10L)
                .codigoPatrimonio("PAT-001")
                .status(StatusExemplar.DISPONIVEL)
                .livro(livro)
                .build();
    }

    @Test
    @DisplayName("RF04 - Deve cadastrar exemplar com status inicial DISPONIVEL")
    void deveAdicionarExemplarComStatusDisponivel() {
        ExemplarRequestDTO request = new ExemplarRequestDTO("PAT-001");

        when(livroRepository.findById(1L)).thenReturn(Optional.of(livro));
        when(exemplarRepository.existsByCodigoPatrimonio("PAT-001")).thenReturn(false);
        when(exemplarRepository.save(any(Exemplar.class))).thenReturn(exemplar);

        ExemplarResponseDTO resultado = exemplarService.adicionarExemplar(1L, request);

        assertEquals("PAT-001", resultado.getCodigoPatrimonio());
        assertEquals(StatusExemplar.DISPONIVEL, resultado.getStatus());
        verify(exemplarRepository).save(any(Exemplar.class));
    }

    @Test
    @DisplayName("RF04 - Deve impedir cadastro de exemplar com código de patrimônio duplicado")
    void deveLancarExcecaoAoAdicionarExemplarComPatrimonioDuplicado() {
        ExemplarRequestDTO request = new ExemplarRequestDTO("PAT-001");

        when(livroRepository.findById(1L)).thenReturn(Optional.of(livro));
        when(exemplarRepository.existsByCodigoPatrimonio("PAT-001")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> exemplarService.adicionarExemplar(1L, request));
        verify(exemplarRepository, never()).save(any());
    }

    @Test
    @DisplayName("RF04 - Deve marcar exemplar disponível como INDISPONIVEL registrando o motivo")
    void deveMarcarExemplarComoIndisponivelComSucesso() {
        IndisponibilizarExemplarDTO request = new IndisponibilizarExemplarDTO("Restauração de capa");

        when(exemplarRepository.findById(10L)).thenReturn(Optional.of(exemplar));
        when(exemplarRepository.save(exemplar)).thenReturn(exemplar);

        ExemplarResponseDTO resultado = exemplarService.marcarIndisponivel(10L, request);

        assertEquals(StatusExemplar.INDISPONIVEL, resultado.getStatus());
        assertEquals("Restauração de capa", resultado.getMotivoIndisponibilidade());
    }

    @Test
    @DisplayName("RF04 - Deve impedir indisponibilização de exemplar que está EMPRESTADO")
    void deveImpedirMarcarComoIndisponivelExemplarEmprestado() {
        exemplar.setStatus(StatusExemplar.EMPRESTADO);
        IndisponibilizarExemplarDTO request = new IndisponibilizarExemplarDTO("Danificado");

        when(exemplarRepository.findById(10L)).thenReturn(Optional.of(exemplar));

        assertThrows(BusinessRuleException.class, () -> exemplarService.marcarIndisponivel(10L, request));
        verify(exemplarRepository, never()).save(any());
    }

    @Test
    @DisplayName("RF04 - Deve reativar exemplar INDISPONIVEL para DISPONIVEL limpando o motivo")
    void deveReativarExemplarIndisponivelParaDisponivel() {
        exemplar.setStatus(StatusExemplar.INDISPONIVEL);
        exemplar.setMotivoIndisponibilidade("Restauração concluída");

        when(exemplarRepository.findById(10L)).thenReturn(Optional.of(exemplar));
        when(exemplarRepository.save(exemplar)).thenReturn(exemplar);

        ExemplarResponseDTO resultado = exemplarService.marcarDisponivel(10L);

        assertEquals(StatusExemplar.DISPONIVEL, resultado.getStatus());
        assertNull(resultado.getMotivoIndisponibilidade());
    }
}
