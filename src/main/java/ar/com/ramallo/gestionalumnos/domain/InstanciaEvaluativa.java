package ar.com.ramallo.gestionalumnos.domain;

import ar.com.ramallo.gestionalumnos.domain.enums.TipoInstanciaEvaluativa;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "instancia_evaluativa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstanciaEvaluativa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "inscripcion_id", nullable = false)
    private Inscripcion inscripcion;

    @ManyToOne
    @JoinColumn(name = "modulo_id", nullable = false)
    private Modulo modulo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoInstanciaEvaluativa tipo;

    private BigDecimal nota;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    @Builder.Default
    private boolean cuentaParaPromedio = true;

    @ManyToOne
    @JoinColumn(name = "recupera_a_id")
    private InstanciaEvaluativa recuperaA;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InstanciaEvaluativa that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}