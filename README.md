# Spacecraft Mission Control

This project now contains a full-stack spacecraft mission-control simulator:

- `backend/`: Spring Boot API that stores an in-memory fleet of spacecrafts plus a per-craft telemetry/history log
- `frontend/`: React + Vite mission control dashboard that can switch between spacecrafts, read telemetry with `GET`, and update controls with `POST`

## Backend API

- `GET /api/spacecrafts` – return the current fleet overview
- `GET /api/spacecrafts/{craftId}` – return the current spacecraft status for one craft
- `GET /api/spacecrafts/{craftId}/history` – return the recent telemetry/history log for one craft
- `POST /api/spacecrafts/{craftId}/controls` – update thrust, power, orientation, autopilot, docking state, or mission stage
- `POST /api/spacecrafts/{craftId}/reset` – restore the selected craft to its initial mission profile

Example request body for `POST /api/spacecrafts/{craftId}/controls`:

```json
{
  "thrustPercent": 55,
  "powerLevel": 90,
  "pitchDegrees": 5,
  "yawDegrees": 12,
  "rollDegrees": -3,
  "autopilotEnabled": true,
  "docked": false,
  "missionStage": "Lunar Transfer"
}
```

Seeded spacecraft ids:

- `iss-pathfinder`
- `europa-clipper`
- `ares-vanguard`

## Run locally

Backend:

```powershell
Set-Location "C:\Users\gerno\IdeaProjects\2025-26\minishop-devops-teaching\backend"
.\mvnw.cmd spring-boot:run
```

Frontend:

```powershell
Set-Location "C:\Users\gerno\IdeaProjects\2025-26\minishop-devops-teaching\frontend"
npm install
npm run dev
```

The Vite dev server proxies `/api` requests to `http://localhost:8080`.

## Validate

Backend tests:

```powershell
Set-Location "C:\Users\gerno\IdeaProjects\2025-26\minishop-devops-teaching\backend"
.\mvnw.cmd test
```

Frontend checks:

```powershell
Set-Location "C:\Users\gerno\IdeaProjects\2025-26\minishop-devops-teaching\frontend"
npm run lint
npm run build
```

