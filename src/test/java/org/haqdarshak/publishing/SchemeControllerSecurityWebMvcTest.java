package org.haqdarshak.publishing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.haqdarshak.publishing.config.JwtTokenProvider;
import org.haqdarshak.publishing.domain.enums.UserRole;
import org.haqdarshak.publishing.dto.request.CreateSchemeRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SchemeControllerSecurityWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Unauthenticated request to secured endpoint returns 401/403")
    void unauthenticatedRequestFails() throws Exception {
        mockMvc.perform(get("/api/schemes"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FIELD_AGENT cannot create schemes (requires STATE_REVIEWER)")
    void fieldAgentCannotCreateScheme() throws Exception {
        String token = jwtTokenProvider.createToken("agent_1", UserRole.FIELD_AGENT);

        CreateSchemeRequest request = new CreateSchemeRequest("SCH_SEC_TEST", "Test Scheme", "Health", "Delhi");

        mockMvc.perform(post("/api/schemes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("STATE_REVIEWER can create schemes")
    void stateReviewerCanCreateScheme() throws Exception {
        String token = jwtTokenProvider.createToken("state_reviewer_1", UserRole.STATE_REVIEWER);

        CreateSchemeRequest request = new CreateSchemeRequest("SCH_SEC_SUCCESS", "Health Scheme", "Health", "Delhi");

        mockMvc.perform(post("/api/schemes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
