package com.apim.apibiblioteca.controller;

import com.apim.apibiblioteca.dto.relatorio.EmprestimoAtrasadoDTO;
import com.apim.apibiblioteca.dto.relatorio.LivroMaisEmprestadoDTO;
import com.apim.apibiblioteca.dto.relatorio.MultaPorLeitorDTO;
import com.apim.apibiblioteca.service.RelatorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/relatorios")
@Tag(name = "Relatórios", description = "Relatórios gerenciais exclusivos para bibliotecários (RF15, RF16 e RF17)")
@SecurityRequirement(name = "bearerAuth")
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/livros-mais-emprestados")
    @Operation(summary = "Listar os 10 livros mais emprestados em um período (RF15 - Bibliotecário)")
    public ResponseEntity<List<LivroMaisEmprestadoDTO>> relatorioLivrosMaisEmprestados(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        List<LivroMaisEmprestadoDTO> response = relatorioService.relatorioLivrosMaisEmprestados(inicio, fim);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/emprestimos-atrasados")
    @Operation(summary = "Listar todos os empréstimos em atraso ordenados do maior atraso para o menor (RF16 - Bibliotecário)")
    public ResponseEntity<List<EmprestimoAtrasadoDTO>> relatorioEmprestimosAtrasados() {
        List<EmprestimoAtrasadoDTO> response = relatorioService.relatorioEmprestimosAtrasados();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/multas-pendentes-por-leitor")
    @Operation(summary = "Listar o valor total acumulado de multas pendentes agrupado por leitor (RF17 - Bibliotecário)")
    public ResponseEntity<List<MultaPorLeitorDTO>> relatorioMultasPendentesPorLeitor() {
        List<MultaPorLeitorDTO> response = relatorioService.relatorioMultasPendentesPorLeitor();
        return ResponseEntity.ok(response);
    }
}
