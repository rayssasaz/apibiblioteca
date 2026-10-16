package com.apim.apibiblioteca.dto.relatorio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LivroMaisEmprestadoDTO {

    private Long livroId;
    private String titulo;
    private String autor;
    private String isbn;
    private String categoria;
    private long totalEmprestimos;
}
