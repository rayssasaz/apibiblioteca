package com.apim.apibiblioteca.repository;

import com.apim.apibiblioteca.model.Exemplar;
import com.apim.apibiblioteca.model.enumerations.StatusExemplar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExemplarRepository extends JpaRepository<Exemplar, Long> {

    boolean existsByCodigoPatrimonio(String codigoPatrimonio);

    Optional<Exemplar> findByCodigoPatrimonio(String codigoPatrimonio);

    List<Exemplar> findByLivroId(Long livroId);

    long countByLivroIdAndStatus(Long livroId, StatusExemplar status);

    long countByLivroIdAndStatusNot(Long livroId, StatusExemplar status);

    boolean existsByLivroIdAndStatusIn(Long livroId, Collection<StatusExemplar> statuses);
}
