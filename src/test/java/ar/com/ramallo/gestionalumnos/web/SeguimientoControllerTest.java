package ar.com.ramallo.gestionalumnos.web;

import ar.com.ramallo.gestionalumnos.domain.Inscripcion;
import ar.com.ramallo.gestionalumnos.domain.Seguimiento;
import ar.com.ramallo.gestionalumnos.exception.CategoriaInvalidaException;
import ar.com.ramallo.gestionalumnos.repository.SeguimientoRepository;
import ar.com.ramallo.gestionalumnos.security.JwtAuthenticationEntryPoint;
import ar.com.ramallo.gestionalumnos.security.JwtAuthenticationFilter;
import ar.com.ramallo.gestionalumnos.service.SeguimientoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SeguimientoController.class)
@AutoConfigureMockMvc(addFilters = false)
class SeguimientoControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private SeguimientoService seguimientoService;
    @MockitoBean private SeguimientoRepository seguimientoRepository;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void creaSeguimientoYDevuelve201() throws Exception {
        Inscripcion inscripcion = Inscripcion.builder().id(1L).build();
        Seguimiento guardado = Seguimiento.builder().id(1L).inscripcion(inscripcion)
                .fecha(LocalDate.parse("2026-03-01")).observacion("Buen progreso").build();
        when(seguimientoService.crearSeguimiento(1L, LocalDate.parse("2026-03-01"), "Buen progreso"))
                .thenReturn(guardado);

        mockMvc.perform(post("/api/seguimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inscripcionId\":1,\"fecha\":\"2026-03-01\",\"observacion\":\"Buen progreso\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.observacion").value("Buen progreso"));
    }

    @Test
    void devuelve422SiLaCategoriaNoEsParticular() throws Exception {
        when(seguimientoService.crearSeguimiento(2L, LocalDate.parse("2026-03-01"), "Observacion"))
                .thenThrow(new CategoriaInvalidaException("Seguimiento solo aplica a inscripciones de categoria PARTICULAR"));

        mockMvc.perform(post("/api/seguimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inscripcionId\":2,\"fecha\":\"2026-03-01\",\"observacion\":\"Observacion\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void rechazaObservacionVaciaConBadRequest() throws Exception {
        mockMvc.perform(post("/api/seguimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inscripcionId\":1,\"fecha\":\"2026-03-01\",\"observacion\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listaSeguimientosPorInscripcion() throws Exception {
        Inscripcion inscripcion = Inscripcion.builder().id(1L).build();
        when(seguimientoRepository.findByInscripcionIdOrderByFechaDesc(1L)).thenReturn(List.of(
                Seguimiento.builder().id(1L).inscripcion(inscripcion).fecha(LocalDate.now()).observacion("Obs 1").build()));

        mockMvc.perform(get("/api/seguimientos").param("inscripcionId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}