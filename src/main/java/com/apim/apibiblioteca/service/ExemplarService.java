package com.apim.apibiblioteca.service;

import com.apim.apibiblioteca.dto.exemplar.ExemplarRequestDTO;
import com.apim.apibiblioteca.dto.exemplar.ExemplarResponseDTO;
import com.apim.apibiblioteca.dto.exemplar.IndisponibilizarExemplarDTO;
import com.apim.apibiblioteca.exception.BusinessRuleException;
import com.apim.apibiblioteca.exception.ResourceNotFoundException;
import com.apim.apibiblioteca.model.Exemplar;
import com.apim.apibiblioteca.model.Livro;
import com.apim.apibiblioteca.model.enumerations.StatusExemplar;
import com.apim.apibiblioteca.repository.ExemplarRepository;
import com.apim.apibiblioteca.repository.LivroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ExemplarService {

    private final ExemplarRepository exemplarRepository;
    private final LivroRepository livroRepository;

    public ExemplarService(ExemplarRepository exemplarRepository, LivroRepository livroRepository) {
        this.exemplarRepository = exemplarRepository;
        this.livroRepository = livroRepository;
    }

    @Transactional
    public ExemplarResponseDTO adicionarExemplar(Long livroId, ExemplarRequestDTO request) {
        Livro livro = livroRepository.findById(livroId)
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado com ID: " + livroId));

        if (exemplarRepository.existsByCodigoPatrimonio(request.getCodigoPatrimonio())) {
            throw new BusinessRuleException("Já existe um exemplar cadastrado com este código de patrimônio");
        }

        Exemplar exemplar = Exemplar.builder()
                .codigoPatrimonio(request.getCodigoPatrimonio())
                .status(StatusExemplar.DISPONIVEL)
                .livro(livro)
                .build();

        Exemplar salvo = exemplarRepository.save(exemplar);
        return toResponseDTO(salvo);
    }

    @Transactional(readOnly = true)
    public List<ExemplarResponseDTO> listarExemplaresPorLivro(Long livroId) {
        if (!livroRepository.existsById(livroId)) {
            throw new ResourceNotFoundException("Livro não encontrado com ID: " + livroId);
        }

        return exemplarRepository.findByLivroId(livroId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExemplarResponseDTO buscarPorId(Long id) {
        Exemplar exemplar = exemplarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exemplar não encontrado com ID: " + id));
        return toResponseDTO(exemplar);
    }

    @Transactional
    public ExemplarResponseDTO marcarIndisponivel(Long exemplarId, IndisponibilizarExemplarDTO request) {
        Exemplar exemplar = exemplarRepository.findById(exemplarId)
                .orElseThrow(() -> new ResourceNotFoundException("Exemplar não encontrado com ID: " + exemplarId));

        if (exemplar.getStatus() == StatusExemplar.EMPRESTADO || exemplar.getStatus() == StatusExemplar.SEPARADO) {
            throw new BusinessRuleException("Não é permitido marcar como indisponível um exemplar que está emprestado ou separado para reserva");
        }

        exemplar.setStatus(StatusExemplar.INDISPONIVEL);
        exemplar.setMotivoIndisponibilidade(request.getMotivo());

        Exemplar atualizado = exemplarRepository.save(exemplar);
        return toResponseDTO(atualizado);
    }

    @Transactional
    public ExemplarResponseDTO marcarDisponivel(Long exemplarId) {
        Exemplar exemplar = exemplarRepository.findById(exemplarId)
                .orElseThrow(() -> new ResourceNotFoundException("Exemplar não encontrado com ID: " + exemplarId));

        if (exemplar.getStatus() != StatusExemplar.INDISPONIVEL) {
            throw new BusinessRuleException("Apenas exemplares indisponíveis podem ser reativados manualmente para disponíveis");
        }

        exemplar.setStatus(StatusExemplar.DISPONIVEL);
        exemplar.setMotivoIndisponibilidade(null);

        Exemplar atualizado = exemplarRepository.save(exemplar);
        return toResponseDTO(atualizado);
    }

    private ExemplarResponseDTO toResponseDTO(Exemplar exemplar) {
        return ExemplarResponseDTO.builder()
                .id(exemplar.getId())
                .codigoPatrimonio(exemplar.getCodigoPatrimonio())
                .status(exemplar.getStatus())
                .motivoIndisponibilidade(exemplar.getMotivoIndisponibilidade())
                .livroId(exemplar.getLivro().getId())
                .livroTitulo(exemplar.getLivro().getTitulo())
                .livroIsbn(exemplar.getLivro().getIsbn())
                .build();
    }
}
