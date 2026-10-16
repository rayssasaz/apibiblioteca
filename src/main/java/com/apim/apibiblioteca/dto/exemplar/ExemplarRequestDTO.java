package com.apim.apibiblioteca.dto.exemplar;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExemplarRequestDTO {

    @NotBlank(message = "O código de patrimônio é obrigatório")
    @Size(max = 50, message = "O código de patrimônio deve ter no máximo 50 caracteres")
    private String codigoPatrimonio;
}
