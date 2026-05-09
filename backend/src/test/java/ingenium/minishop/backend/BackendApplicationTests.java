package ingenium.minishop.backend;

import ingenium.minishop.backend.service.SpacecraftStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BackendApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpacecraftStateService spacecraftStateService;

    @BeforeEach
    void resetFleet() {
        spacecraftStateService.restoreFleet();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void getFleetStatusReturnsAllSeededSpacecrafts() throws Exception {
        mockMvc.perform(get("/api/spacecrafts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].craftId").value("iss-pathfinder"))
                .andExpect(jsonPath("$[0].craftName").value("ISS Pathfinder"))
                .andExpect(jsonPath("$[1].craftId").value("europa-clipper"))
                .andExpect(jsonPath("$[2].craftId").value("ares-vanguard"));
    }

    @Test
    void getSingleSpacecraftStatusReturnsTelemetry() throws Exception {
        mockMvc.perform(get("/api/spacecrafts/iss-pathfinder"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.craftId").value("iss-pathfinder"))
                .andExpect(jsonPath("$.craftName").value("ISS Pathfinder"))
                .andExpect(jsonPath("$.missionStage").value("Transfer Burn"))
                .andExpect(jsonPath("$.thrustPercent").value(42))
                .andExpect(jsonPath("$.autopilotEnabled").value(true));
    }

    @Test
    void postControlsUpdatesOnlySelectedSpacecraftAndAddsHistory() throws Exception {
        String requestBody = """
                {
                  "thrustPercent": 65,
                  "pitchDegrees": 8,
                  "yawDegrees": 18,
                  "rollDegrees": -12
                }
                """;

        mockMvc.perform(post("/api/spacecrafts/iss-pathfinder/controls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.craftId").value("iss-pathfinder"))
                                .andExpect(jsonPath("$.thrustPercent").value(65))
                                .andExpect(jsonPath("$.pitchDegrees").value(8))
                .andExpect(jsonPath("$.yawDegrees").value(18))
                                .andExpect(jsonPath("$.rollDegrees").value(-12));

        mockMvc.perform(get("/api/spacecrafts/iss-pathfinder"))
                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.thrustPercent").value(65))
                                .andExpect(jsonPath("$.pitchDegrees").value(8))
                                .andExpect(jsonPath("$.yawDegrees").value(18))
                                .andExpect(jsonPath("$.rollDegrees").value(-12));

        mockMvc.perform(get("/api/spacecrafts/europa-clipper"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thrustPercent").value(18))
                .andExpect(jsonPath("$.missionStage").value("Science Survey"));

        mockMvc.perform(get("/api/spacecrafts/iss-pathfinder/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("CONTROL_UPDATE"))
                .andExpect(jsonPath("$[0].craftId").value("iss-pathfinder"))
                .andExpect(jsonPath("$[0].snapshot.craftId").value("iss-pathfinder"));

        mockMvc.perform(post("/api/spacecrafts/iss-pathfinder/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.missionStage").value("Transfer Burn"))
                .andExpect(jsonPath("$.thrustPercent").value(42));

        mockMvc.perform(get("/api/spacecrafts/iss-pathfinder/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("RESET"))
                .andExpect(jsonPath("$[0].snapshot.craftId").value("iss-pathfinder"));
    }

    @Test
    void speedAndFuelAreSimulatedOverTime() throws Exception {
        mockMvc.perform(post("/api/spacecrafts/iss-pathfinder/controls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"thrustPercent\": 95" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thrustPercent").value(95));

        Thread.sleep(2200);

        mockMvc.perform(get("/api/spacecrafts/iss-pathfinder"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thrustPercent").value(95))
                .andExpect(jsonPath("$.deltaVMs").value(org.hamcrest.Matchers.greaterThan(0.0)))
                .andExpect(jsonPath("$.fuelLevel").value(org.hamcrest.Matchers.lessThanOrEqualTo(79)));
    }

    @Test
    void postControlsRejectsInvalidThrustValues() throws Exception {
        mockMvc.perform(post("/api/spacecrafts/iss-pathfinder/controls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"thrustPercent\": 140" +
                                "}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownSpacecraftReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/spacecrafts/unknown-craft"))
                .andExpect(status().isNotFound());
    }
}
