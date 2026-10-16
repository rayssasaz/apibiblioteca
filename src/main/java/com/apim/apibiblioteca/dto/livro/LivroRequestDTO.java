package com.apim.apibiblioteca.dto.livro;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LivroRequestDTO {

    @NotBlank(message = "O título é obrigatório")
    @Size(max = 200, message = "O título deve ter no máximo 200 caracteres")
    private String titulo;

    @NotBlank(message = "O autor é obrigatório")
    @Size(max = 150, message = "O autor deve ter no máximo 150 caracteres")
    private String autor;

    @NotBlank(message = "O ISBN é obrigatório")
    @Size(max = 30, message = "O ISBN deve ter no máximo 30 caracteres")
    private String isbn;

    @NotBlank(message = "A categoria é obrigatória")
    @Size(max = 100, message = "A categoria deve ter no máximo 100 caracteres")
    private String categoria;

    @NotNull(message = "O ano de publicação é obrigatório")
    @Min(value = 1000, message = "O ano de publicação deve ser válido")
    private Integer anoPublicacao;
}
