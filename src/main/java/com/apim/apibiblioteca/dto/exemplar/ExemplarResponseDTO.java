package com.apim.apibiblioteca.dto.exemplar;

import com.apim.apibiblioteca.model.enumerations.StatusExemplar;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExemplarResponseDTO {

    private Long id;
    private String codigoPatrimonio;
    private StatusExemplar status;
    private String motivoIndisponibilidade;
    private Long livroId;
    private String livroTitulo;
    private String livroIsbn;
}
