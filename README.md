# Spacecraft Mission Control – CI/CD mit GitHub Actions und GHCR

## 1. Ziel der Dokumentation

Diese Dokumentation beschreibt Schritt für Schritt, wie für das Projekt **Spacecraft Mission Control** eine CI/CD-Pipeline mit GitHub Actions aufgebaut wurde.

Die Pipeline erfüllt folgende Ziele:

- Backend automatisch testen und bauen
- Frontend automatisch linten und bauen
- Docker Images für Backend und Frontend erstellen
- Images in die GitHub Container Registry, kurz GHCR, pushen
- Images lokal aus GHCR pullen und testen

---

## 2. Projekt vorbereiten

Zuerst wurde die ZIP-Datei heruntergeladen, entpackt und in ein neues GitHub-Repository hochgeladen.

Danach wurde das Repository lokal geklont:

```powershell
git clone https://github.com/Luisvs27/cicd_pipeline_worksheet_code.git
cd cicd_pipeline_worksheet_code
```

Der verwendete Branch wurde geprüft mit:

```powershell
git branch
```

In diesem Projekt heißt der Branch:

```text
main
```

Deshalb verwenden die GitHub Actions Workflows später `main` als Trigger-Branch.

---

## 3. Backend und Frontend lokal testen

Bevor Docker und GitHub Actions eingerichtet werden, sollte geprüft werden, ob Backend und Frontend lokal funktionieren.

### 3.1 Backend testen

Im Backend-Ordner:

```powershell
cd backend
mvn -B clean package
```

Wenn der Build erfolgreich ist, wird im Ordner `target/` eine `.jar`-Datei erzeugt.

Hinweis: Lokal war JDK 25 installiert. Für Docker und GitHub Actions wird trotzdem Java 17 verwendet, weil das Projekt laut Vorgabe auf Java 17 basiert.

### 3.2 Frontend testen

Im Frontend-Ordner:

```powershell
cd frontend
npm install
npm run lint
npm run build
```

Wenn der Build erfolgreich ist, wird im Ordner `dist/` der Build-Output erzeugt.

---

## 4. Docker Desktop starten

Bevor Docker-Befehle ausgeführt werden, muss Docker Desktop gestartet sein.

Prüfen mit:

```powershell
docker version
```

Wenn Docker korrekt läuft, wird neben `Client` auch ein Abschnitt `Server` angezeigt.

Falls nur der Client angezeigt wird oder eine Pipe-Fehlermeldung erscheint, läuft Docker Desktop noch nicht richtig. Dann Docker Desktop starten und erneut prüfen.

---

## 5. Dockerfile für das Backend erstellen

Im Ordner `backend/` wurde die Datei `Dockerfile` erstellt.

Pfad:

```text
backend/Dockerfile
```

Inhalt:

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -B clean package

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

Das Dockerfile verwendet einen Multi-Stage-Build:

1. In der ersten Stage wird das Maven-Projekt gebaut.
2. In der zweiten Stage wird nur die fertige `.jar`-Datei in ein Java-17-Runtime-Image kopiert.

Backend-Image bauen:

```powershell
docker build -t spacecraft-backend ./backend
```

Backend-Container starten:

```powershell
docker run --rm -p 8080:8080 spacecraft-backend
```

Danach sollte das Backend erreichbar sein unter:

```text
http://localhost:8080/api/spacecrafts
```

Wichtig: Für das Backend ist die Port-Zuordnung `8080:8080`. Port `80` wird nur beim Frontend mit nginx verwendet.

---

## 6. Dockerfile für das Frontend erstellen

Im Ordner `frontend/` wurde die Datei `Dockerfile` erstellt.

Pfad:

```text
frontend/Dockerfile
```

Inhalt:

```dockerfile
FROM node:24.12-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:alpine

COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

Auch hier wird ein Multi-Stage-Build verwendet:

1. Node.js baut die React/Vite-App.
2. nginx liefert danach die fertigen statischen Dateien aus.

Frontend-Image bauen:

```powershell
docker build -t spacecraft-frontend ./frontend
```

Frontend-Container starten:

```powershell
docker run --rm -p 3000:80 spacecraft-frontend
```

Danach ist das Frontend erreichbar unter:

```text
http://localhost:3000
```

Die Port-Zuordnung `3000:80` bedeutet: Der Browser ruft `localhost:3000` auf und Docker leitet die Anfrage an Port `80` im nginx-Container weiter.

---

## 7. Häufiges Docker-Problem: Port ist bereits belegt

Falls beim Starten eines Containers diese Fehlermeldung erscheint:

```text
port is already allocated
```

ist der Port bereits durch einen anderen Prozess oder Container belegt.

Laufende Container anzeigen:

```powershell
docker ps
```

Einen bestimmten Container stoppen:

```powershell
docker stop <CONTAINER_ID>
```

Alle laufenden Container stoppen:

```powershell
docker ps -q | ForEach-Object { docker stop $_ }
```

Alternativ kann ein anderer lokaler Port verwendet werden.

Backend auf anderem Port starten:

```powershell
docker run --rm -p 8081:8080 spacecraft-backend
```

Dann ist das Backend unter folgender URL erreichbar:

```text
http://localhost:8081/api/spacecrafts
```

---

## 8. Verbindung zwischen Frontend und Backend

Im Backend wurde CORS gesetzt:

```java
@CrossOrigin(origins = "http://localhost:3000")
```

Dadurch darf das Frontend auf Port `3000` Anfragen an das Backend senden.

Im Frontend wurde die API-URL angepasst:

```javascript
const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api/spacecrafts";
```

Das ist wichtig, weil das Frontend im Docker-Container über nginx läuft.

Wenn im Browser diese URL aufgerufen wird:

```text
http://localhost:3000/api/spacecrafts
```

geht die Anfrage an nginx im Frontend-Container. Dort existiert aber kein Backend-Endpunkt. Deshalb entsteht ein Fehler wie:

```text
404 Not Found nginx
```

Richtig ist die Backend-URL:

```text
http://localhost:8080/api/spacecrafts
```

---

## 9. Workflow-Verzeichnis erstellen

Im Root-Verzeichnis des Projekts wurde das Workflow-Verzeichnis erstellt:

```powershell
mkdir -p .github/workflows
```

Wichtig: Der richtige Befehl ist `mkdir -p`, nicht `mkdir -d`.

---

## 10. Backend Workflow erstellen

Datei:

```text
.github/workflows/backend.yml
```

Inhalt:

```yaml
name: Backend CI/CD

on:
  push:
    branches:
      - main
    paths:
      - "backend/**"
      - ".github/workflows/backend.yml"

env:
  REGISTRY: ghcr.io

jobs:
  test:
    name: Test and build backend
    runs-on: ubuntu-latest

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up Java 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"
          cache: maven

      - name: Build backend with Maven
        working-directory: backend
        run: mvn -B clean package

  build-and-push:
    name: Build and push backend image
    runs-on: ubuntu-latest
    needs: test

    permissions:
      contents: read
      packages: write

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Log in to GitHub Container Registry
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and push backend Docker image
        uses: docker/build-push-action@v5
        with:
          context: ./backend
          push: true
          tags: |
            ghcr.io/luisvs27/cicd_pipeline_worksheet_code-backend:latest
            ghcr.io/luisvs27/cicd_pipeline_worksheet_code-backend:${{ github.sha }}
```

Der Job `test` baut das Backend mit Maven. Der Job `build-and-push` startet erst danach und pusht das Docker Image in GHCR.

---

## 11. Frontend Workflow erstellen

Datei:

```text
.github/workflows/frontend.yml
```

Inhalt:

```yaml
name: Frontend CI/CD

on:
  push:
    branches:
      - main
    paths:
      - "frontend/**"
      - ".github/workflows/frontend.yml"

env:
  REGISTRY: ghcr.io

jobs:
  lint-and-build:
    name: Lint and build frontend
    runs-on: ubuntu-latest

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up Node.js 24
        uses: actions/setup-node@v4
        with:
          node-version: "24"
          cache: npm
          cache-dependency-path: frontend/package-lock.json

      - name: Install dependencies
        working-directory: frontend
        run: npm ci

      - name: Run linting
        working-directory: frontend
        run: npm run lint

      - name: Build frontend
        working-directory: frontend
        run: npm run build

  build-and-push:
    name: Build and push frontend image
    runs-on: ubuntu-latest
    needs: lint-and-build

    permissions:
      contents: read
      packages: write

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Log in to GitHub Container Registry
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and push frontend Docker image
        uses: docker/build-push-action@v5
        with:
          context: ./frontend
          push: true
          tags: |
            ghcr.io/luisvs27/cicd_pipeline_worksheet_code-frontend:latest
            ghcr.io/luisvs27/cicd_pipeline_worksheet_code-frontend:${{ github.sha }}
```

Der Job `lint-and-build` installiert die Dependencies, führt das Linting aus und baut das Frontend. Danach erstellt `build-and-push` das Docker Image und pusht es in GHCR.

---

## 12. Warum die Image-Tags direkt lowercase geschrieben wurden

Ursprünglich war geplant, den Image-Namen automatisch aus `${{ github.repository }}` zu erzeugen.

Das führte aber zu diesem Fehler:

```text
repository name must be lowercase
```

Der Grund war, dass der GitHub-Benutzername oder Repository-Name Großbuchstaben enthielt, zum Beispiel:

```text
ghcr.io/Luisvs27/CICD_Pipeline_Worksheet_code-backend
```

Docker/GHCR akzeptiert in Image-Namen keine Großbuchstaben.

Deshalb wurden die Image-Tags direkt in lowercase geschrieben:

```text
ghcr.io/luisvs27/cicd_pipeline_worksheet_code-backend
```

und:

```text
ghcr.io/luisvs27/cicd_pipeline_worksheet_code-frontend
```

---

## 13. Änderungen committen und pushen

Alle Änderungen hinzufügen:

```powershell
git add .
```

Commit erstellen:

```powershell
git commit -m "Add CI/CD workflows"
```

Pushen:

```powershell
git push
```

Danach starten die GitHub Actions automatisch.

---

## 14. GitHub Actions prüfen

Auf GitHub:

```text
Repository -> Actions
```

Dort sollten zwei Workflows sichtbar sein:

```text
Backend CI/CD
Frontend CI/CD
```

Wenn ein Workflow fehlschlägt:

1. Fehlgeschlagenen Workflow öffnen.
2. Fehlgeschlagenen Job öffnen.
3. Logs lesen.
4. Die erste wichtige Fehlermeldung suchen.

---

## 15. GitHub Container Registry prüfen

Nach erfolgreichem Workflow werden die Images in GHCR gepusht.

Zu finden unter:

```text
GitHub Profil -> Packages
```

Dort sollten die beiden Packages sichtbar sein:

```text
cicd_pipeline_worksheet_code-backend
cicd_pipeline_worksheet_code-frontend
```

Jedes Image hat mindestens diese Tags:

```text
latest
<commit-sha>
```

`latest` zeigt auf die aktuellste erfolgreiche Version. Der Commit-SHA-Tag zeigt auf eine eindeutig identifizierbare Version.

---

## 16. GHCR Login lokal

Wenn die Images privat sind, muss man sich lokal bei GHCR anmelden.

Befehl:

```powershell
docker login ghcr.io
```

Eingaben:

```text
Username: Luisvs27
Password: Personal Access Token
```

Das Passwort wird im Terminal nicht sichtbar angezeigt. Das ist normal und dient der Sicherheit.

### Personal Access Token erstellen

Auf GitHub:

```text
Profilbild -> Settings -> Developer settings -> Personal access tokens -> Tokens (classic)
```

Dann:

```text
Generate new token (classic)
```

Benötigte Berechtigung:

```text
read:packages
```

Optional zusätzlich:

```text
write:packages
```

Der Token wird nur einmal angezeigt und muss direkt kopiert werden.

---

## 17. Images aus GHCR lokal testen

Backend pullen:

```powershell
docker pull ghcr.io/luisvs27/cicd_pipeline_worksheet_code-backend:latest
```

Backend starten:

```powershell
docker run --rm -p 8080:8080 ghcr.io/luisvs27/cicd_pipeline_worksheet_code-backend:latest
```

Backend prüfen:

```text
http://localhost:8080/api/spacecrafts
```

Frontend pullen:

```powershell
docker pull ghcr.io/luisvs27/cicd_pipeline_worksheet_code-frontend:latest
```

Frontend starten:

```powershell
docker run --rm -p 3000:80 ghcr.io/luisvs27/cicd_pipeline_worksheet_code-frontend:latest
```

Frontend öffnen:

```text
http://localhost:3000
```

---

## 18. Häufige Fehler und Lösungen

| Problem | Ursache | Lösung |
|---|---|---|
| Docker findet Engine nicht | Docker Desktop läuft nicht | Docker Desktop starten |
| Port ist belegt | Alter Container läuft noch | `docker ps` und `docker stop <ID>` |
| Frontend zeigt nginx 404 | API wird auf Port 3000 statt 8080 aufgerufen | API-URL auf `http://localhost:8080/api/spacecrafts` setzen |
| CORS-Fehler | Backend erlaubt Frontend-Port nicht | `@CrossOrigin(origins = "http://localhost:3000")` |
| GitHub Actions startet nicht | Falscher Branch | `main` oder `master` im Workflow prüfen |
| GHCR Push schlägt fehl | Image-Name enthält Großbuchstaben | Tags komplett lowercase schreiben |
| Docker Pull denied | Image privat oder nicht eingeloggt | `docker login ghcr.io` mit PAT |
| npm ci schlägt fehl | Lockfile fehlt oder passt nicht | `package-lock.json` prüfen |
| Maven Build schlägt fehl | Tests oder Dependencies fehlerhaft | Backend lokal mit Maven testen |

---

## 19. Ergebnis

Am Ende wurden folgende Ziele erreicht:

- Backend lokal getestet
- Frontend lokal getestet
- Backend-Dockerfile erstellt
- Frontend-Dockerfile erstellt
- Backend-Image lokal gebaut
- Frontend-Image lokal gebaut
- GitHub Actions Workflows erstellt
- Backend CI/CD erfolgreich eingerichtet
- Frontend CI/CD erfolgreich eingerichtet
- Docker Images nach GHCR gepusht
- Images mit `latest` und Commit-SHA getaggt
- Images lokal aus GHCR gepullt und getestet
- Frontend erreichbar unter `http://localhost:3000`
- Backend erreichbar unter `http://localhost:8080/api/spacecrafts`

---

## 20. Empfehlung für die finale Abgabe

Für eine übersichtliche Dokumentation sollte die Anleitung in zwei Bereiche getrennt werden:

1. **Hauptanleitung**: Der erfolgreiche Ablauf von Setup bis GHCR.
2. **Fehlerbehebung**: Häufige Probleme wie Docker Desktop, belegte Ports, CORS, GHCR Login und lowercase Image-Tags.

Dadurch wirkt die Dokumentation klarer, professioneller und einfacher nachvollziehbar.

