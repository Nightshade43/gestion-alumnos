package ar.com.ramallo.gestionalumnos.web;

import ar.com.ramallo.gestionalumnos.domain.Persona;
import ar.com.ramallo.gestionalumnos.exception.RecursoNoEncontradoException;
import ar.com.ramallo.gestionalumnos.repository.PersonaRepository;
import ar.com.ramallo.gestionalumnos.web.dto.PersonaRequest;
import ar.com.ramallo.gestionalumnos.web.dto.PersonaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/personas")
@RequiredArgsConstructor
public class PersonaController {

    private final PersonaRepository personaRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PersonaResponse crear(@Valid @RequestBody PersonaRequest request) {
        Persona persona = Persona.builder()
                .nombre(request.nombre()).email(request.email())
                .telefono(request.telefono()).documento(request.documento())
                .build();
        return PersonaResponse.from(personaRepository.save(persona));
    }

    @GetMapping("/{id}")
    public PersonaResponse obtener(@PathVariable Long id) {
        return PersonaResponse.from(buscarOFallar(id));
    }

    @GetMapping
    public List<PersonaResponse> listar() {
        return personaRepository.findAll().stream().map(PersonaResponse::from).toList();
    }

    @PutMapping("/{id}")
    public PersonaResponse actualizar(@PathVariable Long id, @Valid @RequestBody PersonaRequest request) {
        Persona persona = buscarOFallar(id);
        persona.setNombre(request.nombre());
        persona.setEmail(request.email());
        persona.setTelefono(request.telefono());
        persona.setDocumento(request.documento());
        return PersonaResponse.from(personaRepository.save(persona));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        buscarOFallar(id);
        personaRepository.deleteById(id);
    }

    private Persona buscarOFallar(Long id) {
        return personaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Persona no encontrada: " + id));
    }
}