package ingenium.minishop.backend.model;

public class SpacecraftStatus {
    private String craftId;
    private String craftName;
    private String missionStage;
    private boolean autopilotEnabled;
    private boolean docked;
    private int thrustPercent;
    private int powerLevel;
    private int fuelLevel;
    private double mass;
    private double deltaVMs;
    private int hullIntegrity;
    private int oxygenLevel;
    private int pitchDegrees;
    private int yawDegrees;
    private int rollDegrees;
    private String lastCommand;
    private double reactorTemperatureCelsius;

    public SpacecraftStatus() {
    }

    public SpacecraftStatus(
            String craftId,
            String craftName,
            String missionStage,
            boolean autopilotEnabled,
            boolean docked,
            int thrustPercent,
            int powerLevel,
            int fuelLevel,
            double mass,
            double deltaVMs,
            int hullIntegrity,
            int oxygenLevel,
            int pitchDegrees,
            int yawDegrees,
            int rollDegrees,
            String lastCommand,
            double reactorTemperatureCelsius
    ) {
        this.craftId = craftId;
        this.craftName = craftName;
        this.missionStage = missionStage;
        this.autopilotEnabled = autopilotEnabled;
        this.docked = docked;
        this.thrustPercent = thrustPercent;
        this.powerLevel = powerLevel;
        this.fuelLevel = fuelLevel;
        this.mass = mass;
        this.deltaVMs = deltaVMs;
        this.hullIntegrity = hullIntegrity;
        this.oxygenLevel = oxygenLevel;
        this.pitchDegrees = pitchDegrees;
        this.yawDegrees = yawDegrees;
        this.rollDegrees = rollDegrees;
        this.lastCommand = lastCommand;
        this.reactorTemperatureCelsius = reactorTemperatureCelsius;
    }

    public String getCraftId() {
        return craftId;
    }

    public void setCraftId(String craftId) {
        this.craftId = craftId;
    }

    public String getCraftName() {
        return craftName;
    }

    public void setCraftName(String craftName) {
        this.craftName = craftName;
    }

    public String getMissionStage() {
        return missionStage;
    }

    public void setMissionStage(String missionStage) {
        this.missionStage = missionStage;
    }

    public boolean isAutopilotEnabled() {
        return autopilotEnabled;
    }

    public void setAutopilotEnabled(boolean autopilotEnabled) {
        this.autopilotEnabled = autopilotEnabled;
    }

    public boolean isDocked() {
        return docked;
    }

    public void setDocked(boolean docked) {
        this.docked = docked;
    }

    public int getThrustPercent() {
        return thrustPercent;
    }

    public void setThrustPercent(int thrustPercent) {
        this.thrustPercent = thrustPercent;
    }

    public int getPowerLevel() {
        return powerLevel;
    }

    public void setPowerLevel(int powerLevel) {
        this.powerLevel = powerLevel;
    }

    public int getFuelLevel() {
        return fuelLevel;
    }

    public void setFuelLevel(int fuelLevel) {
        this.fuelLevel = fuelLevel;
    }

    public double getMass() {
        return mass;
    }

    public void setMass(double mass) {
        this.mass = mass;
    }

    public double getDeltaVMs() {
        return deltaVMs;
    }

    public void setDeltaVMs(double deltaVMs) {
        this.deltaVMs = deltaVMs;
    }

    public int getHullIntegrity() {
        return hullIntegrity;
    }

    public void setHullIntegrity(int hullIntegrity) {
        this.hullIntegrity = hullIntegrity;
    }

    public int getOxygenLevel() {
        return oxygenLevel;
    }

    public void setOxygenLevel(int oxygenLevel) {
        this.oxygenLevel = oxygenLevel;
    }

    public int getPitchDegrees() {
        return pitchDegrees;
    }

    public void setPitchDegrees(int pitchDegrees) {
        this.pitchDegrees = pitchDegrees;
    }

    public int getYawDegrees() {
        return yawDegrees;
    }

    public void setYawDegrees(int yawDegrees) {
        this.yawDegrees = yawDegrees;
    }

    public int getRollDegrees() {
        return rollDegrees;
    }

    public void setRollDegrees(int rollDegrees) {
        this.rollDegrees = rollDegrees;
    }

    public String getLastCommand() {
        return lastCommand;
    }

    public void setLastCommand(String lastCommand) {
        this.lastCommand = lastCommand;
    }

    public double getReactorTemperatureCelsius() {
        return reactorTemperatureCelsius;
    }

    public void setReactorTemperatureCelsius(double reactorTemperatureCelsius) {
        this.reactorTemperatureCelsius = reactorTemperatureCelsius;
    }
}
