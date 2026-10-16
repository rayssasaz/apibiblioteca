package com.apim.apibiblioteca.controller;

import com.apim.apibiblioteca.dto.emprestimo.EmprestimoRequestDTO;
import com.apim.apibiblioteca.dto.emprestimo.EmprestimoResponseDTO;
import com.apim.apibiblioteca.model.UserAuthenticated;
import com.apim.apibiblioteca.model.enumerations.StatusEmprestimo;
import com.apim.apibiblioteca.service.EmprestimoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/emprestimos")
@Tag(name = "4. Empréstimos", description = "Endpoints para registro de empréstimos (RF06), devolução (RF07), renovação (RF08) e consultas (RF10)")
@SecurityRequirement(name = "bearerAuth")
public class EmprestimoController {

    private final EmprestimoService emprestimoService;

    public EmprestimoController(EmprestimoService emprestimoService) {
        this.emprestimoService = emprestimoService;
    }

    @Operation(
            summary = "Registrar empréstimo de exemplar (RF06)",
            description = "Exclusivo para BIBLIOTECARIO. Registra empréstimo de exemplar disponível por 14 dias (máx. 3 ativos por leitor não bloqueado).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Empréstimo registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Leitor ou exemplar não encontrado"),
            @ApiResponse(responseCode = "409", description = "Leitor bloqueado, limite atingido ou exemplar indisponível")
    })
    @PostMapping
    public ResponseEntity<EmprestimoResponseDTO> registrarEmprestimo(
            @Valid @RequestBody EmprestimoRequestDTO request) {
        EmprestimoResponseDTO response = emprestimoService.registrarEmprestimo(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Registrar devolução de exemplar (RF07)",
            description = "Exclusivo para BIBLIOTECARIO. Finaliza o empréstimo, gera multa de R$ 1,00/dia se houver atraso e separa o exemplar caso haja fila de reserva.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Devolução registrada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Empréstimo não encontrado"),
            @ApiResponse(responseCode = "409", description = "Empréstimo já devolvido anteriormente")
    })
    @PatchMapping("/{id}/devolucao")
    public ResponseEntity<EmprestimoResponseDTO> registrarDevolucao(@PathVariable Long id) {
        return ResponseEntity.ok(emprestimoService.registrarDevolucao(id));
    }

    @Operation(
            summary = "Renovar próprio empréstimo ativo (RF08)",
            description = "Exclusivo para LEITOR dono do empréstimo. Estende o prazo em +7 dias uma única vez, desde que sem atraso, sem reservas para o livro e leitor não bloqueado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Empréstimo renovado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Tentativa de renovar empréstimo de outro leitor"),
            @ApiResponse(responseCode = "404", description = "Empréstimo não encontrado"),
            @ApiResponse(responseCode = "409", description = "Empréstimo já renovado, atrasado, leitor bloqueado ou livro com reserva ativa")
    })
    @PatchMapping("/{id}/renovar")
    public ResponseEntity<EmprestimoResponseDTO> renovarEmprestimo(
            @PathVariable Long id,
            @AuthenticationPrincipal UserAuthenticated userAuthenticated) {
        return ResponseEntity.ok(emprestimoService.renovarEmprestimo(id, userAuthenticated.getUsuario()));
    }

    @Operation(
            summary = "Consultar meus empréstimos (RF10)",
            description = "Exclusivo para LEITOR. Retorna todos os empréstimos do próprio leitor autenticado (ativos, devolvidos e em atraso).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de empréstimos do leitor retornada com sucesso")
    })
    @GetMapping("/meus")
    public ResponseEntity<List<EmprestimoResponseDTO>> listarMeusEmprestimos(
            @AuthenticationPrincipal UserAuthenticated userAuthenticated) {
        return ResponseEntity.ok(emprestimoService.listarMeusEmprestimos(userAuthenticated.getUsuario()));
    }

    @Operation(
            summary = "Consultar empréstimos de qualquer leitor com filtros (RF10)",
            description = "Exclusivo para BIBLIOTECARIO. Permite filtrar por leitorId, status (ATIVO, DEVOLVIDO) e atrasados (true/false).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de empréstimos retornada com sucesso")
    })
    @GetMapping
    public ResponseEntity<List<EmprestimoResponseDTO>> listarTodosEmprestimos(
            @RequestParam(required = false) Long leitorId,
            @RequestParam(required = false) StatusEmprestimo status,
            @RequestParam(required = false) Boolean atrasados) {
        return ResponseEntity.ok(emprestimoService.listarTodosEmprestimos(leitorId, status, atrasados));
    }
}
