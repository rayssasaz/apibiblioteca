package com.apim.apibiblioteca.repository;

import com.apim.apibiblioteca.model.Reserva;
import com.apim.apibiblioteca.model.enumerations.StatusReserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    boolean existsByLivroIdAndStatusIn(Long livroId, Collection<StatusReserva> statuses);

    boolean existsByLivroIdAndStatus(Long livroId, StatusReserva status);

    boolean existsByLeitorIdAndLivroIdAndStatusIn(
            Long leitorId, Long livroId, Collection<StatusReserva> statuses);

    Optional<Reserva> findFirstByLivroIdAndStatusOrderByDataReservaAsc(
            Long livroId, StatusReserva status);

    List<Reserva> findByLivroIdAndStatusInOrderByDataReservaAsc(
            Long livroId, Collection<StatusReserva> statuses);

    List<Reserva> findByLeitorIdOrderByDataReservaDesc(Long leitorId);

    List<Reserva> findByStatusAndDataLimiteRetiradaBeforeOrderByDataReservaAsc(
            StatusReserva status, LocalDate dataReferencia);
}
