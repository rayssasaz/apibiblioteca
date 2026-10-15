package com.apim.apibiblioteca.model;

import com.apim.apibiblioteca.model.enumerations.StatusEmprestimo;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "emprestimos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Emprestimo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exemplar_id", nullable = false)
    private Exemplar exemplar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leitor_id", nullable = false)
    private Usuario leitor;

    @Column(nullable = false)
    private LocalDate dataEmprestimo;

    @Column(nullable = false)
    private LocalDate dataPrevistaDevolucao;

    @Column
    private LocalDate dataDevolucao;

    @Column(nullable = false)
    private boolean renovado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusEmprestimo status;

    @OneToOne(mappedBy = "emprestimo", cascade = CascadeType.ALL)
    private Multa multa;

    @PrePersist
    public void onCreate() {
        if (this.dataEmprestimo == null) {
            this.dataEmprestimo = LocalDate.now();
        }
        if (this.dataPrevistaDevolucao == null) {
            this.dataPrevistaDevolucao = this.dataEmprestimo.plusDays(14);
        }
        if (this.status == null) {
            this.status = StatusEmprestimo.ATIVO;
        }
    }
}
