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

The API starts at `http://localhost:8080`.

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
