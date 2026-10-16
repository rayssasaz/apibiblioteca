package com.apim.apibiblioteca.dto.emprestimo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmprestimoRequestDTO {

    @NotNull(message = "O ID do exemplar é obrigatório")
    private Long exemplarId;

    @NotNull(message = "O ID do leitor é obrigatório")
    private Long leitorId;
}
