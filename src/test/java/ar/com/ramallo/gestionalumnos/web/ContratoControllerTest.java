package ar.com.ramallo.gestionalumnos.web;

import ar.com.ramallo.gestionalumnos.domain.Contrato;
import ar.com.ramallo.gestionalumnos.domain.Inscripcion;
import ar.com.ramallo.gestionalumnos.domain.enums.EstadoContrato;
import ar.com.ramallo.gestionalumnos.domain.enums.TipoFacturacion;
import ar.com.ramallo.gestionalumnos.exception.CategoriaInvalidaException;
import ar.com.ramallo.gestionalumnos.exception.LimiteClasesExcedidoException;
import ar.com.ramallo.gestionalumnos.exception.RecursoNoEncontradoException;
import ar.com.ramallo.gestionalumnos.repository.ContratoRepository;
import ar.com.ramallo.gestionalumnos.security.JwtAuthenticationEntryPoint;
import ar.com.ramallo.gestionalumnos.security.JwtAuthenticationFilter;
import ar.com.ramallo.gestionalumnos.service.ContratoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ContratoController.class)
@AutoConfigureMockMvc(addFilters = false)

class ContratoControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ContratoService contratoService;
    @MockitoBean private ContratoRepository contratoRepository;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private Contrato contratoDe(Long id, Integer contratadas, Integer consumidas, EstadoContrato estado) {
        Inscripcion inscripcion = Inscripcion.builder().id(1L).build();
        return Contrato.builder().id(id).inscripcion(inscripcion).tipoFacturacion(TipoFacturacion.PAQUETE)
                .clasesContratadas(contratadas).clasesConsumidas(consumidas).estado(estado).build();
    }

    @Test
    void creaContratoYDevuelve201() throws Exception {
        when(contratoService.crearContrato(1L, TipoFacturacion.PAQUETE, 10))
                .thenReturn(contratoDe(5L, 10, 0, EstadoContrato.ACTIVO));

        mockMvc.perform(post("/api/contratos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inscripcionId\":1,\"tipoFacturacion\":\"PAQUETE\",\"clasesContratadas\":10}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.clasesContratadas").value(10));
    }

    @Test
    void devuelve422SiLaCategoriaNoEsParticular() throws Exception {
        when(contratoService.crearContrato(2L, TipoFacturacion.MENSUAL, null))
                .thenThrow(new CategoriaInvalidaException("Contrato solo aplica a inscripciones de categoria PARTICULAR"));

        mockMvc.perform(post("/api/contratos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inscripcionId\":2,\"tipoFacturacion\":\"MENSUAL\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void devuelve404SiLaInscripcionNoExiste() throws Exception {
        when(contratoService.crearContrato(99L, TipoFacturacion.POR_CLASE, null))
                .thenThrow(new RecursoNoEncontradoException("Inscripcion no encontrada: 99"));

        mockMvc.perform(post("/api/contratos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inscripcionId\":99,\"tipoFacturacion\":\"POR_CLASE\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void consumeUnaClase() throws Exception {
        when(contratoService.consumirClase(5L)).thenReturn(contratoDe(5L, 10, 1, EstadoContrato.ACTIVO));

        mockMvc.perform(post("/api/contratos/5/consumir-clase"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clasesConsumidas").value(1));
    }

    @Test
    void devuelve409SiSeSuperaElLimiteDeClases() throws Exception {
        when(contratoService.consumirClase(5L))
                .thenThrow(new LimiteClasesExcedidoException("Se alcanzo el limite de clases contratadas (10)"));

        mockMvc.perform(post("/api/contratos/5/consumir-clase"))
                .andExpect(status().isConflict());
    }

    @Test
    void finalizaUnContrato() throws Exception {
        when(contratoService.finalizar(5L)).thenReturn(contratoDe(5L, 10, 3, EstadoContrato.FINALIZADO));

        mockMvc.perform(post("/api/contratos/5/finalizar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADO"));
    }

    @Test
    void obtieneUnContratoExistente() throws Exception {
        when(contratoRepository.findById(5L)).thenReturn(Optional.of(contratoDe(5L, 10, 2, EstadoContrato.ACTIVO)));

        mockMvc.perform(get("/api/contratos/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clasesConsumidas").value(2));
    }
}