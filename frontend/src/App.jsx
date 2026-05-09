import { useCallback, useEffect, useState } from "react";
import "./App.css";
import aresVanguard from "./assets/ares-vanguard.jpg";
import europaClipper from "./assets/europa-clipper-2.jpg";
import lunarPathfinder from "./assets/lunar-pathfinder.jpg";
import heroImage from "./assets/hero.png";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "/api/spacecrafts";

// Map spacecraft names to their respective images
const getSpacecraftImage = (craftName) => {
  if (!craftName) return heroImage;

  const name = craftName.toLowerCase();
  if (name.includes("ares") || name.includes("vanguard")) return aresVanguard;
  if (name.includes("europa") || name.includes("clipper")) return europaClipper;
  if (name.includes("lunar") || name.includes("pathfinder")) return lunarPathfinder;

  return heroImage; // fallback
};

const defaultForm = {
  thrustPercent: 0,
  pitchDegrees: 0,
  yawDegrees: 0,
  rollDegrees: 0,
};

function createFormFromStatus(status) {
  if (!status) {
    return defaultForm;
  }

  return {
    thrustPercent: status.thrustPercent,
    pitchDegrees: status.pitchDegrees,
    yawDegrees: status.yawDegrees,
    rollDegrees: status.rollDegrees,
  };
}

function App() {
  const [fleet, setFleet] = useState([]);
  const [selectedCraftId, setSelectedCraftId] = useState("");
  const [spacecraft, setSpacecraft] = useState(null);
  const [history, setHistory] = useState([]);
  const [form, setForm] = useState(defaultForm);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("Loading telemetry...");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const fetchJson = useCallback(async (url, options) => {
    const response = await fetch(url, options);

    if (!response.ok) {
      const text = await response.text();
      throw new Error(text || "Backend request failed.");
    }

    return response.json();
  }, []);

  const updateFleetStatus = useCallback((updatedStatus) => {
    setFleet((currentFleet) =>
      currentFleet.map((craft) =>
        craft.craftId === updatedStatus.craftId ? updatedStatus : craft
      )
    );
  }, []);

  const loadTelemetry = useCallback(async (craftId, skipFormUpdate = false) => {
    setError("");

    try {
      const [statusData, historyData] = await Promise.all([
        fetchJson(`${API_BASE_URL}/${craftId}`),
        fetchJson(`${API_BASE_URL}/${craftId}/history`),
      ]);

      setSpacecraft(statusData);
      updateFleetStatus(statusData);
      setHistory(historyData);
      // Only update form if not skipping (i.e., when user explicitly loads or selects a craft)
      if (!skipFormUpdate) {
        setForm(createFormFromStatus(statusData));
        setMessage(`Telemetry synchronized for ${statusData.craftName}.`);
      }
    } catch (err) {
      setError(err.message);
    }
  }, [fetchJson, updateFleetStatus]);

  useEffect(() => {
    let ignore = false;

    fetchJson(API_BASE_URL)
      .then(async (data) => {
        if (ignore) {
          return;
        }

        setFleet(data);

        if (data.length === 0) {
          setSelectedCraftId("");
          setSpacecraft(null);
          setHistory([]);
          setMessage("No spacecraft are available in the fleet.");
          return;
        }

        const nextCraftId = data[0].craftId;
        setSelectedCraftId(nextCraftId);

        const [statusData, historyData] = await Promise.all([
          fetchJson(`${API_BASE_URL}/${nextCraftId}`),
          fetchJson(`${API_BASE_URL}/${nextCraftId}/history`),
        ]);

        if (ignore) {
          return;
        }

        setSpacecraft(statusData);
        updateFleetStatus(statusData);
        setHistory(historyData);
        setForm(createFormFromStatus(statusData));
        setMessage(`Telemetry synchronized for ${statusData.craftName}.`);
      })
      .catch((err) => {
        if (!ignore) {
          setError(err.message);
        }
      });

    return () => {
      ignore = true;
    };
  }, [fetchJson, updateFleetStatus]);

  async function handleCraftSelection(craftId) {
    setSelectedCraftId(craftId);
    await loadTelemetry(craftId);
  }

  function updateFormValue(name, value) {
    setForm((current) => ({
      ...current,
      [name]: value,
    }));
  }

  async function sendControlUpdate(payload, successMessage) {
    if (!selectedCraftId) {
      return;
    }

    setIsSubmitting(true);
    setError("");

    try {
      const response = await fetch(`${API_BASE_URL}/${selectedCraftId}/controls`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });

      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || "Unable to update spacecraft controls.");
      }

      const data = await response.json();
      const updatedHistory = await fetchJson(`${API_BASE_URL}/${selectedCraftId}/history`);
      setSpacecraft(data);
      updateFleetStatus(data);
      setHistory(updatedHistory);
      setForm(createFormFromStatus(data));
      setMessage(successMessage);
    } catch (err) {
      setError(err.message);
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleSubmit(event) {
    event.preventDefault();

    await sendControlUpdate(
      {
        thrustPercent: Number(form.thrustPercent),
        pitchDegrees: Number(form.pitchDegrees),
        yawDegrees: Number(form.yawDegrees),
        rollDegrees: Number(form.rollDegrees),
      },
      "Control update accepted by mission control."
    );
  }

  async function resetSimulation() {
    if (!selectedCraftId) {
      return;
    }

    setIsSubmitting(true);
    setError("");

    try {
      const response = await fetch(`${API_BASE_URL}/${selectedCraftId}/reset`, {
        method: "POST",
      });

      if (!response.ok) {
        throw new Error("Unable to reset spacecraft state.");
      }

      const data = await response.json();
      const updatedHistory = await fetchJson(`${API_BASE_URL}/${selectedCraftId}/history`);
      setSpacecraft(data);
      updateFleetStatus(data);
      setHistory(updatedHistory);
      setForm(createFormFromStatus(data));
      setMessage(`${data.craftName} restored to the nominal mission profile.`);
    } catch (err) {
      setError(err.message);
    } finally {
      setIsSubmitting(false);
    }
  }

  function formatTimestamp(timestamp) {
    return new Date(timestamp).toLocaleString();
  }

  useEffect(() => {
    if (!selectedCraftId) {
      return;
    }

    const interval = setInterval(() => {
      loadTelemetry(selectedCraftId, true); // true = skip form update during polling
    }, 2000);

    return () => clearInterval(interval);
  }, [selectedCraftId, loadTelemetry]);

  if (!spacecraft && !error) {
    return (
      <main className="app-shell">
        <section className="panel hero-panel">
          <p className="eyebrow">Spacecraft mission control</p>
          <h1>Connecting to live telemetry...</h1>
          <p>{message}</p>
        </section>
      </main>
    );
  }

  return (
    <main className="app-shell">
      {/* Spacecraft Selection Header */}
      <section className="panel selection-header">
        <div>
          <p className="eyebrow">Spacecraft mission control</p>
          <h1>{spacecraft?.craftName ?? "Fleet uplink"}</h1>
        </div>
        <div className="header-actions">
          <label className="craft-picker">
            <span>Active spacecraft</span>
            <select
              value={selectedCraftId}
              onChange={(event) => handleCraftSelection(event.target.value)}
              disabled={isSubmitting || fleet.length === 0}
            >
              {fleet.map((craft) => (
                <option key={craft.craftId} value={craft.craftId}>
                  {craft.craftName}
                </option>
              ))}
            </select>
          </label>
          <button type="button" onClick={() => loadTelemetry(selectedCraftId)} disabled={isSubmitting || !selectedCraftId}>
            Refresh telemetry
          </button>
          <button type="button" className="secondary" onClick={resetSimulation} disabled={isSubmitting}>
            Reset profile
          </button>
        </div>
      </section>

      {/* Main 3-Column Layout: Controls | Image | Telemetry */}
      <section className="main-layout">
        {/* Left: Flight Controls */}
        <form className="panel control-panel" onSubmit={handleSubmit}>
          <div className="section-heading">
            <h2>Flight controls</h2>
            <p>Adjust thrust and spacecraft attitude.</p>
          </div>

          <label>
            <span>Thrust output</span>
            <div className="input-row">
              <input
                type="range"
                min="0"
                max="100"
                step="1"
                value={form.thrustPercent}
                onChange={(event) => updateFormValue("thrustPercent", Number(event.target.value))}
              />
              <output>{form.thrustPercent}%</output>
            </div>
          </label>

          <div className="attitude-grid">
            <label>
              <span>Pitch</span>
              <div className="input-row">
                <input
                  type="range"
                  min="-180"
                  max="180"
                  step="1"
                  value={form.pitchDegrees}
                  onChange={(event) => updateFormValue("pitchDegrees", Number(event.target.value))}
                />
                <output>{form.pitchDegrees} deg</output>
              </div>
            </label>

            <label>
              <span>Yaw</span>
              <div className="input-row">
                <input
                  type="range"
                  min="-180"
                  max="180"
                  step="1"
                  value={form.yawDegrees}
                  onChange={(event) => updateFormValue("yawDegrees", Number(event.target.value))}
                />
                <output>{form.yawDegrees} deg</output>
              </div>
            </label>

            <label>
              <span>Roll</span>
              <div className="input-row">
                <input
                  type="range"
                  min="-180"
                  max="180"
                  step="1"
                  value={form.rollDegrees}
                  onChange={(event) => updateFormValue("rollDegrees", Number(event.target.value))}
                />
                <output>{form.rollDegrees} deg</output>
              </div>
            </label>
          </div>

          <div className="button-row">
            <button type="submit" disabled={isSubmitting}>
              Send control update
            </button>
          </div>
        </form>

        {/* Center: Spacecraft Image */}
        <div className="panel spacecraft-visual-container">
          <div className="spacecraft-visual">
            <img src={getSpacecraftImage(spacecraft?.craftName)} alt={`Visualization of ${spacecraft?.craftName ?? "Spacecraft"}`} />
          </div>
        </div>

        {/* Right: Current Telemetry */}
        <aside className="panel telemetry-panel">
          <div className="section-heading">
            <h2>Telemetry snapshot</h2>
            <p>Current values for the selected craft.</p>
          </div>

          <dl>
            <div>
              <dt>Craft ID</dt>
              <dd>{spacecraft.craftId}</dd>
            </div>
            <div>
              <dt>Thrust</dt>
              <dd>{spacecraft.thrustPercent}%</dd>
            </div>
            <div>
              <dt>Power</dt>
              <dd>{spacecraft.powerLevel}%</dd>
            </div>
            <div>
              <dt>Pitch / Yaw / Roll</dt>
              <dd>
                {spacecraft.pitchDegrees}° / {spacecraft.yawDegrees}° / {spacecraft.rollDegrees}°
              </dd>
            </div>
            <div>
              <dt>Docked</dt>
              <dd>{spacecraft.docked ? "Yes" : "No"}</dd>
            </div>
            <div>
              <dt>Autopilot</dt>
              <dd>{spacecraft.autopilotEnabled ? "Enabled" : "Disabled"}</dd>
            </div>
            <div>
              <dt>Status message</dt>
              <dd>{message}</dd>
            </div>
          </dl>

          {error && <p className="feedback error" role="alert">{error}</p>}
          {!error && <p className="feedback success">{message}</p>}
        </aside>
      </section>

      {/* Status Widgets Below */}
      <section className="status-grid" aria-label="Spacecraft telemetry overview">
        <article className="panel stat-card">
          <span>Mission stage</span>
          <strong>{spacecraft.missionStage}</strong>
          <small>Last command: {spacecraft.lastCommand}</small>
        </article>
        <article className="panel stat-card">
          <span>Delta-V</span>
          <strong>{spacecraft.deltaVMs.toFixed(2)} m/s²</strong>
          <small>Autopilot {spacecraft.autopilotEnabled ? "engaged" : "manual"}</small>
        </article>
        <article className="panel stat-card">
          <span>Fuel reserve</span>
          <strong>{spacecraft.fuelLevel}%</strong>
          <small>Oxygen {spacecraft.oxygenLevel}%</small>
        </article>
        <article className="panel stat-card">
          <span>Reactor temperature</span>
          <strong>{spacecraft.reactorTemperatureCelsius.toFixed(1)} °C</strong>
          <small>Hull integrity {spacecraft.hullIntegrity}%</small>
        </article>
      </section>

      {/* History Log */}
      <aside className="panel history-panel">
        <div className="section-heading">
          <h2>Telemetry & history log</h2>
          <p>Recent backend snapshots for {spacecraft.craftName}.</p>
        </div>

        <ul className="history-list">
          {history.map((entry) => (
            <li key={`${entry.timestamp}-${entry.eventType}`} className="history-item">
              <div className="history-item-header">
                <strong>{entry.eventType.replaceAll("_", " ")}</strong>
                <time>{formatTimestamp(entry.timestamp)}</time>
              </div>
              <p>{entry.message}</p>
              <small>
                Stage: {entry.snapshot.missionStage} · Thrust {entry.snapshot.thrustPercent}% · Delta-V {entry.snapshot.deltaVMs.toFixed(2)} m/s²
              </small>
            </li>
          ))}
        </ul>
      </aside>
    </main>
  );
}

export default App;