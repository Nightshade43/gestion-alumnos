package ar.com.ramallo.gestionalumnos.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "seguimiento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seguimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "inscripcion_id", nullable = false)
    private Inscripcion inscripcion;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 1000)
    private String observacion;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Seguimiento seguimiento)) return false;
        return id != null && id.equals(seguimiento.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}