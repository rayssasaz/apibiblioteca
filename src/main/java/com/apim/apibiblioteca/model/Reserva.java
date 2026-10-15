package com.apim.apibiblioteca.model;

import com.apim.apibiblioteca.model.enumerations.StatusReserva;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "livro_id", nullable = false)
    private Livro livro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leitor_id", nullable = false)
    private Usuario leitor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exemplar_separado_id")
    private Exemplar exemplarSeparado;

    @Column(nullable = false)
    private LocalDateTime dataReserva;

    @Column
    private LocalDate dataLimiteRetirada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusReserva status;

    @PrePersist
    public void onCreate() {
        if (this.dataReserva == null) {
            this.dataReserva = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = StatusReserva.AGUARDANDO;
        }
    }
}
