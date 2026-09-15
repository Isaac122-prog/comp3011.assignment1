package comp3011.assignment1;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminController.class)
class AdminControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatsService statsService;

    @MockitoBean
    private ConfigurableApplicationContext applicationContext;

    @Test
    void uptimeReturnsExpectedFields() throws Exception {
        mockMvc.perform(get("/api/v1/admin/uptime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utcServerStart").exists())
                .andExpect(jsonPath("$.utcNow").exists())
                .andExpect(jsonPath("$.serverUptimeSeconds").exists());
    }

    @Test
    void globalStatsReflectsStatsService() throws Exception {
        given(statsService.getInputTokens()).willReturn(1234L);
        given(statsService.getOutputTokens()).willReturn(567L);

        mockMvc.perform(get("/api/v1/global/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inputTokens").value(1234))
                .andExpect(jsonPath("$.outputTokens").value(567));
    }

    @Test
    void secondShutdownRequestIsRejectedWithConflict() throws Exception {
        mockMvc.perform(post("/api/v1/admin/shutdown"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("Graceful shutdown requested."));

        mockMvc.perform(post("/api/v1/admin/shutdown"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Graceful shutdown is already in progress."));
    }
}