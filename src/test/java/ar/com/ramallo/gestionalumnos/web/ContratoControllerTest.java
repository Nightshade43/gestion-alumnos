package ar.com.ramallo.gestionalumnos.web;

import ar.com.ramallo.gestionalumnos.domain.*;
import ar.com.ramallo.gestionalumnos.domain.enums.*;
import ar.com.ramallo.gestionalumnos.exception.CategoriaInvalidaException;
import ar.com.ramallo.gestionalumnos.repository.ContratoRepository;
import ar.com.ramallo.gestionalumnos.repository.InscripcionRepository;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ContratoController.class)
@AutoConfigureMockMvc(addFilters = false)
class ContratoControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ContratoService contratoService;
    @MockitoBean private ContratoRepository contratoRepository;
    @MockitoBean private InscripcionRepository inscripcionRepository;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private Inscripcion inscripcionDe(Long id, String nombrePersona) {
        Persona persona = Persona.builder().id(id).nombre(nombrePersona).build();
        return Inscripcion.builder().id(id).persona(persona).fechaInicio(LocalDate.now()).build();
    }

    private Contrato contratoDe(Long id, Empresa empresa, Integer contratadas, Integer consumidas) {
        return Contrato.builder().id(id).empresa(empresa).tipoFacturacion(TipoFacturacion.PAQUETE)
                .clasesContratadas(contratadas).clasesConsumidas(consumidas).estado(EstadoContrato.ACTIVO).build();
    }

    @Test
    void creaContratoIndividualYDevuelve201() throws Exception {
        Contrato contrato = contratoDe(1L, null, 10, 0);
        when(contratoService.crearContratoIndividual(5L, TipoFacturacion.PAQUETE, 10)).thenReturn(contrato);
        when(inscripcionRepository.findByContratoId(1L)).thenReturn(List.of(inscripcionDe(5L, "Martin Sosa")));

        mockMvc.perform(post("/api/contratos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inscripcionId\":5,\"tipoFacturacion\":\"PAQUETE\",\"clasesContratadas\":10}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inscripciones[0].personaNombre").value("Martin Sosa"));
    }

    @Test
    void creaContratoDeEmpresaCubriendoVariosEmpleados() throws Exception {
        Empresa empresa = Empresa.builder().id(1L).nombre("Acme SA").build();
        Contrato contrato = contratoDe(2L, empresa, 50, 0);
        when(contratoService.crearContratoEmpresa(1L, List.of(5L, 6L), TipoFacturacion.PAQUETE, 50))
                .thenReturn(contrato);
        when(inscripcionRepository.findByContratoId(2L)).thenReturn(List.of(
                inscripcionDe(5L, "Martin Sosa"), inscripcionDe(6L, "Julia Torres")));

        mockMvc.perform(post("/api/contratos/empresa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"empresaId\":1,\"tipoFacturacion\":\"PAQUETE\",\"clasesContratadas\":50,\"inscripcionIds\":[5,6]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.empresaNombre").value("Acme SA"))
                .andExpect(jsonPath("$.inscripciones.length()").value(2));
    }

    @Test
    void devuelve422SiUnaInscripcionEsEscolarAlCrearContratoDeEmpresa() throws Exception {
        when(contratoService.crearContratoEmpresa(1L, List.of(5L), TipoFacturacion.PAQUETE, 50))
                .thenThrow(new CategoriaInvalidaException("Contrato solo aplica a inscripciones de categoria PARTICULAR"));

        mockMvc.perform(post("/api/contratos/empresa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"empresaId\":1,\"tipoFacturacion\":\"PAQUETE\",\"clasesContratadas\":50,\"inscripcionIds\":[5]}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void ampliaElCupoDeUnContrato() throws Exception {
        Contrato contrato = contratoDe(1L, null, 70, 48);
        when(contratoService.ampliarCupo(1L, 20)).thenReturn(contrato);
        when(inscripcionRepository.findByContratoId(1L)).thenReturn(List.of());

        mockMvc.perform(post("/api/contratos/1/ampliar-cupo").param("clasesAdicionales", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clasesContratadas").value(70));
    }
}