package ar.com.ramallo.gestionalumnos.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "empresa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Empresa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    private String contacto;

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Empresa empresa)) return false;
        return id != null && id.equals(empresa.id);
    }
    @Override public int hashCode() { return getClass().hashCode(); }
}