package com.apim.apibiblioteca.controller;

import com.apim.apibiblioteca.dto.exemplar.ExemplarRequestDTO;
import com.apim.apibiblioteca.dto.exemplar.ExemplarResponseDTO;
import com.apim.apibiblioteca.dto.exemplar.IndisponibilizarExemplarDTO;
import com.apim.apibiblioteca.service.ExemplarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "3. Exemplares", description = "Endpoints para gestão de exemplares físicos e controle de disponibilidade (RF04)")
@SecurityRequirement(name = "bearerAuth")
public class ExemplarController {

    private final ExemplarService exemplarService;

    public ExemplarController(ExemplarService exemplarService) {
        this.exemplarService = exemplarService;
    }

    @Operation(
            summary = "Adicionar novo exemplar a um livro (RF04)",
            description = "Exclusivo para BIBLIOTECARIO. Cadastra uma nova cópia física com código de patrimônio único.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Exemplar cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Livro não encontrado"),
            @ApiResponse(responseCode = "409", description = "Código de patrimônio já cadastrado")
    })
    @PostMapping("/api/livros/{livroId}/exemplares")
    public ResponseEntity<ExemplarResponseDTO> adicionarExemplar(
            @PathVariable Long livroId,
            @Valid @RequestBody ExemplarRequestDTO request) {
        ExemplarResponseDTO response = exemplarService.adicionarExemplar(livroId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Listar exemplares de um livro (RF04)",
            description = "Exclusivo para BIBLIOTECARIO. Lista todos os exemplares físicos de um livro específico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de exemplares retornada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Livro não encontrado")
    })
    @GetMapping("/api/livros/{livroId}/exemplares")
    public ResponseEntity<List<ExemplarResponseDTO>> listarExemplaresPorLivro(@PathVariable Long livroId) {
        return ResponseEntity.ok(exemplarService.listarExemplaresPorLivro(livroId));
    }

    @Operation(
            summary = "Buscar exemplar por ID (RF04)",
            description = "Exclusivo para BIBLIOTECARIO. Consulta os detalhes e status de um exemplar pelo seu ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Exemplar encontrado"),
            @ApiResponse(responseCode = "404", description = "Exemplar não encontrado")
    })
    @GetMapping("/api/exemplares/{id}")
    public ResponseEntity<ExemplarResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(exemplarService.buscarPorId(id));
    }

    @Operation(
            summary = "Marcar exemplar como indisponível (RF04)",
            description = "Exclusivo para BIBLIOTECARIO. Marca um exemplar como INDISPONIVEL (manutenção/extravio), desde que não esteja emprestado no momento.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Exemplar marcado como indisponível"),
            @ApiResponse(responseCode = "400", description = "Motivo de indisponibilidade não informado"),
            @ApiResponse(responseCode = "404", description = "Exemplar não encontrado"),
            @ApiResponse(responseCode = "409", description = "Exemplar está emprestado ou separado para reserva")
    })
    @PatchMapping("/api/exemplares/{id}/indisponibilizar")
    public ResponseEntity<ExemplarResponseDTO> marcarIndisponivel(
            @PathVariable Long id,
            @Valid @RequestBody IndisponibilizarExemplarDTO request) {
        return ResponseEntity.ok(exemplarService.marcarIndisponivel(id, request));
    }

    @Operation(
            summary = "Reativar exemplar indisponível para disponível (RF04)",
            description = "Exclusivo para BIBLIOTECARIO. Retorna um exemplar INDISPONIVEL após manutenção para o status DISPONIVEL.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Exemplar marcado como disponível"),
            @ApiResponse(responseCode = "404", description = "Exemplar não encontrado"),
            @ApiResponse(responseCode = "409", description = "Exemplar não está no status INDISPONIVEL")
    })
    @PatchMapping("/api/exemplares/{id}/disponibilizar")
    public ResponseEntity<ExemplarResponseDTO> marcarDisponivel(@PathVariable Long id) {
        return ResponseEntity.ok(exemplarService.marcarDisponivel(id));
    }
}
