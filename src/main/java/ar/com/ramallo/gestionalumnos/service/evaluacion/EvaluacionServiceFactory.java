package ar.com.ramallo.gestionalumnos.service.evaluacion;

import ar.com.ramallo.gestionalumnos.domain.Programa;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EvaluacionServiceFactory {

    private final CenmaBaseEvaluacionService cenmaBaseEvaluacionService;
    private final CenmaSedeEvaluacionService cenmaSedeEvaluacionService;

    public EstrategiaEvaluacionService resolver(Programa programa) {
        return switch (programa.getEstrategiaEvaluacion()) {
            case CENMA_BASE -> cenmaBaseEvaluacionService;
            case CENMA_SEDE -> cenmaSedeEvaluacionService;
            case SEGUIMIENTO_LIBRE -> throw new IllegalArgumentException(
                    "SEGUIMIENTO_LIBRE no tiene EstrategiaEvaluacionService asociada; "
                            + "el progreso de clientes particulares se maneja via la entidad Seguimiento.");
        };
    }
}