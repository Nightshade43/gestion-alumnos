package ar.com.ramallo.gestionalumnos.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "institucion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Institucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Institucion institucion)) return false;
        return id != null && id.equals(institucion.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}