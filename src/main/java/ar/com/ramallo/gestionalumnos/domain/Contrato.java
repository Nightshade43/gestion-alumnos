package ar.com.ramallo.gestionalumnos.domain;

import ar.com.ramallo.gestionalumnos.domain.enums.EstadoContrato;
import ar.com.ramallo.gestionalumnos.domain.enums.TipoFacturacion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "contrato")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "inscripcion_id", nullable = false, unique = true)
    private Inscripcion inscripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoFacturacion tipoFacturacion;

    private Integer clasesContratadas;

    @Column(nullable = false)
    @Builder.Default
    private Integer clasesConsumidas = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoContrato estado = EstadoContrato.ACTIVO;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Contrato contrato)) return false;
        return id != null && id.equals(contrato.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}