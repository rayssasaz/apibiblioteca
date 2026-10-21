package com.apim.apibiblioteca.service;

import com.apim.apibiblioteca.dto.livro.LivroRequestDTO;
import com.apim.apibiblioteca.dto.livro.LivroResponseDTO;
import com.apim.apibiblioteca.dto.page.PagedResponse;
import com.apim.apibiblioteca.exception.BusinessRuleException;
import com.apim.apibiblioteca.exception.ResourceNotFoundException;
import com.apim.apibiblioteca.mapper.PagedResponseMapper;
import com.apim.apibiblioteca.model.Livro;
import com.apim.apibiblioteca.model.enumerations.StatusExemplar;
import com.apim.apibiblioteca.model.enumerations.StatusReserva;
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
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LivroServiceTest {

    @Mock
    private LivroRepository livroRepository;

    @Mock
    private ExemplarRepository exemplarRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private PagedResponseMapper pagedResponseMapper;

    @InjectMocks
    private LivroService livroService;

    private Livro livro;
    private LivroRequestDTO requestDTO;
    private LivroResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        livro = Livro.builder()
                .id(1L)
                .titulo("Clean Architecture")
                .autor("Robert C. Martin")
                .isbn("978-0134494166")
                .categoria("Engenharia de Software")
                .anoPublicacao(2017)
                .build();

        requestDTO = LivroRequestDTO.builder()
                .titulo("Clean Architecture")
                .autor("Robert C. Martin")
                .isbn("978-0134494166")
                .categoria("Engenharia de Software")
                .anoPublicacao(2017)
                .build();

        responseDTO = LivroResponseDTO.builder()
                .id(1L)
                .titulo("Clean Architecture")
                .autor("Robert C. Martin")
                .isbn("978-0134494166")
                .categoria("Engenharia de Software")
                .anoPublicacao(2017)
                .quantidadeDisponivel(0L)
                .build();
    }

    @Test
    @DisplayName("RF03 - Deve criar livro com ISBN único com sucesso")
    void deveCriarLivroComSucesso() {
        when(livroRepository.existsByIsbn(requestDTO.getIsbn())).thenReturn(false);
        when(modelMapper.map(requestDTO, Livro.class)).thenReturn(livro);
        when(livroRepository.save(livro)).thenReturn(livro);
        when(modelMapper.map(livro, LivroResponseDTO.class)).thenReturn(responseDTO);
        when(exemplarRepository.countByLivroIdAndStatus(1L, StatusExemplar.DISPONIVEL)).thenReturn(2L);

        LivroResponseDTO resultado = livroService.criarLivro(requestDTO);

        assertNotNull(resultado);
        assertEquals(2L, resultado.getQuantidadeDisponivel());
        verify(livroRepository).save(livro);
    }

    @Test
    @DisplayName("RF03 - Deve impedir cadastro de livro com ISBN duplicado")
    void deveLancarExcecaoAoCriarLivroComIsbnDuplicado() {
        when(livroRepository.existsByIsbn(requestDTO.getIsbn())).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> livroService.criarLivro(requestDTO));
        verify(livroRepository, never()).save(any());
    }

    @Test
    @DisplayName("RF03 - Deve remover livro sem exemplares emprestados nem reservas ativas")
    void deveRemoverLivroSemPendenciasComSucesso() {
        when(livroRepository.findById(1L)).thenReturn(Optional.of(livro));
        when(exemplarRepository.existsByLivroIdAndStatusIn(
                eq(1L), eq(List.of(StatusExemplar.EMPRESTADO, StatusExemplar.SEPARADO)))).thenReturn(false);
        when(reservaRepository.existsByLivroIdAndStatusIn(
                eq(1L), eq(List.of(StatusReserva.AGUARDANDO, StatusReserva.PRONTA_PARA_RETIRADA)))).thenReturn(false);

        livroService.removerLivro(1L);

        verify(livroRepository).delete(livro);
    }

    @Test
    @DisplayName("RF03 - Deve impedir remoção de livro que possui exemplar emprestado ou separado")
    void deveImpedirRemocaoDeLivroComExemplarEmprestadoOuSeparado() {
        when(livroRepository.findById(1L)).thenReturn(Optional.of(livro));
        when(exemplarRepository.existsByLivroIdAndStatusIn(
                eq(1L), eq(List.of(StatusExemplar.EMPRESTADO, StatusExemplar.SEPARADO)))).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> livroService.removerLivro(1L));
        verify(livroRepository, never()).delete(any());
    }

    @Test
    @DisplayName("RF03 - Deve impedir remoção de livro que possui reserva ativa")
    void deveImpedirRemocaoDeLivroComReservaAtiva() {
        when(livroRepository.findById(1L)).thenReturn(Optional.of(livro));
        when(exemplarRepository.existsByLivroIdAndStatusIn(
                eq(1L), eq(List.of(StatusExemplar.EMPRESTADO, StatusExemplar.SEPARADO)))).thenReturn(false);
        when(reservaRepository.existsByLivroIdAndStatusIn(
                eq(1L), eq(List.of(StatusReserva.AGUARDANDO, StatusReserva.PRONTA_PARA_RETIRADA)))).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> livroService.removerLivro(1L));
        verify(livroRepository, never()).delete(any());
    }

    @Test
    @DisplayName("RF03 - Deve lançar ResourceNotFoundException ao buscar livro inexistente")
    void deveLancarExcecaoAoBuscarLivroInexistente() {
        when(livroRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> livroService.buscarPorId(99L));
    }

    @Test
    @DisplayName("RF05 - Deve buscar livros paginados preenchendo quantidadeDisponivel")
    void deveBuscarLivrosPaginadosComCalculoDeQuantidadeDisponivel() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Livro> page = new PageImpl<>(List.of(livro), pageable, 1);
        PagedResponse<LivroResponseDTO> pagedResponse = new PagedResponse<>(
                List.of(responseDTO), 0, 10, 1L, 1, true);

        when(livroRepository.buscarComFiltros("Clean", null, null, pageable)).thenReturn(page);
        when(pagedResponseMapper.toPagedResponse(page, LivroResponseDTO.class)).thenReturn(pagedResponse);
        when(exemplarRepository.countByLivroIdAndStatus(1L, StatusExemplar.DISPONIVEL)).thenReturn(3L);

        PagedResponse<LivroResponseDTO> resultado = livroService.buscarLivros("Clean", null, null, pageable);

        assertEquals(1, resultado.getContent().size());
        assertEquals(3L, resultado.getContent().get(0).getQuantidadeDisponivel());
    }
}
