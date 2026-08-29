package ar.com.ramallo.gestionalumnos.web;

import ar.com.ramallo.gestionalumnos.domain.Persona;
import ar.com.ramallo.gestionalumnos.repository.PersonaRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PersonaController.class)
@AutoConfigureMockMvc(addFilters = false)
class PersonaControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private PersonaRepository personaRepository;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void creaUnaPersonaYDevuelve201() throws Exception {
        Persona guardada = Persona.builder().id(1L).nombre("Ana Gomez").email("ana@mail.com").build();
        when(personaRepository.save(any(Persona.class))).thenReturn(guardada);

        mockMvc.perform(post("/api/personas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana Gomez\",\"email\":\"ana@mail.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Ana Gomez"));
    }

    @Test
    void rechazaCreacionSinNombreConBadRequest() throws Exception {
        mockMvc.perform(post("/api/personas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"sinnombre@mail.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void devuelvePersonaExistente() throws Exception {
        Persona persona = Persona.builder().id(5L).nombre("Carlos Diaz").build();
        when(personaRepository.findById(5L)).thenReturn(Optional.of(persona));

        mockMvc.perform(get("/api/personas/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Carlos Diaz"));
    }

    @Test
    void devuelve404SiLaPersonaNoExiste() throws Exception {
        when(personaRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/personas/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void listaTodasLasPersonas() throws Exception {
        when(personaRepository.findAll()).thenReturn(List.of(
                Persona.builder().id(1L).nombre("Ana").build(),
                Persona.builder().id(2L).nombre("Bruno").build()));

        mockMvc.perform(get("/api/personas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void eliminaUnaPersonaExistente() throws Exception {
        when(personaRepository.findById(3L)).thenReturn(Optional.of(Persona.builder().id(3L).nombre("Diego").build()));

        mockMvc.perform(delete("/api/personas/3"))
                .andExpect(status().isNoContent());

        verify(personaRepository).deleteById(3L);
    }
}