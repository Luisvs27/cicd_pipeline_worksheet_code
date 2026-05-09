package ingenium.minishop.backend.model;

public class SpacecraftControlRequest {
    private Integer thrustPercent;
    private Integer powerLevel;
    private Integer pitchDegrees;
    private Integer yawDegrees;
    private Integer rollDegrees;
    private Boolean autopilotEnabled;
    private Boolean docked;
    private String missionStage;

    public Integer getThrustPercent() {
        return thrustPercent;
    }

    public void setThrustPercent(Integer thrustPercent) {
        this.thrustPercent = thrustPercent;
    }

    public Integer getPowerLevel() {
        return powerLevel;
    }

    public void setPowerLevel(Integer powerLevel) {
        this.powerLevel = powerLevel;
    }

    public Integer getPitchDegrees() {
        return pitchDegrees;
    }

    public void setPitchDegrees(Integer pitchDegrees) {
        this.pitchDegrees = pitchDegrees;
    }

    public Integer getYawDegrees() {
        return yawDegrees;
    }

    public void setYawDegrees(Integer yawDegrees) {
        this.yawDegrees = yawDegrees;
    }

    public Integer getRollDegrees() {
        return rollDegrees;
    }

    public void setRollDegrees(Integer rollDegrees) {
        this.rollDegrees = rollDegrees;
    }

    public Boolean getAutopilotEnabled() {
        return autopilotEnabled;
    }

    public void setAutopilotEnabled(Boolean autopilotEnabled) {
        this.autopilotEnabled = autopilotEnabled;
    }

    public Boolean getDocked() {
        return docked;
    }

    public void setDocked(Boolean docked) {
        this.docked = docked;
    }

    public String getMissionStage() {
        return missionStage;
    }

    public void setMissionStage(String missionStage) {
        this.missionStage = missionStage;
    }
}
