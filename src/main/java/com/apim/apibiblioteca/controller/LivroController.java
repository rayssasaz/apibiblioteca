package com.apim.apibiblioteca.controller;

import com.apim.apibiblioteca.dto.livro.LivroRequestDTO;
import com.apim.apibiblioteca.dto.livro.LivroResponseDTO;
import com.apim.apibiblioteca.dto.page.PagedResponse;
import com.apim.apibiblioteca.service.LivroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/livros")
@Tag(name = "2. Livros (Acervo)", description = "Endpoints para cadastro, manutenção (RF03) e busca paginada de livros (RF05)")
public class LivroController {

    private final LivroService livroService;

    public LivroController(LivroService livroService) {
        this.livroService = livroService;
    }

    @Operation(
            summary = "Cadastrar novo livro (RF03)",
            description = "Exclusivo para BIBLIOTECARIO. Cadastra um novo livro com ISBN único.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Livro cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso restrito ao perfil BIBLIOTECARIO"),
            @ApiResponse(responseCode = "409", description = "ISBN já cadastrado")
    })
    @PostMapping
    public ResponseEntity<LivroResponseDTO> criarLivro(@Valid @RequestBody LivroRequestDTO request) {
        LivroResponseDTO response = livroService.criarLivro(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Buscar livros no acervo com filtros e paginação (RF05)",
            description = "Público. Filtra livros por título, autor ou categoria com paginação e retorna a quantidade de exemplares disponíveis no momento.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Página de livros retornada com sucesso")
    })
    @GetMapping
    public ResponseEntity<PagedResponse<LivroResponseDTO>> buscarLivros(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) String autor,
            @RequestParam(required = false) String categoria,
            @ParameterObject @PageableDefault(size = 10, sort = "titulo") Pageable pageable) {
        return ResponseEntity.ok(livroService.buscarLivros(titulo, autor, categoria, pageable));
    }

    @Operation(
            summary = "Buscar livro por ID (RF05)",
            description = "Público. Retorna os detalhes de um livro específico e sua quantidade de exemplares disponíveis.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Livro encontrado"),
            @ApiResponse(responseCode = "404", description = "Livro não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<LivroResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(livroService.buscarPorId(id));
    }

    @Operation(
            summary = "Editar livro existente (RF03)",
            description = "Exclusivo para BIBLIOTECARIO. Atualiza os dados cadastrais de um livro.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Livro atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Livro não encontrado"),
            @ApiResponse(responseCode = "409", description = "ISBN já pertencente a outro livro")
    })
    @PutMapping("/{id}")
    public ResponseEntity<LivroResponseDTO> atualizarLivro(
            @PathVariable Long id,
            @Valid @RequestBody LivroRequestDTO request) {
        return ResponseEntity.ok(livroService.atualizarLivro(id, request));
    }

    @Operation(
            summary = "Remover livro do acervo (RF03)",
            description = "Exclusivo para BIBLIOTECARIO. Remove um livro desde que não possua exemplares emprestados/separados ou reservas ativas.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Livro removido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Livro não encontrado"),
            @ApiResponse(responseCode = "409", description = "Livro possui exemplares emprestados ou reservas ativas")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removerLivro(@PathVariable Long id) {
        livroService.removerLivro(id);
        return ResponseEntity.noContent().build();
    }
}
