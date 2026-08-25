package ar.com.ramallo.gestionalumnos.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "historial_grupo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialGrupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "inscripcion_id", nullable = false)
    private Inscripcion inscripcion;

    @ManyToOne
    @JoinColumn(name = "grupo_id", nullable = false)
    private Grupo grupo;

    @Column(nullable = false)
    private LocalDate fechaDesde;

    private LocalDate fechaHasta;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HistorialGrupo historialGrupo)) return false;
        return id != null && id.equals(historialGrupo.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}