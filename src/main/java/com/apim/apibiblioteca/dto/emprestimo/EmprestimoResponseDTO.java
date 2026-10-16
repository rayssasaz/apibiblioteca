package com.apim.apibiblioteca.dto.emprestimo;

import com.apim.apibiblioteca.model.enumerations.StatusEmprestimo;
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
public class EmprestimoResponseDTO {

    private Long id;
    private Long exemplarId;
    private String codigoPatrimonio;
    private Long livroId;
    private String livroTitulo;
    private Long leitorId;
    private String leitorNome;
    private String leitorEmail;
    private LocalDate dataEmprestimo;
    private LocalDate dataPrevistaDevolucao;
    private LocalDate dataDevolucao;
    private boolean renovado;
    private StatusEmprestimo status;
    private boolean atrasado;
    private BigDecimal valorMulta;
}
