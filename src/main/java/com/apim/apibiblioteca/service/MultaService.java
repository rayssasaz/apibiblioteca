package com.apim.apibiblioteca.service;

import com.apim.apibiblioteca.dto.multa.MultaResponseDTO;
import com.apim.apibiblioteca.exception.BusinessRuleException;
import com.apim.apibiblioteca.exception.ResourceNotFoundException;
import com.apim.apibiblioteca.model.Multa;
import com.apim.apibiblioteca.model.Usuario;
import com.apim.apibiblioteca.model.enumerations.StatusMulta;
import com.apim.apibiblioteca.repository.MultaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class MultaService {

    private final MultaRepository multaRepository;

    public MultaService(MultaRepository multaRepository) {
        this.multaRepository = multaRepository;
    }

    @Transactional
    public MultaResponseDTO pagarMulta(Long multaId) {
        Multa multa = multaRepository.findById(multaId)
                .orElseThrow(() -> new ResourceNotFoundException("Multa não encontrada com ID: " + multaId));

        if (multa.getStatus() == StatusMulta.PAGA) {
            throw new BusinessRuleException("Esta multa já foi paga anteriormente");
        }

        multa.setStatus(StatusMulta.PAGA);
        multa.setDataPagamento(LocalDate.now());

        Multa atualizada = multaRepository.save(multa);
        return toResponseDTO(atualizada);
    }

    @Transactional(readOnly = true)
    public List<MultaResponseDTO> listarMinhasMultas(Usuario leitorLogado, StatusMulta status) {
        List<Multa> multas;
        if (status != null) {
            multas = multaRepository.findByEmprestimoLeitorIdAndStatusOrderByDataGeracaoDesc(leitorLogado.getId(), status);
        } else {
            multas = multaRepository.findByEmprestimoLeitorIdOrderByDataGeracaoDesc(leitorLogado.getId());
        }

        return multas.stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MultaResponseDTO> listarTodasMultas(Long leitorId, StatusMulta status) {
        return multaRepository.findAll().stream()
                .filter(m -> leitorId == null || m.getEmprestimo().getLeitor().getId().equals(leitorId))
                .filter(m -> status == null || m.getStatus() == status)
                .map(this::toResponseDTO)
                .toList();
    }

    private MultaResponseDTO toResponseDTO(Multa multa) {
        return MultaResponseDTO.builder()
                .id(multa.getId())
                .emprestimoId(multa.getEmprestimo().getId())
                .leitorId(multa.getEmprestimo().getLeitor().getId())
                .leitorNome(multa.getEmprestimo().getLeitor().getNome())
                .livroTitulo(multa.getEmprestimo().getExemplar().getLivro().getTitulo())
                .codigoPatrimonio(multa.getEmprestimo().getExemplar().getCodigoPatrimonio())
                .valor(multa.getValor())
                .diasAtraso(multa.getDiasAtraso())
                .status(multa.getStatus())
                .dataGeracao(multa.getDataGeracao())
                .dataPagamento(multa.getDataPagamento())
                .build();
    }
}
