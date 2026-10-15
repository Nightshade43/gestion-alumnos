package ar.com.ramallo.gestionalumnos.web;

import ar.com.ramallo.gestionalumnos.domain.Empresa;
import ar.com.ramallo.gestionalumnos.repository.EmpresaRepository;
import ar.com.ramallo.gestionalumnos.security.JwtAuthenticationEntryPoint;
import ar.com.ramallo.gestionalumnos.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmpresaController.class)
@AutoConfigureMockMvc(addFilters = false)
class EmpresaControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private EmpresaRepository empresaRepository;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void creaUnaEmpresaYDevuelve201() throws Exception {
        when(empresaRepository.save(any(Empresa.class)))
                .thenReturn(Empresa.builder().id(1L).nombre("Acme SA").build());

        mockMvc.perform(post("/api/empresas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Acme SA\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Acme SA"));
    }

    @Test
    void devuelve404SiLaEmpresaNoExiste() throws Exception {
        when(empresaRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/empresas/99")).andExpect(status().isNotFound());
    }

    @Test
    void actualizaNombreYContactoDeUnaEmpresa() throws Exception {
        Empresa existente = Empresa.builder().id(1L).nombre("Acme SA").contacto("viejo@acme.com").build();
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/empresas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Acme SRL\",\"contacto\":\"nuevo@acme.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Acme SRL"))
                .andExpect(jsonPath("$.contacto").value("nuevo@acme.com"));
    }

    @Test
    void actualizarUnaEmpresaInexistenteDevuelve404() throws Exception {
        when(empresaRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/empresas/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Acme SRL\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarConNombreVacioDevuelve400() throws Exception {
        mockMvc.perform(put("/api/empresas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("nombre:")));
    }
}
