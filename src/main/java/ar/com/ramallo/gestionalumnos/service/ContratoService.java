package ar.com.ramallo.gestionalumnos.service;

import ar.com.ramallo.gestionalumnos.domain.Contrato;
import ar.com.ramallo.gestionalumnos.domain.Empresa;
import ar.com.ramallo.gestionalumnos.domain.Inscripcion;
import ar.com.ramallo.gestionalumnos.domain.enums.CategoriaPrograma;
import ar.com.ramallo.gestionalumnos.domain.enums.EstadoContrato;
import ar.com.ramallo.gestionalumnos.domain.enums.TipoFacturacion;
import ar.com.ramallo.gestionalumnos.exception.*;
import ar.com.ramallo.gestionalumnos.repository.ContratoRepository;
import ar.com.ramallo.gestionalumnos.repository.EmpresaRepository;
import ar.com.ramallo.gestionalumnos.repository.InscripcionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContratoService {

    private final ContratoRepository contratoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final EmpresaRepository empresaRepository;

    @Transactional
    public Contrato crearContratoIndividual(Long inscripcionId, TipoFacturacion tipoFacturacion, Integer clasesContratadas) {
        Inscripcion inscripcion = inscripcionRepository.findById(inscripcionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Inscripcion no encontrada: " + inscripcionId));
        validarParticularSinContrato(inscripcion);

        Contrato contrato = contratoRepository.save(Contrato.builder()
                .tipoFacturacion(tipoFacturacion).clasesContratadas(clasesContratadas).build());

        inscripcion.setContrato(contrato);
        inscripcionRepository.save(inscripcion);
        return contrato;
    }

    @Transactional
    public Contrato crearContratoEmpresa(Long empresaId, List<Long> inscripcionIds,
                                         TipoFacturacion tipoFacturacion, Integer clasesContratadas) {
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Empresa no encontrada: " + empresaId));

        List<Inscripcion> inscripciones = inscripcionIds.stream()
                .map(id -> inscripcionRepository.findById(id)
                        .orElseThrow(() -> new RecursoNoEncontradoException("Inscripcion no encontrada: " + id)))
                .peek(this::validarParticularSinContrato)
                .toList();

        Contrato contrato = contratoRepository.save(Contrato.builder()
                .empresa(empresa).tipoFacturacion(tipoFacturacion).clasesContratadas(clasesContratadas).build());

        inscripciones.forEach(i -> i.setContrato(contrato));
        inscripcionRepository.saveAll(inscripciones);
        return contrato;
    }

    @Transactional
    public Contrato ampliarCupo(Long contratoId, Integer clasesAdicionales) {
        Contrato contrato = obtener(contratoId);
        contrato.setClasesContratadas(contrato.getClasesContratadas() + clasesAdicionales);
        return contratoRepository.save(contrato);
    }

    private void validarParticularSinContrato(Inscripcion inscripcion) {
        if (inscripcion.getPrograma().getCategoria() != CategoriaPrograma.PARTICULAR) {
            throw new CategoriaInvalidaException("Contrato solo aplica a inscripciones de categoria PARTICULAR");
        }
        if (inscripcion.getContrato() != null) {
            throw new RegistroDuplicadoException("La inscripcion ya tiene un contrato asociado");
        }
    }

    @Transactional
    public Contrato consumirClase(Long contratoId) {
        Contrato contrato = obtener(contratoId);

        if (contrato.getEstado() == EstadoContrato.FINALIZADO) {
            throw new EstadoInvalidoException("No se puede consumir una clase de un contrato finalizado");
        }

        if (contrato.getTipoFacturacion() == TipoFacturacion.PAQUETE
                && contrato.getClasesConsumidas() + 1 > contrato.getClasesContratadas()) {
            throw new LimiteClasesExcedidoException(
                    "Se alcanzo el limite de clases contratadas (" + contrato.getClasesContratadas() + ")");
        }

        contrato.setClasesConsumidas(contrato.getClasesConsumidas() + 1);
        return contratoRepository.save(contrato);
    }

    @Transactional
    public Contrato finalizar(Long contratoId) {
        Contrato contrato = obtener(contratoId);
        contrato.setEstado(EstadoContrato.FINALIZADO);
        return contratoRepository.save(contrato);
    }

    private Contrato obtener(Long contratoId) {
        return contratoRepository.findById(contratoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Contrato no encontrado: " + contratoId));
    }
}