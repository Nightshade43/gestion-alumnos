package ar.com.ramallo.gestionalumnos.repository;

import ar.com.ramallo.gestionalumnos.domain.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void guardaYEncuentraUsuarioPorUsername() {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .username("lucas.ramallo")
                .passwordHash("$2a$10$hashDeEjemploNoRealParaElTest")
                .build());

        Optional<Usuario> encontrado = usuarioRepository.findByUsername("lucas.ramallo");

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getId()).isEqualTo(usuario.getId());
    }

    @Test
    void noEncuentraUsuarioConUsernameInexistente() {
        Optional<Usuario> encontrado = usuarioRepository.findByUsername("no_existe");

        assertThat(encontrado).isEmpty();
    }

    @Test
    void rechazaUsernameDuplicado() {
        usuarioRepository.save(Usuario.builder()
                .username("duplicado").passwordHash("hash1").build());

        Usuario duplicado = Usuario.builder().username("duplicado").passwordHash("hash2").build();

        assertThrows(DataIntegrityViolationException.class, () -> usuarioRepository.saveAndFlush(duplicado));
    }
}