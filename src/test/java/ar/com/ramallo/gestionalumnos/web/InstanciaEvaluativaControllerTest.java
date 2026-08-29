package ar.com.ramallo.gestionalumnos.web;

import ar.com.ramallo.gestionalumnos.domain.Inscripcion;
import ar.com.ramallo.gestionalumnos.domain.InstanciaEvaluativa;
import ar.com.ramallo.gestionalumnos.domain.Modulo;
import ar.com.ramallo.gestionalumnos.domain.enums.TipoInstanciaEvaluativa;
import ar.com.ramallo.gestionalumnos.repository.InscripcionRepository;
import ar.com.ramallo.gestionalumnos.repository.InstanciaEvaluativaRepository;
import ar.com.ramallo.gestionalumnos.repository.ModuloRepository;
import ar.com.ramallo.gestionalumnos.security.JwtAuthenticationEntryPoint;
import ar.com.ramallo.gestionalumnos.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InstanciaEvaluativaController.class)
@AutoConfigureMockMvc(addFilters = false)
class InstanciaEvaluativaControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private InstanciaEvaluativaRepository instanciaEvaluativaRepository;
    @MockitoBean private InscripcionRepository inscripcionRepository;
    @MockitoBean private ModuloRepository moduloRepository;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void creaUnaInstanciaEvaluativaConCuentaParaPromedioPorDefecto() throws Exception {
        Inscripcion inscripcion = Inscripcion.builder().id(1L).build();
        Modulo modulo = Modulo.builder().id(1L).build();
        InstanciaEvaluativa guardada = InstanciaEvaluativa.builder().id(1L).inscripcion(inscripcion).modulo(modulo)
                .tipo(TipoInstanciaEvaluativa.NOTA).nota(new BigDecimal("8.00"))
                .fecha(LocalDate.now()).cuentaParaPromedio(true).build();
        when(inscripcionRepository.findById(1L)).thenReturn(Optional.of(inscripcion));
        when(moduloRepository.findById(1L)).thenReturn(Optional.of(modulo));
        when(instanciaEvaluativaRepository.save(any())).thenReturn(guardada);

        mockMvc.perform(post("/api/instancias-evaluativas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inscripcionId\":1,\"moduloId\":1,\"tipo\":\"NOTA\",\"nota\":8.00,\"fecha\":\"2026-03-01\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cuentaParaPromedio").value(true));
    }

    @Test
    void devuelve404SiElModuloNoExiste() throws Exception {
        when(inscripcionRepository.findById(1L)).thenReturn(Optional.of(Inscripcion.builder().id(1L).build()));
        when(moduloRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/instancias-evaluativas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inscripcionId\":1,\"moduloId\":99,\"tipo\":\"NOTA\",\"fecha\":\"2026-03-01\"}"))
                .andExpect(status().isNotFound());
    }
}