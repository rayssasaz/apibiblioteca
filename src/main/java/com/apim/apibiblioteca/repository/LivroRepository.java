package com.apim.apibiblioteca.repository;

import com.apim.apibiblioteca.model.Livro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LivroRepository extends JpaRepository<Livro, Long> {

    Optional<Livro> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdNot(String isbn, Long id);

    @Query("""
            SELECT l FROM Livro l
            WHERE (:titulo IS NULL OR LOWER(l.titulo) LIKE LOWER(CONCAT('%', CAST(:titulo AS string), '%')))
              AND (:autor IS NULL OR LOWER(l.autor) LIKE LOWER(CONCAT('%', CAST(:autor AS string), '%')))
              AND (:categoria IS NULL OR LOWER(l.categoria) LIKE LOWER(CONCAT('%', CAST(:categoria AS string), '%')))
            """)
    Page<Livro> buscarComFiltros(
            @Param("titulo") String titulo,
            @Param("autor") String autor,
            @Param("categoria") String categoria,
            Pageable pageable);
}
