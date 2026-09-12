# Project instructions

## Overview

- `Copilot Middleware` is a Java 21 / Spring Boot 4.1 application.
- The build uses Maven; always use the checked-in `./mvnw` wrapper.
- OpenAPI documentation is generated automatically for the REST API.

## Project structure

- Production sources live in `src/main/java` and tests in `src/test/java`.
- Code is organized by domain/feature.
- Application configuration lives in `src/main/resources/application.properties`.

## Development commands

Run from the repository root:

```shell
./mvnw spring-boot:run
./mvnw test
./mvnw package
```


## Working conventions

- Use constructor injection and follow adjacent Java code and Lombok patterns.
- Prefer integration tests; use unit tests for focused coverage.
- Run the narrowest meaningful verification while iterating.
- Keep changes task-scoped and preserve unrelated existing changes. When a required file is already modified, integrate with those changes.
- Never commit production credentials. Keep secrets in environment variables or local, unversioned overrides.
- For outbound HTTP integrations, prefer Spring `RestClient` with environment-driven base URLs, tokens, and version headers configured in `application.properties`.
- Add concise, verifiable repository-wide guidance discovered during a task to `AGENTS.md`; omit task details and duplicate documentation.
