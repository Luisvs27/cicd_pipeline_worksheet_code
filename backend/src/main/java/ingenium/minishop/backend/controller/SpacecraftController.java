package ingenium.minishop.backend.controller;

import ingenium.minishop.backend.model.SpacecraftControlRequest;
import ingenium.minishop.backend.model.SpacecraftStatus;
import ingenium.minishop.backend.model.TelemetryLogEntry;
import ingenium.minishop.backend.service.SpacecraftStateService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class SpacecraftController {
    private final SpacecraftStateService spacecraftStateService;

    public SpacecraftController(SpacecraftStateService spacecraftStateService) {
        this.spacecraftStateService = spacecraftStateService;
    }

    @GetMapping("/api/spacecrafts")
    public List<SpacecraftStatus> getFleetStatus() {
        return spacecraftStateService.getFleetStatus();
    }

    @GetMapping("/api/spacecrafts/{craftId}")
    public SpacecraftStatus getSpacecraftStatus(@PathVariable String craftId) {
        return spacecraftStateService.getCurrentStatus(craftId);
    }

    @GetMapping("/api/spacecrafts/{craftId}/history")
    public List<TelemetryLogEntry> getTelemetryHistory(@PathVariable String craftId) {
        return spacecraftStateService.getTelemetryHistory(craftId);
    }

    @PostMapping("/api/spacecrafts/{craftId}/controls")
    public SpacecraftStatus updateControls(@PathVariable String craftId, @RequestBody SpacecraftControlRequest request) {
        return spacecraftStateService.updateControls(craftId, request);
    }

    @PostMapping("/api/spacecrafts/{craftId}/reset")
    public SpacecraftStatus resetSpacecraft(@PathVariable String craftId) {
        return spacecraftStateService.reset(craftId);
    }
}

