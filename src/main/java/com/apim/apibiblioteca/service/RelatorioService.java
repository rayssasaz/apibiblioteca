package com.apim.apibiblioteca.service;

import com.apim.apibiblioteca.dto.relatorio.EmprestimoAtrasadoDTO;
import com.apim.apibiblioteca.dto.relatorio.LivroMaisEmprestadoDTO;
import com.apim.apibiblioteca.dto.relatorio.MultaPorLeitorDTO;
import com.apim.apibiblioteca.exception.BusinessRuleException;
import com.apim.apibiblioteca.model.Emprestimo;
import com.apim.apibiblioteca.model.Livro;
import com.apim.apibiblioteca.model.Usuario;
import com.apim.apibiblioteca.model.enumerations.StatusEmprestimo;
import com.apim.apibiblioteca.model.enumerations.StatusMulta;
import com.apim.apibiblioteca.repository.EmprestimoRepository;
import com.apim.apibiblioteca.repository.MultaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class RelatorioService {

    public static final int LIMITE_TOP_LIVROS = 10;

    private final EmprestimoRepository emprestimoRepository;
    private final MultaRepository multaRepository;

    public RelatorioService(
            EmprestimoRepository emprestimoRepository,
            MultaRepository multaRepository) {
        this.emprestimoRepository = emprestimoRepository;
        this.multaRepository = multaRepository;
    }

    @Transactional(readOnly = true)
    public List<LivroMaisEmprestadoDTO> relatorioLivrosMaisEmprestados(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) {
            throw new BusinessRuleException("As datas de início e fim do período são obrigatórias");
        }
        if (inicio.isAfter(fim)) {
            throw new BusinessRuleException("A data de início não pode ser posterior à data de fim");
        }

        List<Object[]> resultados = emprestimoRepository.findLivrosMaisEmprestadosNoPeriodo(
                inicio, fim, PageRequest.of(0, LIMITE_TOP_LIVROS));

        return resultados.stream()
                .map(linha -> {
                    Livro livro = (Livro) linha[0];
                    long totalEmprestimos = ((Number) linha[1]).longValue();
                    return LivroMaisEmprestadoDTO.builder()
                            .livroId(livro.getId())
                            .titulo(livro.getTitulo())
                            .autor(livro.getAutor())
                            .isbn(livro.getIsbn())
                            .categoria(livro.getCategoria())
                            .totalEmprestimos(totalEmprestimos)
                            .build();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EmprestimoAtrasadoDTO> relatorioEmprestimosAtrasados() {
        LocalDate hoje = LocalDate.now();
        List<Emprestimo> atrasados = emprestimoRepository
                .findByStatusAndDataPrevistaDevolucaoBeforeOrderByDataPrevistaDevolucaoAsc(
                        StatusEmprestimo.ATIVO, hoje);

        return atrasados.stream()
                .map(emprestimo -> {
                    long diasAtraso = ChronoUnit.DAYS.between(emprestimo.getDataPrevistaDevolucao(), hoje);
                    BigDecimal multaEstimada = EmprestimoService.VALOR_MULTA_POR_DIA
                            .multiply(BigDecimal.valueOf(diasAtraso))
                            .setScale(2, RoundingMode.HALF_UP);

                    return EmprestimoAtrasadoDTO.builder()
                            .emprestimoId(emprestimo.getId())
                            .leitorId(emprestimo.getLeitor().getId())
                            .leitorNome(emprestimo.getLeitor().getNome())
                            .leitorEmail(emprestimo.getLeitor().getEmail())
                            .livroId(emprestimo.getExemplar().getLivro().getId())
                            .livroTitulo(emprestimo.getExemplar().getLivro().getTitulo())
                            .exemplarId(emprestimo.getExemplar().getId())
                            .codigoPatrimonio(emprestimo.getExemplar().getCodigoPatrimonio())
                            .dataEmprestimo(emprestimo.getDataEmprestimo())
                            .dataPrevistaDevolucao(emprestimo.getDataPrevistaDevolucao())
                            .diasAtraso(diasAtraso)
                            .multaEstimada(multaEstimada)
                            .build();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MultaPorLeitorDTO> relatorioMultasPendentesPorLeitor() {
        List<Object[]> resultados = multaRepository.findTotalMultasPorLeitor(StatusMulta.PENDENTE);

        return resultados.stream()
                .map(linha -> {
                    Usuario leitor = (Usuario) linha[0];
                    long quantidade = ((Number) linha[1]).longValue();
                    BigDecimal valorTotal = ((BigDecimal) linha[2]).setScale(2, RoundingMode.HALF_UP);

                    return MultaPorLeitorDTO.builder()
                            .leitorId(leitor.getId())
                            .leitorNome(leitor.getNome())
                            .leitorEmail(leitor.getEmail())
                            .quantidadeMultasPendentes(quantidade)
                            .valorTotalPendente(valorTotal)
                            .build();
                })
                .toList();
    }
}
