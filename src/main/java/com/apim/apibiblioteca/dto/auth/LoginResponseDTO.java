package com.apim.apibiblioteca.dto.auth;

import com.apim.apibiblioteca.model.enumerations.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDTO {

    private String token;
    private String tipo;
    private Long id;
    private String nome;
    private String email;
    private Role role;
}
