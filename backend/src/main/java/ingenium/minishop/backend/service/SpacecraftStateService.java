package ingenium.minishop.backend.service;

import ingenium.minishop.backend.model.SpacecraftControlRequest;
import ingenium.minishop.backend.model.SpacecraftStatus;
import ingenium.minishop.backend.model.TelemetryLogEntry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class SpacecraftStateService {
    private static final int HISTORY_LIMIT = 12;
    private static final int SIMULATION_TICK_MS = 1000;
    private static final double BASE_FUEL_RECOVERY_PER_TICK = 0.2;
    private static final double BASE_FUEL_BURN_PER_TICK = 0.06;
    private static final double MAX_THRUST_NEWTONS = 4500000.0;

    private final Map<String, SpacecraftStatus> fleet = new LinkedHashMap<>();
    private final Map<String, List<TelemetryLogEntry>> telemetryHistory = new LinkedHashMap<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    public SpacecraftStateService() {
        restoreFleet();
        startSimulation();
    }

    private void startSimulation() {
        executor.scheduleAtFixedRate(this::simulateTick, SIMULATION_TICK_MS, SIMULATION_TICK_MS, TimeUnit.MILLISECONDS);
    }

    private synchronized void simulateTick() {
        for (SpacecraftStatus spacecraft : fleet.values()) {
            simulateSpeedAndFuel(spacecraft);
            applyFlightRules(spacecraft, true);
        }
    }

    public synchronized List<SpacecraftStatus> getFleetStatus() {
        return fleet.values().stream()
                .map(this::copyOf)
                .toList();
    }

    public synchronized SpacecraftStatus getCurrentStatus(String craftId) {
        return copyOf(getRequiredSpacecraft(craftId));
    }

    public synchronized List<TelemetryLogEntry> getTelemetryHistory(String craftId) {
        getRequiredSpacecraft(craftId);

        return telemetryHistory.getOrDefault(craftId, List.of()).stream()
                .map(this::copyOf)
                .sorted(Comparator.comparing(TelemetryLogEntry::getTimestamp).reversed())
                .toList();
    }

    public synchronized SpacecraftStatus updateControls(String craftId, SpacecraftControlRequest request) {
        SpacecraftStatus spacecraft = getRequiredSpacecraft(craftId);
        List<String> updatedFields = new ArrayList<>();

        if (request.getThrustPercent() != null) {
            int thrustPercent = validateRange("thrustPercent", request.getThrustPercent(), 0, 100);
            spacecraft.setThrustPercent(thrustPercent);
            updatedFields.add("thrust");
        }

        if (request.getPowerLevel() != null) {
            spacecraft.setPowerLevel(validateRange("powerLevel", request.getPowerLevel(), 0, 100));
            updatedFields.add("power");
        }

        if (request.getPitchDegrees() != null) {
            spacecraft.setPitchDegrees(validateRange("pitchDegrees", request.getPitchDegrees(), -180, 180));
            updatedFields.add("pitch");
        }

        if (request.getYawDegrees() != null) {
            spacecraft.setYawDegrees(validateRange("yawDegrees", request.getYawDegrees(), -180, 180));
            updatedFields.add("yaw");
        }

        if (request.getRollDegrees() != null) {
            spacecraft.setRollDegrees(validateRange("rollDegrees", request.getRollDegrees(), -180, 180));
            updatedFields.add("roll");
        }

        if (request.getAutopilotEnabled() != null) {
            spacecraft.setAutopilotEnabled(request.getAutopilotEnabled());
            updatedFields.add("autopilot");
        }

        if (request.getDocked() != null) {
            spacecraft.setDocked(request.getDocked());
            updatedFields.add("docking mode");
        }

        if (request.getMissionStage() != null) {
            String missionStage = request.getMissionStage().trim();

            if (missionStage.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missionStage must not be blank");
            }

            spacecraft.setMissionStage(missionStage);
            updatedFields.add("mission stage");
        }

        applyFlightRules(spacecraft, updatedFields.isEmpty());
        spacecraft.setLastCommand(updatedFields.isEmpty()
                ? "Telemetry refresh requested"
                : "Updated " + String.join(", ", updatedFields));

        recordHistory(spacecraft, "CONTROL_UPDATE", spacecraft.getLastCommand());

        return copyOf(spacecraft);
    }

    public synchronized SpacecraftStatus reset(String craftId) {
        SpacecraftStatus initial = createInitialStatusFor(craftId);
        SpacecraftStatus spacecraft = getRequiredSpacecraft(craftId);

        copyValues(initial, spacecraft);
        recordHistory(spacecraft, "RESET", "Mission profile restored to nominal settings");

        return copyOf(spacecraft);
    }

    public synchronized void restoreFleet() {
        fleet.clear();
        telemetryHistory.clear();

        for (SpacecraftStatus spacecraft : List.of(
                createInitialStatusFor("iss-pathfinder"),
                createInitialStatusFor("europa-clipper"),
                createInitialStatusFor("ares-vanguard")
        )) {
            fleet.put(spacecraft.getCraftId(), spacecraft);
            telemetryHistory.put(spacecraft.getCraftId(), new ArrayList<>());
            recordHistory(spacecraft, "BOOTSTRAP", "Initial telemetry profile loaded");
        }
    }

    private void applyFlightRules(SpacecraftStatus spacecraft, boolean noDirectUpdate) {
        if (spacecraft.isDocked()) {
            spacecraft.setThrustPercent(0);
            spacecraft.setMissionStage("Docked");
        } else {
            if (noDirectUpdate || spacecraft.getMissionStage() == null || spacecraft.getMissionStage().isBlank() || "Docked".equalsIgnoreCase(spacecraft.getMissionStage())) {
                spacecraft.setMissionStage(deriveMissionStage(spacecraft.getThrustPercent()));
            }
        }

        int oxygenLevel = 98 - (spacecraft.isDocked() ? 0 : spacecraft.getThrustPercent() / 20);
        spacecraft.setOxygenLevel(Math.max(82, oxygenLevel));

        double reactorTemperature = 210.0 + (spacecraft.getThrustPercent() * 5.1) + (spacecraft.getPowerLevel() * 1.8);
        if (spacecraft.isAutopilotEnabled()) {
            reactorTemperature -= 12.0;
        }
        spacecraft.setReactorTemperatureCelsius(roundToSingleDecimal(reactorTemperature));

        int hullIntegrity = 99 - Math.max(Math.abs(spacecraft.getPitchDegrees()), Math.max(Math.abs(spacecraft.getYawDegrees()), Math.abs(spacecraft.getRollDegrees()))) / 30;
        spacecraft.setHullIntegrity(Math.max(90, hullIntegrity));
    }

    private void simulateSpeedAndFuel(SpacecraftStatus spacecraft) {
        // Calculate deltaV from current thrust and mass
        double currentThrust = (spacecraft.getThrustPercent() / 100.0) * MAX_THRUST_NEWTONS;
        double currentDeltaV = currentThrust / spacecraft.getMass();
        spacecraft.setDeltaVMs(roundToSingleDecimal(currentDeltaV));

        // Handle fuel consumption
        double fuelChange;
        if (spacecraft.isDocked() || spacecraft.getThrustPercent() == 0) {
            fuelChange = BASE_FUEL_RECOVERY_PER_TICK;
        } else {
            double attitudeLoad = (Math.abs(spacecraft.getPitchDegrees()) + Math.abs(spacecraft.getYawDegrees()) + Math.abs(spacecraft.getRollDegrees())) / 600.0;
            fuelChange = -(BASE_FUEL_BURN_PER_TICK + (spacecraft.getThrustPercent() / 120.0) + attitudeLoad);
        }

        int nextFuelLevel = (int) Math.round(spacecraft.getFuelLevel() + fuelChange);
        nextFuelLevel = Math.max(0, Math.min(100, nextFuelLevel));
        spacecraft.setFuelLevel(nextFuelLevel);

        if (nextFuelLevel == 0 && spacecraft.getThrustPercent() > 0) {
            spacecraft.setThrustPercent(0);
            spacecraft.setLastCommand("Fuel depleted - thrust forced to 0%");
        }
    }

    private int validateRange(String fieldName, int value, int min, int max) {
        if (value < min || value > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    fieldName + " must be between " + min + " and " + max);
        }
        return value;
    }

    private String deriveMissionStage(int thrustPercent) {
        if (thrustPercent == 0) {
            return "Orbital Drift";
        }
        if (thrustPercent < 35) {
            return "Station Keeping";
        }
        if (thrustPercent < 70) {
            return "Transfer Burn";
        }
        return "High-Energy Escape Burn";
    }

    private double roundToSingleDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private SpacecraftStatus createInitialStatusFor(String craftId) {
        return switch (craftId) {
            case "iss-pathfinder" -> createStatus(craftId, "ISS Pathfinder", "Transfer Burn", true, false, 42, 88, 4, 12, 450000.0, 0.4, -2, "Nominal flight profile loaded", 17.2, 79, 99, 96, 582.4);
            case "europa-clipper" -> createStatus(craftId, "Europa Clipper II", "Science Survey", true, false, 18, 74, -6, 22, 600000.0, 0.14, 8, "Radiation shield cycle stable", 12.0, 88, 97, 94, 434.7);
            case "ares-vanguard" -> createStatus(craftId, "Ares Vanguard", "Docked", false, true, 0, 63, 0, 0, 500000.0, 0.0, 0, "Docking clamps engaged", 0.0, 96, 100, 98, 323.0);
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown spacecraft: " + craftId);
        };
    }

    private SpacecraftStatus copyOf(SpacecraftStatus source) {
        return new SpacecraftStatus(
                source.getCraftId(),
                source.getCraftName(),
                source.getMissionStage(),
                source.isAutopilotEnabled(),
                source.isDocked(),
                source.getThrustPercent(),
                source.getPowerLevel(),
                source.getFuelLevel(),
                source.getMass(),
                source.getDeltaVMs(),
                source.getHullIntegrity(),
                source.getOxygenLevel(),
                source.getPitchDegrees(),
                source.getYawDegrees(),
                source.getRollDegrees(),
                source.getLastCommand(),
                source.getReactorTemperatureCelsius()
        );
    }

    private TelemetryLogEntry copyOf(TelemetryLogEntry source) {
        return new TelemetryLogEntry(
                source.getTimestamp(),
                source.getCraftId(),
                source.getCraftName(),
                source.getEventType(),
                source.getMessage(),
                copyOf(source.getSnapshot())
        );
    }

    private void copyValues(SpacecraftStatus source, SpacecraftStatus target) {
        target.setCraftId(source.getCraftId());
        target.setCraftName(source.getCraftName());
        target.setMissionStage(source.getMissionStage());
        target.setAutopilotEnabled(source.isAutopilotEnabled());
        target.setDocked(source.isDocked());
        target.setThrustPercent(source.getThrustPercent());
        target.setPowerLevel(source.getPowerLevel());
        target.setFuelLevel(source.getFuelLevel());
        target.setMass(source.getMass());
        target.setDeltaVMs(source.getDeltaVMs());
        target.setHullIntegrity(source.getHullIntegrity());
        target.setOxygenLevel(source.getOxygenLevel());
        target.setPitchDegrees(source.getPitchDegrees());
        target.setYawDegrees(source.getYawDegrees());
        target.setRollDegrees(source.getRollDegrees());
        target.setLastCommand(source.getLastCommand());
        target.setReactorTemperatureCelsius(source.getReactorTemperatureCelsius());
    }

    private SpacecraftStatus createStatus(
            String craftId,
            String craftName,
            String missionStage,
            boolean autopilotEnabled,
            boolean docked,
            int thrustPercent,
            int powerLevel,
            int pitchDegrees,
            int yawDegrees,
            double mass,
            double deltaVMs,
            int rollDegrees,
            String lastCommand,
            double reactorTemperatureCelsius,
            int fuelLevel,
            int hullIntegrity,
            int oxygenLevel,
            double unused
    ) {
        return new SpacecraftStatus(
                craftId,
                craftName,
                missionStage,
                autopilotEnabled,
                docked,
                thrustPercent,
                powerLevel,
                fuelLevel,
                mass,
                deltaVMs,
                hullIntegrity,
                oxygenLevel,
                pitchDegrees,
                yawDegrees,
                rollDegrees,
                lastCommand,
                reactorTemperatureCelsius
        );
    }

    private SpacecraftStatus getRequiredSpacecraft(String craftId) {
        SpacecraftStatus spacecraft = fleet.get(craftId);

        if (spacecraft == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown spacecraft: " + craftId);
        }

        return spacecraft;
    }

    private void recordHistory(SpacecraftStatus spacecraft, String eventType, String message) {
        List<TelemetryLogEntry> log = telemetryHistory.computeIfAbsent(spacecraft.getCraftId(), key -> new ArrayList<>());

        log.add(0, new TelemetryLogEntry(
                Instant.now().toString(),
                spacecraft.getCraftId(),
                spacecraft.getCraftName(),
                eventType,
                message,
                copyOf(spacecraft)
        ));

        if (log.size() > HISTORY_LIMIT) {
            log.remove(log.size() - 1);
        }
    }
}
