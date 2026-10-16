package com.apim.apibiblioteca.dto.livro;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LivroResponseDTO {

    private Long id;
    private String titulo;
    private String autor;
    private String isbn;
    private String categoria;
    private Integer anoPublicacao;
    private long quantidadeDisponivel;
}
