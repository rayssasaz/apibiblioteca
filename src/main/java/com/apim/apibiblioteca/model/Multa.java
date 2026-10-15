package com.apim.apibiblioteca.model;

import com.apim.apibiblioteca.model.enumerations.StatusMulta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "multas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Multa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emprestimo_id", nullable = false, unique = true)
    private Emprestimo emprestimo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private long diasAtraso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusMulta status;

    @Column(nullable = false)
    private LocalDate dataGeracao;

    @Column
    private LocalDate dataPagamento;

    @PrePersist
    public void onCreate() {
        if (this.dataGeracao == null) {
            this.dataGeracao = LocalDate.now();
        }
        if (this.status == null) {
            this.status = StatusMulta.PENDENTE;
        }
    }
}
