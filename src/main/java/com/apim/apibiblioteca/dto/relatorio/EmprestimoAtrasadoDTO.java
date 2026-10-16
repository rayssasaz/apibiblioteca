package com.apim.apibiblioteca.dto.relatorio;

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
public class EmprestimoAtrasadoDTO {

    private Long emprestimoId;
    private Long leitorId;
    private String leitorNome;
    private String leitorEmail;
    private Long livroId;
    private String livroTitulo;
    private Long exemplarId;
    private String codigoPatrimonio;
    private LocalDate dataEmprestimo;
    private LocalDate dataPrevistaDevolucao;
    private long diasAtraso;
    private BigDecimal multaEstimada;
}
