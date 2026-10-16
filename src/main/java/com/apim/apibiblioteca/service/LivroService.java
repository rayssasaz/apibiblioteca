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
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LivroService {

    private final LivroRepository livroRepository;
    private final ExemplarRepository exemplarRepository;
    private final ReservaRepository reservaRepository;
    private final ModelMapper modelMapper;
    private final PagedResponseMapper pagedResponseMapper;

    public LivroService(
            LivroRepository livroRepository,
            ExemplarRepository exemplarRepository,
            ReservaRepository reservaRepository,
            ModelMapper modelMapper,
            PagedResponseMapper pagedResponseMapper) {
        this.livroRepository = livroRepository;
        this.exemplarRepository = exemplarRepository;
        this.reservaRepository = reservaRepository;
        this.modelMapper = modelMapper;
        this.pagedResponseMapper = pagedResponseMapper;
    }

    @Transactional
    public LivroResponseDTO criarLivro(LivroRequestDTO request) {
        if (livroRepository.existsByIsbn(request.getIsbn())) {
            throw new BusinessRuleException("Já existe um livro cadastrado com este ISBN");
        }

        Livro livro = modelMapper.map(request, Livro.class);
        Livro salvo = livroRepository.save(livro);
        return toResponseDTO(salvo);
    }

    @Transactional
    public LivroResponseDTO atualizarLivro(Long id, LivroRequestDTO request) {
        Livro livroExistente = livroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado com ID: " + id));

        if (livroRepository.existsByIsbnAndIdNot(request.getIsbn(), id)) {
            throw new BusinessRuleException("Já existe outro livro cadastrado com este ISBN");
        }

        modelMapper.map(request, livroExistente);
        livroExistente.setId(id);

        Livro atualizado = livroRepository.save(livroExistente);
        return toResponseDTO(atualizado);
    }

    @Transactional
    public void removerLivro(Long id) {
        Livro livro = livroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado com ID: " + id));

        boolean possuiExemplarEmprestadoOuSeparado = exemplarRepository.existsByLivroIdAndStatusIn(
                id, List.of(StatusExemplar.EMPRESTADO, StatusExemplar.SEPARADO));
        if (possuiExemplarEmprestadoOuSeparado) {
            throw new BusinessRuleException("Não é permitido remover um livro que possua exemplares emprestados ou separados para reserva");
        }

        boolean possuiReservaAtiva = reservaRepository.existsByLivroIdAndStatusIn(
                id, List.of(StatusReserva.AGUARDANDO, StatusReserva.PRONTA_PARA_RETIRADA));
        if (possuiReservaAtiva) {
            throw new BusinessRuleException("Não é permitido remover um livro que possua reservas ativas");
        }

        livroRepository.delete(livro);
    }

    @Transactional(readOnly = true)
    public LivroResponseDTO buscarPorId(Long id) {
        Livro livro = livroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado com ID: " + id));
        return toResponseDTO(livro);
    }

    @Transactional(readOnly = true)
    public PagedResponse<LivroResponseDTO> buscarLivros(
            String titulo,
            String autor,
            String categoria,
            Pageable pageable) {
        Page<Livro> paginaLivros = livroRepository.buscarComFiltros(titulo, autor, categoria, pageable);
        PagedResponse<LivroResponseDTO> response = pagedResponseMapper.toPagedResponse(paginaLivros, LivroResponseDTO.class);

        response.getContent().forEach(dto -> {
            long disponiveis = exemplarRepository.countByLivroIdAndStatus(dto.getId(), StatusExemplar.DISPONIVEL);
            dto.setQuantidadeDisponivel(disponiveis);
        });

        return response;
    }

    private LivroResponseDTO toResponseDTO(Livro livro) {
        LivroResponseDTO dto = modelMapper.map(livro, LivroResponseDTO.class);
        long disponiveis = exemplarRepository.countByLivroIdAndStatus(livro.getId(), StatusExemplar.DISPONIVEL);
        dto.setQuantidadeDisponivel(disponiveis);
        return dto;
    }
}
