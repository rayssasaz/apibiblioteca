package com.apim.apibiblioteca.dto.reserva;

import com.apim.apibiblioteca.model.enumerations.StatusReserva;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservaResponseDTO {

    private Long id;
    private Long livroId;
    private String livroTitulo;
    private Long leitorId;
    private String leitorNome;
    private String leitorEmail;
    private Long exemplarSeparadoId;
    private String codigoPatrimonioSeparado;
    private LocalDateTime dataReserva;
    private LocalDate dataLimiteRetirada;
    private StatusReserva status;
    private Integer posicaoFila;
}
