package com.apim.apibiblioteca.repository;

import com.apim.apibiblioteca.model.Multa;
import com.apim.apibiblioteca.model.enumerations.StatusMulta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MultaRepository extends JpaRepository<Multa, Long> {

    boolean existsByEmprestimoLeitorIdAndStatus(Long leitorId, StatusMulta status);

    List<Multa> findByEmprestimoLeitorIdOrderByDataGeracaoDesc(Long leitorId);

    List<Multa> findByEmprestimoLeitorIdAndStatusOrderByDataGeracaoDesc(Long leitorId, StatusMulta status);

    Optional<Multa> findByEmprestimoId(Long emprestimoId);

    @Query("""
            SELECT m.emprestimo.leitor, COUNT(m), SUM(m.valor)
            FROM Multa m
            WHERE m.status = :status
            GROUP BY m.emprestimo.leitor
            ORDER BY SUM(m.valor) DESC
            """)
    List<Object[]> findTotalMultasPorLeitor(@Param("status") StatusMulta status);
}
