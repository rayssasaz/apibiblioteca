package com.apim.apibiblioteca.controller;

import com.apim.apibiblioteca.dto.emprestimo.EmprestimoResponseDTO;
import com.apim.apibiblioteca.dto.reserva.ReservaRequestDTO;
import com.apim.apibiblioteca.dto.reserva.ReservaResponseDTO;
import com.apim.apibiblioteca.model.UserAuthenticated;
import com.apim.apibiblioteca.service.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reservas")
@Tag(name = "Reservas", description = "Gerenciamento da fila de reservas, retiradas, cancelamentos e expiração (RF11 a RF14)")
@SecurityRequirement(name = "bearerAuth")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping
    @Operation(summary = "Solicitar reserva de um livro sem exemplares disponíveis (RF11 - Leitor)")
    public ResponseEntity<ReservaResponseDTO> criarReserva(
            @Valid @RequestBody ReservaRequestDTO request,
            @AuthenticationPrincipal UserAuthenticated userAuthenticated) {
        ReservaResponseDTO response = reservaService.criarReserva(request, userAuthenticated.getUsuario());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/retirar")
    @Operation(summary = "Registrar a retirada de exemplar separado convertendo a reserva em empréstimo (RF12 - Bibliotecário)")
    public ResponseEntity<EmprestimoResponseDTO> retirarReserva(@PathVariable Long id) {
        EmprestimoResponseDTO response = reservaService.retirarReserva(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar uma reserva ativa (RF13 - Próprio Leitor ou Bibliotecário)")
    public ResponseEntity<ReservaResponseDTO> cancelarReserva(
            @PathVariable Long id,
            @AuthenticationPrincipal UserAuthenticated userAuthenticated) {
        ReservaResponseDTO response = reservaService.cancelarReserva(id, userAuthenticated.getUsuario());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/minhas")
    @Operation(summary = "Consultar as próprias reservas do leitor autenticado (RF11/RF14 - Leitor)")
    public ResponseEntity<List<ReservaResponseDTO>> listarMinhasReservas(
            @AuthenticationPrincipal UserAuthenticated userAuthenticated) {
        List<ReservaResponseDTO> response = reservaService.listarMinhasReservas(userAuthenticated.getUsuario());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/livro/{livroId}/fila")
    @Operation(summary = "Consultar a fila ordenada de reservas ativas de um livro (RF14 - Bibliotecário)")
    public ResponseEntity<List<ReservaResponseDTO>> listarFilaPorLivro(@PathVariable Long livroId) {
        List<ReservaResponseDTO> response = reservaService.listarFilaPorLivro(livroId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/processar-expiradas")
    @Operation(summary = "Processar em lote reservas com prazo de retirada vencido e repassar exemplares à fila (RF14 - Bibliotecário)")
    public ResponseEntity<List<ReservaResponseDTO>> processarReservasExpiradas() {
        List<ReservaResponseDTO> response = reservaService.processarReservasExpiradas();
        return ResponseEntity.ok(response);
    }
}
