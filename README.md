# HRIS build and run

Requires JDK 25. The Maven Wrapper downloads pinned Maven on first use; dependency resolution requires network access.

From the repository root in PowerShell:

```powershell
.\mvnw.cmd -B clean verify
.\mvnw.cmd -B -pl hris-app -am clean verify
java -jar .\hris-app\target\hris-app-0.1.0-SNAPSHOT.jar --server.address=127.0.0.1 --server.port=18080
```

Open http://127.0.0.1:18080/ after startup. Stop with Ctrl+C.

On Linux/macOS, replace `.\mvnw.cmd` with `./mvnw` (or `sh ./mvnw` if not executable).

The [CI workflow](.github/workflows/ci.yml) runs the root `clean verify` command on Ubuntu 24.04 and Windows 2025 with JDK 25. Its stable job names are `ci / build-linux` and `ci / build-windows`. It currently covers compilation, packaging, and the existing smoke test. Architecture tests and MySQL integration tests will enter the same reactor when IMP-011 and IMP-004 supply them; they are not currently present.

The normal build packages the Vaadin production frontend. No production profile or database is required. Vaadin can reuse its precompiled bundle for this static route; a custom frontend build requires Node.js 24 or later and npm.
