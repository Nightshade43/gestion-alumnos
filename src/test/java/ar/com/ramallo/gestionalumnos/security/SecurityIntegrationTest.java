package ar.com.ramallo.gestionalumnos.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Value("${admin.username}") private String adminUsername;
    @Value("${admin.password}") private String adminPassword;

    @Test
    void rechazaAccesoSinTokenAUnEndpointProtegido() throws Exception {
        mockMvc.perform(get("/api/personas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rechazaLoginConCredencialesIncorrectas() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", adminUsername, "password", "contrasena_incorrecta_a_proposito"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void permiteAccesoConElTokenObtenidoPorLoginReal() throws Exception {
        String loginBody = objectMapper.writeValueAsString(Map.of(
                "username", adminUsername, "password", adminPassword));

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(response).get("token").asText();

        mockMvc.perform(get("/api/personas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}