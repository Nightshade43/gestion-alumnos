package ar.com.ramallo.gestionalumnos.web;

import ar.com.ramallo.gestionalumnos.domain.Plan;
import ar.com.ramallo.gestionalumnos.domain.Programa;
import ar.com.ramallo.gestionalumnos.domain.enums.CategoriaPrograma;
import ar.com.ramallo.gestionalumnos.domain.enums.EstrategiaEvaluacion;
import ar.com.ramallo.gestionalumnos.repository.PlanRepository;
import ar.com.ramallo.gestionalumnos.repository.ProgramaRepository;
import ar.com.ramallo.gestionalumnos.security.JwtAuthenticationEntryPoint;
import ar.com.ramallo.gestionalumnos.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlanController.class)
@AutoConfigureMockMvc(addFilters = false)
class PlanControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private PlanRepository planRepository;
    @MockitoBean private ProgramaRepository programaRepository;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void creaUnPlanYDevuelve201() throws Exception {
        Programa programa = Programa.builder().id(1L).nombre("Ingles Sede")
                .categoria(CategoriaPrograma.ESCOLAR).estrategiaEvaluacion(EstrategiaEvaluacion.CENMA_SEDE).build();
        Plan guardado = Plan.builder().id(1L).codigo("A").moduloInicio(1).programa(programa).build();
        when(programaRepository.findById(1L)).thenReturn(Optional.of(programa));
        when(planRepository.save(any(Plan.class))).thenReturn(guardado);

        mockMvc.perform(post("/api/planes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"programaId\":1,\"codigo\":\"A\",\"moduloInicio\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.programaId").value(1))
                .andExpect(jsonPath("$.codigo").value("A"));
    }

    @Test
    void devuelve409SiElCodigoYaExisteEnEsePrograma() throws Exception {
        Programa programa = Programa.builder().id(1L).build();
        when(programaRepository.findById(1L)).thenReturn(Optional.of(programa));
        when(planRepository.save(any(Plan.class)))
                .thenThrow(new DataIntegrityViolationException("duplicado"));

        mockMvc.perform(post("/api/planes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"programaId\":1,\"codigo\":\"A\",\"moduloInicio\":1}"))
                .andExpect(status().isConflict());
    }
}
