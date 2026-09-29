# HRIS build and run

Requires JDK 25; Maven Enforcer rejects earlier and later Java versions. The Maven Wrapper downloads pinned Maven on first use; dependency resolution requires network access.

From the repository root in PowerShell:

```powershell
.\mvnw.cmd -B clean verify
.\mvnw.cmd -B -pl hris-app -am clean verify
java -jar .\hris-app\target\hris-app-0.1.0-SNAPSHOT.jar --spring.profiles.active=dev --server.port=18080
```

Open http://127.0.0.1:18080/ after startup. Stop with Ctrl+C.

On Linux/macOS, replace `.\mvnw.cmd` with `./mvnw` (or `sh ./mvnw` if not executable). The `dev` profile binds loopback for local use. Normal configuration leaves the server address open to on-premises LAN configuration.

The [CI workflow](.github/workflows/ci.yml) runs on pull requests, `main` pushes, and manual dispatch. Its policy job checks the frozen planning paths and generated planning index before both builds. New runs for a PR cancel older in-progress runs for that PR; each `main` push runs separately. Ubuntu 24.04 runs `./mvnw -B -Pmysql-it clean verify`, including the real MySQL test. Windows 2025 runs the ordinary `clean verify` reactor, including compilation, packaging, and the HTTP smoke test, without repeating the container test. Both use JDK 25. The stable build job names are `ci / build-linux` and `ci / build-windows`.

## Real MySQL integration test

The `mysql-it` Maven profile runs a focused Spring datasource test against a disposable [MySQL `8.4.11` official image](https://hub.docker.com/_/mysql). It uses [Testcontainers](https://java.testcontainers.org/modules/databases/mysql/) and a Spring Boot service connection. Start a Docker API compatible container runtime before running it; Docker Desktop with Linux containers is suitable on Windows. No local MySQL installation, database preparation, or reusable credentials are needed. A missing runtime fails the activated test instead of skipping it.

From the repository root, run:

```powershell
.\mvnw.cmd -B -Pmysql-it clean verify
```

On Linux/macOS, run `./mvnw -B -Pmysql-it clean verify`. The ordinary `clean verify` builds and runs the HTTP smoke test without starting MySQL. The MySQL test checks the server version, selected disposable database, and a query through Spring's injected `DataSource`; it does not create application tables.

The normal build packages the Vaadin production frontend. No production profile or database is required. Vaadin can reuse its precompiled bundle for this static route; a custom frontend build requires Node.js 24 or later and npm.
