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
public class IndisponibilizarExemplarDTO {

    @NotBlank(message = "O motivo da indisponibilidade é obrigatório")
    @Size(max = 255, message = "O motivo deve ter no máximo 255 caracteres")
    private String motivo;
}
