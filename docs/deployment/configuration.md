# Runtime configuration and secrets

TASK-0010 defines the configuration convention for the single Spring Boot application. It does not install or configure a production service. The installer and service wiring will use these locations in later work.

| Layer | Source | Rule |
| --- | --- | --- |
| Repository defaults | Packaged `hris-app/src/main/resources/application.properties` | Safe technical defaults only; no installation values or secrets. |
| Development | Packaged `application-dev.properties` | Activated explicitly with `spring.profiles.active=dev`; currently binds the local server to loopback. Do not enable it for normal LAN service use. |
| Tests | Test code and disposable test resources | Synthetic, isolated values; never depend on an installed HRIS configuration directory. |
| Installation configuration | External `application.properties` | Machine-specific, non-secret technical settings; kept outside the application binary and source control. |
| Protected secrets | External `secrets.properties` | Credentials and private values supplied outside source control, stored with stronger access restrictions than ordinary configuration. |

Spring Boot 4.1.1 loads [external configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html) and [profile-specific files](https://docs.spring.io/spring-boot/reference/features/profiles.html) without an HRIS-specific loader. The service must set both of these early configuration properties, using OS environment variables or equivalent service launch settings:

```text
SPRING_CONFIG_LOCATION=classpath:/
SPRING_CONFIG_ADDITIONAL_LOCATION=<absolute-file-URI-to-application.properties>,<absolute-file-URI-to-secrets.properties>
```

`SPRING_CONFIG_LOCATION=classpath:/` keeps the packaged defaults while avoiding Spring Boot's working-directory search for production service configuration. The two additional locations are explicit files; the protected secrets file comes last and can override a value in the ordinary configuration file. Neither location is marked `optional:`, so a missing specified file stops startup instead of silently falling back. Set the paths in service configuration, not in a committed resource. Do not place secret **values** in command-line arguments. Environment variables such as `SPRING_DATASOURCE_PASSWORD` are also supported by Spring Boot, but the service must protect them from logging and unauthorized process inspection.

| OS | Application/runtime binaries | Installation configuration | Protected secrets | Mutable state and logs |
| --- | --- | --- | --- | --- |
| Windows | `%ProgramFiles%\HRIS` | `%ProgramData%\HRIS\config\application.properties` | `%ProgramData%\HRIS\secrets\secrets.properties` | Separate locations under `%ProgramData%\HRIS` |
| Ubuntu | `/opt/hris` | `/etc/hris/application.properties` | `/etc/hris/secrets/secrets.properties` | `/var/lib/hris` and `/var/log/hris` |

These are logical locations from D-029. The service wrapper must convert the actual Windows paths to absolute file URIs; it must not assume a drive letter or user home. For Ubuntu, the additional-location setting is, for example, `file:/etc/hris/application.properties,file:/etc/hris/secrets/secrets.properties`. The later installer determines directory creation, service identities, file ownership, and ACL automation. Until then, administrators must restrict the configuration and secret files to the dedicated service identity and authorized administrators/root, with stronger restrictions for secrets. MySQL data remains in its supported MySQL-managed location.

The installation configuration may hold technical settings such as `spring.datasource.url` and `spring.datasource.username`. Put `spring.datasource.password` and any future private key, recovery, or signing value in the protected secret source. No production credentials, reusable passwords, private keys, customer values, or machine-specific files belong in the repository. `.env` is ignored for local hygiene but is not parsed or required by the application.

Do not log resolved secret values or include them in ordinary diagnostics. Future configuration or diagnostic views must redact secrets under D-024. D-152 also requires stronger storage protection for credentials and recovery material. This task adds no diagnostic endpoint, production deployment automation, HTTPS certificate provisioning, or database provisioning.
