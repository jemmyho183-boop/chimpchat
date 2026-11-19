# chimpchatapi

A Java 17-compatible Spring Boot backend API built with Maven.

## Requirements

- JDK 17 or newer
- Internet access on the first run so the Maven wrapper can download Maven and dependencies

## Run

From the project directory on Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

The API listens on the port configured by `API_PORT` (default `8080`).

## Health endpoint

```http
GET /api/health
```

Returns:

```json
{"status":"UP","service":"chimpchatapi"}
```

Spring Boot Actuator health is also available at `/actuator/health`.

## Test

```powershell
.\mvnw.cmd test
```

## Automation runtime

Instagram comment automations are stored as versioned definitions containing a trigger and ordered steps. The runtime resolves trigger and step handlers by type, records each step outcome, and snapshots the definition version used by each execution. Existing comment automations without a saved definition are translated from their legacy columns when processed.

The current API and editor still create Instagram comment flows only. A new trigger or action must register and validate its handler, add the event/configuration contract, and be exposed by the API and editor before users can configure it.
