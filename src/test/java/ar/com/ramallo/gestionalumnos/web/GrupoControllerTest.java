package ar.com.ramallo.gestionalumnos.web;

import ar.com.ramallo.gestionalumnos.domain.Grupo;
import ar.com.ramallo.gestionalumnos.domain.Programa;
import ar.com.ramallo.gestionalumnos.domain.enums.CategoriaPrograma;
import ar.com.ramallo.gestionalumnos.domain.enums.EstrategiaEvaluacion;
import ar.com.ramallo.gestionalumnos.repository.GrupoRepository;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GrupoController.class)
@AutoConfigureMockMvc(addFilters = false)
class GrupoControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private GrupoRepository grupoRepository;
    @MockitoBean private ProgramaRepository programaRepository;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void creaUnGrupoYDevuelve201() throws Exception {
        Programa programa = Programa.builder().id(1L).nombre("Ingles Sede")
                .categoria(CategoriaPrograma.ESCOLAR).estrategiaEvaluacion(EstrategiaEvaluacion.CENMA_SEDE).build();
        Grupo guardado = Grupo.builder().id(1L).dia("Miercoles").horario("19-21").programa(programa).build();
        when(programaRepository.findById(1L)).thenReturn(Optional.of(programa));
        when(grupoRepository.save(any(Grupo.class))).thenReturn(guardado);

        mockMvc.perform(post("/api/grupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"programaId\":1,\"dia\":\"Miercoles\",\"horario\":\"19-21\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.programaId").value(1))
                .andExpect(jsonPath("$.dia").value("Miercoles"));
    }

    @Test
    void devuelve404SiElProgramaNoExiste() throws Exception {
        when(programaRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/grupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"programaId\":99,\"dia\":\"Miercoles\",\"horario\":\"19-21\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listaLosGruposDeUnPrograma() throws Exception {
        Programa programa = Programa.builder().id(1L).build();
        Grupo g = Grupo.builder().id(1L).dia("Jueves").horario("19-21").programa(programa).build();
        when(grupoRepository.findByProgramaId(1L)).thenReturn(List.of(g));

        mockMvc.perform(get("/api/grupos").param("programaId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dia").value("Jueves"));
    }
}
