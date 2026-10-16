package com.apim.apibiblioteca.dto.multa;

import com.apim.apibiblioteca.model.enumerations.StatusMulta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MultaResponseDTO {

    private Long id;
    private Long emprestimoId;
    private Long leitorId;
    private String leitorNome;
    private String livroTitulo;
    private String codigoPatrimonio;
    private BigDecimal valor;
    private long diasAtraso;
    private StatusMulta status;
    private LocalDate dataGeracao;
    private LocalDate dataPagamento;
}
