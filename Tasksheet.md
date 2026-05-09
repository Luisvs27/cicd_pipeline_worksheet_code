# Spacecraft Mission Control - DevOps Praktikum

## Übersicht

In diesem Praktikum baut ihr eine vollständige CI/CD-Pipeline mit GitHub Actions auf. Die Pipeline soll:

1. **Continuous Integration (CI)**: Bei jedem Push auf den `master`-Branch automatisch Tests ausführen und das Projekt bauen
2. **Continuous Delivery (CD)**: Docker-Images für Backend und Frontend erstellen und in die GitHub Container Registry (GHCR) pushen
3. **Artifact Management**: Docker-Images versionieren und in der GitHub Container Registry speichern

> **Hinweis zum Branch-Namen:** Dieses Tasksheet verwendet `master`. Falls euer Repository stattdessen `main` als Default-Branch verwendet, ersetzt in den Workflow-Dateien `master` durch `main`.

---

## Voraussetzungen

- Das Repository muss auf GitHub öffentlich oder privat vorhanden sein
- GitHub Actions müssen aktiviert sein, was normalerweise standardmäßig der Fall ist
- Ihr müsst Collaborator auf dem Repository sein oder es selbst erstellt haben
- Docker muss lokal installiert sein, wenn ihr Images lokal testen wollt
- Node.js und Maven sollten lokal installiert sein, wenn ihr Frontend und Backend lokal testen wollt
- Das Backend verwendet Java 17
- Das Frontend verwendet Node.js, passend zur Projektvorgabe, z. B. Node.js 18 oder 20

---

## Phase 1: Repository-Setup

### Schritt 1.1: GitHub Actions und GHCR-Zugriff verstehen

Für das Pushen von Docker-Images aus GitHub Actions in die GitHub Container Registry benötigt ihr **keinen eigenen Personal Access Token**.

GitHub stellt in jedem Workflow automatisch den Token `${{ secrets.GITHUB_TOKEN }}` zur Verfügung. Um damit Images zu pushen, müssen im Workflow passende Berechtigungen gesetzt werden. Recherchiert, welche `permissions` ihr in euren Workflow-Dateien setzen müsst.

> **Wichtig:** Erstellt für dieses Praktikum keinen eigenen PAT für GitHub Actions. Das reduziert Sicherheitsrisiken und vermeidet unnötige Fehlerquellen.

### Schritt 1.2: Repository klonen

Falls noch nicht geschehen, klont das Repository:

```bash
git clone https://github.com/<username>/minishop-devops-teaching.git
cd minishop-devops-teaching
```

Ersetzt `<username>` durch euren GitHub-Benutzernamen oder den Namen der Organisation.

---

## Phase 2: Lokales Testen

Bevor ihr die GitHub Actions erstellt, könnt ihr die einzelnen Komponenten lokal testen. Dieser Schritt ist empfohlen, aber nicht zwingend erforderlich.

### Schritt 2.1: Backend lokal testen

Navigiert in das Backend-Verzeichnis und:

1. Führt die Tests aus
2. Buildet das Projekt

Prüft, ob im `target/`-Verzeichnis eine JAR-Datei erzeugt wurde.

### Schritt 2.2: Frontend lokal testen

Navigiert in das Frontend-Verzeichnis und:

1. Installiert die Dependencies reproduzierbar
2. Führt das Linting aus
3. Buildet das Frontend

Prüft, ob im `dist/`-Verzeichnis der Build-Output erzeugt wurde.

---

## Phase 3: Dockerfiles erstellen

Für Backend und Frontend benötigt ihr jeweils ein Dockerfile. Diese Dockerfiles werden **nicht im Workflow erzeugt**, sondern als normale Dateien im Repository gespeichert.

### Schritt 3.1: Backend-Dockerfile erstellen

Erstellt im Verzeichnis `backend/` eine Datei namens `Dockerfile`.

**Anforderungen:**

- Multi-stage build mit Maven
- Erste Stage: kompiliert das Java-Projekt mit Maven
- Zweite Stage: verwendet eine Java-17-Runtime
- Die erzeugte JAR-Datei wird aus der Build-Stage in die Runtime-Stage kopiert
- Port `8080` wird mit `EXPOSE` dokumentiert
- `ENTRYPOINT` startet die JAR-Datei

> **Hinweis:** Orientiert euch an Best Practices für Multi-stage Docker-Builds. Falls euer Backend einen anderen JAR-Namen oder zusätzliche Dateien benötigt, müsst ihr das Dockerfile entsprechend anpassen.

### Schritt 3.2: Frontend-Dockerfile erstellen

Erstellt im Verzeichnis `frontend/` eine Datei namens `Dockerfile`.

**Anforderungen:**

- Multi-stage build mit Node.js
- Erste Stage: installiert Dependencies und buildet die React/Vite-App
- Zweite Stage: verwendet nginx als Webserver
- Der Build-Output wird nach `/usr/share/nginx/html` kopiert
- Port `80` wird mit `EXPOSE` dokumentiert
- `CMD` startet nginx

---

## Phase 4: CI/CD-Pipeline mit GitHub Actions

### Schritt 4.1: Workflow-Verzeichnis erstellen

Erstellt im Root des Repositories das Verzeichnis `.github/workflows`, falls es noch nicht existiert:

```bash
mkdir -p .github/workflows
```

In diesem Verzeichnis werden die YAML-Dateien für GitHub Actions gespeichert.

---

## Schritt 4.2: Backend-Workflow erstellen

Erstellt die Datei `.github/workflows/backend.yml`.

Der Workflow soll folgende **Trigger** haben:

- Wird ausgelöst bei einem Push auf den `master`-Branch
- Wird nur ausgelöst, wenn sich Dateien im `backend/`-Verzeichnis oder der Workflow-Datei selbst ändern

Der Workflow soll folgende **Jobs** enthalten:

**Job 1: test**
- Runner: frei wählbar (an Java/Maven-Toolchain anpassen)
- Checked den Code aus
- Setzt Java 17 auf (Temurin Distribution) mit Maven-Caching
- Führt in der `backend/`-Working-Directory `mvn -B clean package` aus

**Job 2: build-and-push**
- Runner: frei wählbar (muss Docker-Build und Registry-Push unterstützen)
- Ist abhängig vom Job `test` (startet nur bei Erfolg)
- Hat Berechtigungen für `contents: read` und `packages: write`
- Checked den Code aus
- Loggt sich in GHCR ein mit:
  - Registry: `ghcr.io`
  - Username: GitHub Actor
  - Password: `${{ secrets.GITHUB_TOKEN }}`
- Buildet und pusht das Docker-Image mit:
  - Build-Kontext: `./backend`
  - Tags: `latest` und Commit SHA (als mehrere Tags)
  - Push: aktiviert

**Environment-Variable:**
- `IMAGE_NAME`: `${{ github.repository }}-backend`
- `REGISTRY`: `ghcr.io`

---

## Schritt 4.3: Frontend-Workflow erstellen

Erstellt die Datei `.github/workflows/frontend.yml`.

Der Workflow soll folgende **Trigger** haben:

- Wird ausgelöst bei einem Push auf den `master`-Branch
- Wird nur ausgelöst, wenn sich Dateien im `frontend/`-Verzeichnis oder der Workflow-Datei selbst ändern

Der Workflow soll folgende **Jobs** enthalten:

**Job 1: lint-and-build**
- Runner: frei wählbar (an Node/npm-Toolchain anpassen)
- Checked den Code aus
- Setzt Node.js 18 auf mit npm-Caching (Lockfile-Pfad: `frontend/package-lock.json`)
- In der `frontend/`-Working-Directory:
  - `npm ci` ausführen
  - `npm run lint` ausführen
  - `npm run build` ausführen

**Job 2: build-and-push**
- Runner: frei wählbar (muss Docker-Build und Registry-Push unterstützen)
- Ist abhängig vom Job `lint-and-build` (startet nur bei Erfolg)
- Hat Berechtigungen für `contents: read` und `packages: write`
- Checked den Code aus
- Loggt sich in GHCR ein mit:
  - Registry: `ghcr.io`
  - Username: GitHub Actor
  - Password: `${{ secrets.GITHUB_TOKEN }}`
- Buildet und pusht das Docker-Image mit:
  - Build-Kontext: `./frontend`
  - Tags: `latest` und Commit SHA (als mehrere Tags)
  - Push: aktiviert

**Environment-Variable:**
- `IMAGE_NAME`: `${{ github.repository }}-frontend`
- `REGISTRY`: `ghcr.io`

---

## Phase 5: Docker Image Tags

Für beide Services werden zwei Tags verwendet:

### Backend

```text
ghcr.io/<owner>/<repository>-backend:latest
ghcr.io/<owner>/<repository>-backend:<commit-sha>
```

### Frontend

```text
ghcr.io/<owner>/<repository>-frontend:latest
ghcr.io/<owner>/<repository>-frontend:<commit-sha>
```

Der Tag `latest` zeigt auf die aktuellste erfolgreich gebaute Version.

Der Commit-SHA-Tag zeigt auf eine eindeutig identifizierbare Version des Images.

---

## Phase 6: Workflow ausführen und validieren

### Schritt 6.1: Änderungen committen und pushen

Committed alle erstellten Dateien (`.github/workflows/`, Dockerfiles, nginx.conf) und pusht sie auf euren Branch.

Falls euer Branch `main` heißt statt `master`, passt den Branch-Namen entsprechend an.

### Schritt 6.2: GitHub Actions überwachen

1. Navigiert zu eurem Repository auf GitHub
2. Klickt auf **Actions**
3. Sucht den Workflow-Run zu eurem letzten Commit
4. Öffnet den Run, um Details zu sehen
5. Überprüft die einzelnen Jobs und Schritte

### Schritt 6.3: Fehlerbehandlung

Falls ein Workflow fehlschlägt:

1. Klickt auf den fehlgeschlagenen Run
2. Klickt auf den fehlgeschlagenen Job
3. Lest die Logs sorgfältig durch
4. Sucht nach der ersten aussagekräftigen Fehlermeldung

Häufige Fehler:

| Fehler | Mögliche Ursache | Lösung |
|---|---|---|
| Workflow startet nicht | Branch heißt `main` statt `master` | Branch-Namen im Workflow anpassen |
| Workflow startet nicht | Nur Workflow-Datei geändert, aber nicht im `paths`-Filter enthalten | Workflow-Datei im `paths`-Filter aufnehmen |
| Docker Login fehlgeschlagen | `permissions` fehlen oder sind falsch gesetzt | `packages: write` ergänzen |
| Maven Build fehlgeschlagen | Tests schlagen fehl oder Dependencies fehlen | Maven-Logs lesen und lokal testen |
| npm ci fehlgeschlagen | `package-lock.json` fehlt oder passt nicht zur `package.json` | Lockfile prüfen oder neu erzeugen |
| npm run lint fehlgeschlagen | Linting-Fehler im Frontend | Fehler laut Log beheben |
| Dockerfile nicht gefunden | Falscher Build-Kontext | `context: ./backend` oder `context: ./frontend` prüfen |
| Image wird nicht gepusht | `push: true` fehlt | `push: true` ergänzen |

---

## Phase 7: Images in der GitHub Container Registry ansehen

### Schritt 7.1: Packages öffnen

1. Navigiert zu eurem GitHub-Profil oder zur Organisation
2. Klickt auf **Packages**
3. Dort seht ihr die gepushten Docker-Images
4. Öffnet ein Image, um Tags, Versionen und Metadaten zu sehen

Je nach Repository- und Package-Einstellungen kann es nötig sein, die Sichtbarkeit oder Verknüpfung des Packages zu kontrollieren.

### Schritt 7.2: Image lokal testen

Um ein Image lokal zu testen, benötigt ihr für private Images einen GitHub Token mit Leserechten für Packages. Für öffentliche Images ist häufig kein Login nötig.

**Erste Schritte:**

1. Authentifiziert euch bei GHCR (falls nötig)
2. Pullt das Backend-Image aus GHCR
3. Startet einen Container mit dem Backend-Image und mapped Port 8080
4. Pullt das Frontend-Image aus GHCR
5. Startet einen Container mit dem Frontend-Image und mapped das interne Port 80 auf localhost Port 8080
6. Öffnet das Frontend im Browser

Das Frontend sollte unter `http://localhost:8080` erreichbar sein.

---

## Phase 8: Optionale Erweiterungen

### Aufgabe 8.1: Pull Request Checks

Erstellt zusätzliche Workflows oder erweitert die bestehenden Workflows so, dass bei jedem Pull Request automatisch geprüft wird:

- Backend-Tests laufen erfolgreich
- Frontend-Linting läuft erfolgreich
- Frontend-Build läuft erfolgreich

Dafür benötigt ihr einen neuen Workflow-Trigger. Recherchiert, welcher Trigger für Pull Requests zuständig ist. Passt euren Trigger-Branch an (z.B. `master` oder `main`).

### Aufgabe 8.2: Scheduled Builds

Erstellt einen zusätzlichen Workflow, der regelmäßig (z. B. täglich) automatisch ausgeführt wird.

Mögliche Aufgaben:

- Projekt regelmäßig bauen
- Security Checks ausführen
- Dependency-Updates prüfen
- Vulnerability Scan durchführen

Dafür benötigt ihr einen Workflow-Trigger für zeitbasierte Ereignisse. Recherchiert, wie man mit GitHub Actions zeitgesteuerte Workflows auslöst.

### Aufgabe 8.3: Deployment-Job hinzufügen

Erweitert die Workflows um einen zusätzlichen Job, der nach erfolgreichem Push:

- ein Deployment-Skript ausführt
- eine Benachrichtigung sendet
- Logs oder Deployment-Artefakte speichert

---

## Checkliste zum Abschluss

- [ ] Repository geklont
- [ ] Prüfen, ob der Default-Branch `master` oder `main` heißt
- [ ] `.github/workflows/` Verzeichnis erstellt
- [ ] `backend/Dockerfile` erstellt
- [ ] `frontend/Dockerfile` erstellt
- [ ] `frontend/nginx.conf` erstellt (optional, falls eigene nginx-Konfiguration genutzt wird)
- [ ] `.github/workflows/backend.yml` erstellt
- [ ] `.github/workflows/frontend.yml` erstellt
- [ ] Workflows verwenden `${{ secrets.GITHUB_TOKEN }}`
- [ ] Workflows enthalten `permissions: contents: read` und `packages: write`
- [ ] Backend-Workflow erfolgreich ausgeführt
- [ ] Frontend-Workflow erfolgreich ausgeführt
- [ ] Backend-Image in GHCR sichtbar
- [ ] Frontend-Image in GHCR sichtbar
- [ ] Images mit `latest` und Commit SHA getaggt
- [ ] Images optional lokal getestet

---

## Ressourcen

- GitHub Actions Dokumentation: https://docs.github.com/en/actions
- GitHub Workflow Syntax: https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions
- GitHub Container Registry: https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry
- Authentifizierung in Workflows: https://docs.github.com/en/actions/security-guides/automatic-token-authentication
- Docker Publish mit GitHub Actions: https://docs.github.com/en/actions/publishing-packages/publishing-docker-images
- docker/login-action (v3): https://github.com/docker/login-action
- docker/build-push-action (v5): https://github.com/docker/build-push-action
- actions/setup-java (v4): https://github.com/actions/setup-java
- actions/setup-node (v4): https://github.com/actions/setup-node
- actions/checkout (v4): https://github.com/actions/checkout

---

## Fragen und Hilfe

Falls Fragen oder Fehler auftauchen:

1. Schaut zuerst in die **GitHub Actions Logs**
2. Prüft, ob der richtige Branch im Workflow verwendet wird
3. Prüft die **Dateipfade** und den Docker-Build-Kontext
4. Testet Backend, Frontend und Dockerfiles lokal
5. Achtet darauf, dass für GHCR in GitHub Actions kein eigener PAT nötig ist

Viel Erfolg beim Praktikum! 🚀
