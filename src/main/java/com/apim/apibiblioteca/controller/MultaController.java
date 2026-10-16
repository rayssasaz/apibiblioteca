package com.apim.apibiblioteca.controller;

import com.apim.apibiblioteca.dto.multa.MultaResponseDTO;
import com.apim.apibiblioteca.model.UserAuthenticated;
import com.apim.apibiblioteca.model.enumerations.StatusMulta;
import com.apim.apibiblioteca.service.MultaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/multas")
@Tag(name = "5. Multas", description = "Endpoints para pagamento de multas pelo bibliotecário (RF09) e consulta de multas pelo leitor (RF10)")
@SecurityRequirement(name = "bearerAuth")
public class MultaController {

    private final MultaService multaService;

    public MultaController(MultaService multaService) {
        this.multaService = multaService;
    }

    @Operation(
            summary = "Registrar pagamento de multa (RF09)",
            description = "Exclusivo para BIBLIOTECARIO. Registra o pagamento de uma multa pendente e desbloqueia o leitor caso não haja outras pendências.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Multa paga com sucesso"),
            @ApiResponse(responseCode = "404", description = "Multa não encontrada"),
            @ApiResponse(responseCode = "409", description = "Multa já paga anteriormente")
    })
    @PatchMapping("/{id}/pagar")
    public ResponseEntity<MultaResponseDTO> pagarMulta(@PathVariable Long id) {
        return ResponseEntity.ok(multaService.pagarMulta(id));
    }

    @Operation(
            summary = "Consultar minhas multas (RF10)",
            description = "Exclusivo para LEITOR. Retorna as multas do próprio leitor autenticado (podendo filtrar por status PENDENTE ou PAGA).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de multas do leitor retornada com sucesso")
    })
    @GetMapping("/minhas")
    public ResponseEntity<List<MultaResponseDTO>> listarMinhasMultas(
            @AuthenticationPrincipal UserAuthenticated userAuthenticated,
            @RequestParam(required = false) StatusMulta status) {
        return ResponseEntity.ok(multaService.listarMinhasMultas(userAuthenticated.getUsuario(), status));
    }

    @Operation(
            summary = "Consultar multas de qualquer leitor (RF10)",
            description = "Exclusivo para BIBLIOTECARIO. Lista multas com filtros opcionais por leitorId e status.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de multas retornada com sucesso")
    })
    @GetMapping
    public ResponseEntity<List<MultaResponseDTO>> listarTodasMultas(
            @RequestParam(required = false) Long leitorId,
            @RequestParam(required = false) StatusMulta status) {
        return ResponseEntity.ok(multaService.listarTodasMultas(leitorId, status));
    }
}
