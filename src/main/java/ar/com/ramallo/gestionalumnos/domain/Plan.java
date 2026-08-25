package ar.com.ramallo.gestionalumnos.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "plan", uniqueConstraints = @UniqueConstraint(columnNames = {"programa_id", "codigo"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1)
    private String codigo;

    @Column(nullable = false)
    private Integer moduloInicio;

    @ManyToOne
    @JoinColumn(name = "programa_id", nullable = false)
    private Programa programa;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Plan plan)) return false;
        return id != null && id.equals(plan.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}