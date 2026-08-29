package ar.com.ramallo.gestionalumnos.security;

import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRETO_TEST = "una-clave-de-prueba-con-al-menos-32-caracteres-para-hmac";

    @Test
    void generaYExtraeElUsernameCorrectamente() {
        JwtService jwtService = new JwtService(SECRETO_TEST, 60000);

        String token = jwtService.generarToken("lucas");

        assertThat(jwtService.extraerUsername(token)).isEqualTo("lucas");
    }

    @Test
    void tokenEsValidoParaElUsuarioCorrectoYNoExpirado() {
        JwtService jwtService = new JwtService(SECRETO_TEST, 60000);
        String token = jwtService.generarToken("lucas");

        assertThat(jwtService.esTokenValido(token, "lucas")).isTrue();
    }

    @Test
    void tokenNoEsValidoParaUnUsernameDistinto() {
        JwtService jwtService = new JwtService(SECRETO_TEST, 60000);
        String token = jwtService.generarToken("lucas");

        assertThat(jwtService.esTokenValido(token, "otro_usuario")).isFalse();
    }

    @Test
    void tokenExpiradoNoEsValido() throws InterruptedException {
        JwtService jwtService = new JwtService(SECRETO_TEST, 1);
        String token = jwtService.generarToken("lucas");
        Thread.sleep(10);

        assertThat(jwtService.esTokenValido(token, "lucas")).isFalse();
    }

    @Test
    void rechazaUnTokenFirmadoConOtraClave() {
        JwtService jwtServiceOriginal = new JwtService(SECRETO_TEST, 60000);
        JwtService jwtServiceOtraClave = new JwtService("otra-clave-completamente-distinta-de-32-caracteres", 60000);
        String token = jwtServiceOriginal.generarToken("lucas");

        assertThatThrownBy(() -> jwtServiceOtraClave.extraerUsername(token))
                .isInstanceOf(SignatureException.class);
    }
}