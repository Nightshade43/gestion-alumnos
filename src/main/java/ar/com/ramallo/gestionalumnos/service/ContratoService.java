package ar.com.ramallo.gestionalumnos.service;

import ar.com.ramallo.gestionalumnos.domain.Contrato;
import ar.com.ramallo.gestionalumnos.domain.Inscripcion;
import ar.com.ramallo.gestionalumnos.domain.enums.CategoriaPrograma;
import ar.com.ramallo.gestionalumnos.domain.enums.EstadoContrato;
import ar.com.ramallo.gestionalumnos.domain.enums.TipoFacturacion;
import ar.com.ramallo.gestionalumnos.exception.*;
import ar.com.ramallo.gestionalumnos.repository.ContratoRepository;
import ar.com.ramallo.gestionalumnos.repository.InscripcionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContratoService {

    private final ContratoRepository contratoRepository;
    private final InscripcionRepository inscripcionRepository;

    @Transactional
    public Contrato crearContrato(Long inscripcionId, TipoFacturacion tipoFacturacion, Integer clasesContratadas) {
        Inscripcion inscripcion = inscripcionRepository.findById(inscripcionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Inscripcion no encontrada: " + inscripcionId));

        if (inscripcion.getPrograma().getCategoria() != CategoriaPrograma.PARTICULAR) {
            throw new CategoriaInvalidaException("Contrato solo aplica a inscripciones de categoria PARTICULAR");
        }

        return contratoRepository.save(Contrato.builder()
                .inscripcion(inscripcion)
                .tipoFacturacion(tipoFacturacion)
                .clasesContratadas(clasesContratadas)
                .build());
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