package ar.com.ramallo.gestionalumnos.web;

import ar.com.ramallo.gestionalumnos.domain.Institucion;
import ar.com.ramallo.gestionalumnos.repository.InstitucionRepository;
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

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InstitucionController.class)
@AutoConfigureMockMvc(addFilters = false)
class InstitucionControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private InstitucionRepository institucionRepository;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void creaUnaInstitucionYDevuelve201() throws Exception {
        when(institucionRepository.save(any(Institucion.class)))
                .thenReturn(Institucion.builder().id(1L).nombre("CENMA Bo SMATA").build());

        mockMvc.perform(post("/api/instituciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"CENMA Bo SMATA\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("CENMA Bo SMATA"));
    }

    @Test
    void crearConNombreVacioDevuelve400() throws Exception {
        mockMvc.perform(post("/api/instituciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(startsWith("nombre:")));
    }

    @Test
    void obtienePorId() throws Exception {
        when(institucionRepository.findById(1L))
                .thenReturn(Optional.of(Institucion.builder().id(1L).nombre("CENMA Bo SMATA").build()));

        mockMvc.perform(get("/api/instituciones/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("CENMA Bo SMATA"));
    }

    @Test
    void devuelve404SiLaInstitucionNoExiste() throws Exception {
        when(institucionRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/instituciones/99")).andExpect(status().isNotFound());
    }

    @Test
    void listaTodasLasInstituciones() throws Exception {
        when(institucionRepository.findAll()).thenReturn(List.of(
                Institucion.builder().id(1L).nombre("A").build(),
                Institucion.builder().id(2L).nombre("B").build()));

        mockMvc.perform(get("/api/instituciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void actualizaElNombreDeUnaInstitucion() throws Exception {
        when(institucionRepository.findById(1L))
                .thenReturn(Optional.of(Institucion.builder().id(1L).nombre("CENMA Bo SMATA").build()));
        when(institucionRepository.save(any(Institucion.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/instituciones/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"CENMA Barrio SMATA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("CENMA Barrio SMATA"));
    }

    @Test
    void actualizarUnaInstitucionInexistenteDevuelve404() throws Exception {
        when(institucionRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/instituciones/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"X\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarConNombreVacioDevuelve400() throws Exception {
        mockMvc.perform(put("/api/instituciones/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(startsWith("nombre:")));
    }

    @Test
    void unVerboNoSoportadoDevuelve405() throws Exception {
        mockMvc.perform(delete("/api/instituciones/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("Método DELETE no permitido para esta ruta"));
    }

    @Test
    void unContentTypeQueNoEsJsonDevuelve415() throws Exception {
        mockMvc.perform(post("/api/instituciones")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hola"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message").value("Content-Type 'text/plain' no soportado (se espera application/json)"));
    }

    @Test
    void unaRutaInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/noexiste"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ruta no encontrada: /api/noexiste"));
    }
}
