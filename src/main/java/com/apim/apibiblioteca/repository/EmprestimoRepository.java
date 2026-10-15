package com.apim.apibiblioteca.repository;

import com.apim.apibiblioteca.model.Emprestimo;
import com.apim.apibiblioteca.model.enumerations.StatusEmprestimo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EmprestimoRepository extends JpaRepository<Emprestimo, Long> {

    long countByLeitorIdAndStatus(Long leitorId, StatusEmprestimo status);

    boolean existsByLeitorIdAndStatusAndDataPrevistaDevolucaoBefore(
            Long leitorId, StatusEmprestimo status, LocalDate dataReferencia);

    boolean existsByLeitorIdAndExemplarLivroIdAndStatus(
            Long leitorId, Long livroId, StatusEmprestimo status);

    List<Emprestimo> findByLeitorIdOrderByDataEmprestimoDesc(Long leitorId);

    List<Emprestimo> findByStatusOrderByDataEmprestimoDesc(StatusEmprestimo status);

    List<Emprestimo> findByStatusAndDataPrevistaDevolucaoBeforeOrderByDataPrevistaDevolucaoAsc(
            StatusEmprestimo status, LocalDate dataReferencia);

    @Query("""
            SELECT e.exemplar.livro, COUNT(e)
            FROM Emprestimo e
            WHERE e.dataEmprestimo BETWEEN :inicio AND :fim
            GROUP BY e.exemplar.livro
            ORDER BY COUNT(e) DESC
            """)
    List<Object[]> findLivrosMaisEmprestadosNoPeriodo(
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim,
            Pageable pageable);
}
