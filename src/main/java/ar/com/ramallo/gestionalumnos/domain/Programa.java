package ar.com.ramallo.gestionalumnos.domain;

import ar.com.ramallo.gestionalumnos.domain.enums.CategoriaPrograma;
import ar.com.ramallo.gestionalumnos.domain.enums.EstrategiaEvaluacion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "programa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Programa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaPrograma categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstrategiaEvaluacion estrategiaEvaluacion;

    @ManyToOne
    @JoinColumn(name = "institucion_id")
    private Institucion institucion;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Programa programa)) return false;
        return id != null && id.equals(programa.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}