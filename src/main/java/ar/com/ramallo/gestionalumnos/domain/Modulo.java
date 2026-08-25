package ar.com.ramallo.gestionalumnos.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "modulo", uniqueConstraints = @UniqueConstraint(columnNames = {"programa_id", "orden"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Modulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer orden;

    @Column(nullable = false)
    private boolean esSecuencial;

    @ManyToOne
    @JoinColumn(name = "programa_id", nullable = false)
    private Programa programa;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Modulo modulo)) return false;
        return id != null && id.equals(modulo.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}