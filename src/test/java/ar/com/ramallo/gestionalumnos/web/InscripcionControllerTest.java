package ar.com.ramallo.gestionalumnos.web;

import ar.com.ramallo.gestionalumnos.domain.*;
import ar.com.ramallo.gestionalumnos.domain.enums.*;
import ar.com.ramallo.gestionalumnos.exception.*;
import ar.com.ramallo.gestionalumnos.repository.GrupoRepository;
import ar.com.ramallo.gestionalumnos.repository.InscripcionRepository;
import ar.com.ramallo.gestionalumnos.security.JwtAuthenticationEntryPoint;
import ar.com.ramallo.gestionalumnos.security.JwtAuthenticationFilter;
import ar.com.ramallo.gestionalumnos.service.InscripcionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InscripcionController.class)
@AutoConfigureMockMvc(addFilters = false)
class InscripcionControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private InscripcionService inscripcionService;
    @MockitoBean private InscripcionRepository inscripcionRepository;
    @MockitoBean private GrupoRepository grupoRepository;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private Inscripcion inscripcionDe(Long id, EstadoInscripcion estado) {
        Persona persona = Persona.builder().id(1L).nombre("Ana Gomez").build();
        Programa programa = Programa.builder().id(1L).nombre("Ingles Base").categoria(CategoriaPrograma.ESCOLAR)
                .estrategiaEvaluacion(EstrategiaEvaluacion.CENMA_BASE).build();
        return Inscripcion.builder().id(id).persona(persona).programa(programa)
                .fechaInicio(LocalDate.now()).estado(estado).build();
    }

    @Test
    void creaInscripcionYDevuelve201() throws Exception {
        when(inscripcionService.crearInscripcion(1L, 1L, null, null, LocalDate.parse("2026-03-01")))
                .thenReturn(inscripcionDe(10L, EstadoInscripcion.ACTIVA));

        mockMvc.perform(post("/api/inscripciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personaId\":1,\"programaId\":1,\"fechaInicio\":\"2026-03-01\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.estado").value("ACTIVA"));
    }

    @Test
    void devuelve409SiLaPersonaYaTieneInscripcionEscolar() throws Exception {
        when(inscripcionService.crearInscripcion(anyLong(), anyLong(), any(), any(), any(LocalDate.class)))
                .thenThrow(new RegistroDuplicadoException("La persona ya tiene una inscripcion escolar activa"));

        mockMvc.perform(post("/api/inscripciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personaId\":1,\"programaId\":1,\"fechaInicio\":\"2026-03-01\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void devuelve422SiFaltanModulosAlFinalizar() throws Exception {
        when(inscripcionService.finalizar(10L))
                .thenThrow(new RequisitosAcademicosIncompletosException("Faltan modulos por aprobar"));

        mockMvc.perform(post("/api/inscripciones/10/finalizar"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void devuelve409SiLaTransicionDeEstadoEsInvalida() throws Exception {
        when(inscripcionService.pausar(10L))
                .thenThrow(new EstadoInvalidoException("Transicion invalida: FINALIZADA -> PAUSADA"));

        mockMvc.perform(post("/api/inscripciones/10/pausar"))
                .andExpect(status().isConflict());
    }

    @Test
    void devuelve404SiLaInscripcionNoExisteAlPausar() throws Exception {
        when(inscripcionService.pausar(999L))
                .thenThrow(new RecursoNoEncontradoException("Inscripcion no encontrada: 999"));

        mockMvc.perform(post("/api/inscripciones/999/pausar"))
                .andExpect(status().isNotFound());
    }

    @Test
    void reanudaUnaInscripcion() throws Exception {
        when(inscripcionService.reanudar(10L)).thenReturn(inscripcionDe(10L, EstadoInscripcion.ACTIVA));

        mockMvc.perform(post("/api/inscripciones/10/reanudar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVA"));
    }

    @Test
    void cancelaUnaInscripcion() throws Exception {
        when(inscripcionService.cancelar(10L)).thenReturn(inscripcionDe(10L, EstadoInscripcion.CANCELADA));

        mockMvc.perform(post("/api/inscripciones/10/cancelar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
    }

    @Test
    void cambiaElGrupoDeUnaInscripcion() throws Exception {
        Grupo grupo = Grupo.builder().id(2L).dia("Martes").horario("19:00-21:00").build();
        Inscripcion actualizada = inscripcionDe(10L, EstadoInscripcion.ACTIVA);
        actualizada.setGrupo(grupo);
        when(grupoRepository.findById(2L)).thenReturn(Optional.of(grupo));
        when(inscripcionService.cambiarGrupo(10L, grupo)).thenReturn(actualizada);

        mockMvc.perform(patch("/api/inscripciones/10/grupo").param("grupoId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grupoDia").value("Martes"));
    }

    @Test
    void devuelve404SiElGrupoNoExisteAlCambiarGrupo() throws Exception {
        when(grupoRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/inscripciones/10/grupo").param("grupoId", "99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listaInscripcionesPorPersona() throws Exception {
        when(inscripcionRepository.findByPersonaId(1L))
                .thenReturn(List.of(inscripcionDe(10L, EstadoInscripcion.ACTIVA)));

        mockMvc.perform(get("/api/inscripciones").param("personaId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void listaTodasLasInscripcionesSinFiltro() throws Exception {
        when(inscripcionRepository.findAll())
                .thenReturn(List.of(inscripcionDe(10L, EstadoInscripcion.ACTIVA)));

        mockMvc.perform(get("/api/inscripciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(inscripcionRepository, never()).findByPersonaId(any());
    }

    @Test
    void filtraInscripcionesPorCategoria() throws Exception {
        when(inscripcionRepository.findAll())
                .thenReturn(List.of(inscripcionDe(10L, EstadoInscripcion.ACTIVA)));

        mockMvc.perform(get("/api/inscripciones").param("categoria", "PARTICULAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0)); // inscripcionDe() usa programa ESCOLAR
    }
}