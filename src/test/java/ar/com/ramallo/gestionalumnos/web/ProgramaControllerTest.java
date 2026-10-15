package ar.com.ramallo.gestionalumnos.web;

import ar.com.ramallo.gestionalumnos.domain.Grupo;
import ar.com.ramallo.gestionalumnos.domain.Institucion;
import ar.com.ramallo.gestionalumnos.domain.Programa;
import ar.com.ramallo.gestionalumnos.domain.enums.CategoriaPrograma;
import ar.com.ramallo.gestionalumnos.domain.enums.EstadoInscripcion;
import ar.com.ramallo.gestionalumnos.domain.enums.EstrategiaEvaluacion;
import ar.com.ramallo.gestionalumnos.repository.GrupoRepository;
import ar.com.ramallo.gestionalumnos.repository.InscripcionRepository;
import ar.com.ramallo.gestionalumnos.repository.InstitucionRepository;
import ar.com.ramallo.gestionalumnos.repository.ModuloRepository;
import ar.com.ramallo.gestionalumnos.repository.ProgramaRepository;
import ar.com.ramallo.gestionalumnos.security.JwtAuthenticationEntryPoint;
import ar.com.ramallo.gestionalumnos.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProgramaController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProgramaControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ProgramaRepository programaRepository;
    @MockitoBean private InstitucionRepository institucionRepository;
    @MockitoBean private ModuloRepository moduloRepository;
    @MockitoBean private GrupoRepository grupoRepository;
    @MockitoBean private InscripcionRepository inscripcionRepository;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void creaUnProgramaConInstitucion() throws Exception {
        Institucion institucion = Institucion.builder().id(1L).nombre("CENMA Bo SMATA").build();
        Programa guardado = Programa.builder().id(1L).nombre("Ingles Base").categoria(CategoriaPrograma.ESCOLAR)
                .estrategiaEvaluacion(EstrategiaEvaluacion.CENMA_BASE).institucion(institucion).build();
        when(institucionRepository.findById(1L)).thenReturn(Optional.of(institucion));
        when(programaRepository.save(any(Programa.class))).thenReturn(guardado);

        mockMvc.perform(post("/api/programas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ingles Base\",\"categoria\":\"ESCOLAR\","
                                + "\"estrategiaEvaluacion\":\"CENMA_BASE\",\"institucionId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.institucionNombre").value("CENMA Bo SMATA"));
    }

    @Test
    void creaUnProgramaParticularSinInstitucion() throws Exception {
        Programa guardado = Programa.builder().id(2L).nombre("Ingles IT").categoria(CategoriaPrograma.PARTICULAR)
                .estrategiaEvaluacion(EstrategiaEvaluacion.SEGUIMIENTO_LIBRE).build();
        when(programaRepository.save(any(Programa.class))).thenReturn(guardado);

        mockMvc.perform(post("/api/programas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ingles IT\",\"categoria\":\"PARTICULAR\","
                                + "\"estrategiaEvaluacion\":\"SEGUIMIENTO_LIBRE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.institucionId").value(nullValue()));
    }

    @Test
    void devuelve404SiLaInstitucionNoExiste() throws Exception {
        when(institucionRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/programas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ingles Base\",\"categoria\":\"ESCOLAR\","
                                + "\"estrategiaEvaluacion\":\"CENMA_BASE\",\"institucionId\":99}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listaTodosLosProgramas() throws Exception {
        when(programaRepository.findAll()).thenReturn(List.of(
                Programa.builder().id(1L).nombre("A").categoria(CategoriaPrograma.ESCOLAR)
                        .estrategiaEvaluacion(EstrategiaEvaluacion.CENMA_BASE).build()));

        mockMvc.perform(get("/api/programas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void elListadoIncluyeContadoresYDiasDeGrupoSinRepetir() throws Exception {
        Programa programa = Programa.builder().id(1L).nombre("Ingles Sede").categoria(CategoriaPrograma.ESCOLAR)
                .estrategiaEvaluacion(EstrategiaEvaluacion.CENMA_SEDE).build();
        when(programaRepository.findAll()).thenReturn(List.of(programa));
        when(moduloRepository.countByProgramaId(1L)).thenReturn(4L);
        when(inscripcionRepository.countByProgramaIdAndEstado(1L, EstadoInscripcion.ACTIVA)).thenReturn(12L);
        when(grupoRepository.findByProgramaIdOrderByIdAsc(1L)).thenReturn(List.of(
                Grupo.builder().id(1L).dia("Miercoles").horario("18:00").programa(programa).build(),
                Grupo.builder().id(2L).dia("Sabado").horario("09:00").programa(programa).build(),
                Grupo.builder().id(3L).dia("Miercoles").horario("20:00").programa(programa).build()));

        mockMvc.perform(get("/api/programas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cantidadModulos").value(4))
                .andExpect(jsonPath("$[0].cantidadInscripcionesActivas").value(12))
                .andExpect(jsonPath("$[0].diasDeGrupo.length()").value(2))
                .andExpect(jsonPath("$[0].diasDeGrupo[0]").value("Miercoles"))
                .andExpect(jsonPath("$[0].diasDeGrupo[1]").value("Sabado"));
    }

    @Test
    void unProgramaSinModulosNiGruposDevuelveContadoresEnCero() throws Exception {
        Programa programa = Programa.builder().id(2L).nombre("Ingles IT").categoria(CategoriaPrograma.PARTICULAR)
                .estrategiaEvaluacion(EstrategiaEvaluacion.SEGUIMIENTO_LIBRE).build();
        when(programaRepository.findById(2L)).thenReturn(Optional.of(programa));

        mockMvc.perform(get("/api/programas/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadModulos").value(0))
                .andExpect(jsonPath("$.cantidadInscripcionesActivas").value(0))
                .andExpect(jsonPath("$.diasDeGrupo.length()").value(0));
    }
}
