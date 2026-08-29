package ar.com.ramallo.gestionalumnos.web;

import ar.com.ramallo.gestionalumnos.domain.Empresa;
import ar.com.ramallo.gestionalumnos.exception.RecursoNoEncontradoException;
import ar.com.ramallo.gestionalumnos.repository.EmpresaRepository;
import ar.com.ramallo.gestionalumnos.web.dto.EmpresaRequest;
import ar.com.ramallo.gestionalumnos.web.dto.EmpresaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/empresas")
@RequiredArgsConstructor
public class EmpresaController {

    private final EmpresaRepository empresaRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmpresaResponse crear(@Valid @RequestBody EmpresaRequest request) {
        Empresa empresa = Empresa.builder()
                .nombre(request.nombre())
                .contacto(request.contacto())
                .build();
        return EmpresaResponse.from(empresaRepository.save(empresa));
    }

    @GetMapping("/{id}")
    public EmpresaResponse obtener(@PathVariable Long id) {
        return EmpresaResponse.from(buscarOFallar(id));
    }

    @GetMapping
    public List<EmpresaResponse> listar() {
        return empresaRepository.findAll().stream().map(EmpresaResponse::from).toList();
    }

    private Empresa buscarOFallar(Long id) {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Empresa no encontrada: " + id));
    }
}