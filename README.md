<a href="https://fireflysoftware.io/openplatform/">
<picture>
  <source media="(max-width: 640px) and (prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-narrow-dark.svg">
  <source media="(max-width: 640px)" srcset="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-narrow-light.svg">
  <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-dark.svg">
  <img src="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-light.svg" alt="Firefly OpenPlatform - Domain Insurance - Delegated Authority" width="100%">
</picture>
</a>

# Firefly OpenPlatform - Domain Insurance - Delegated Authority

Developed by the Firefly Software Foundation. [dev@fireflysoftware.io](mailto:dev@fireflysoftware.io)

[Website](https://fireflysoftware.io/) · [OpenPlatform](https://fireflysoftware.io/openplatform/) · [Module catalogue](https://fireflysoftware.io/openplatform/modules/)

Domain layer microservice for delegated authority binders within the Firefly platform. This service orchestrates create, update, status change, close, and query against `core-insurance-delegated-authority` with compensating saga steps.

Repository: [https://github.com/fireflysoftwaresolutions/domain-insurance-delegated-authority](https://github.com/fireflysoftwaresolutions/domain-insurance-delegated-authority)

---

## Overview

`domain-insurance-delegated-authority` sits between experience services and `core-insurance-delegated-authority`. Write operations run through the FireflyFramework saga engine. Queries use the CQRS query bus. The domain depends on the core SDK only. It does not import another core's internal packages. Identifiers that point at parties, contracts, products, or policies stay UUIDs owned by those cores.

It creates, updates, changes status, and closes binders through the delegated authority core. It does not bind a risk on the policies core, does not move money, and does not certify IFRS 17 or Solvency II.

### Key features

- **Create** -- register a binder through `core-insurance-delegated-authority`, with compensation that deletes the core write
- **Update** -- read the current binder and write the new field values through `updateBinder` on `core-insurance-delegated-authority`. Status is left unchanged. Compensation writes the previous record back.
- **Status change** -- read the current binder and update its status through `core-insurance-delegated-authority`. This core keeps status on the record, so the status saga does not append a separate history row. Compensation writes the previous status back through the core.
- **Close** -- Expire runs the status saga and is refused unless the binder is active or suspended. Remove deletes a draft only. A later status is kept.
- **Query** -- read one binder by identifier
- **Compensation** -- a failed later step asks the core to undo the work already accepted
- **CQRS** -- commands and queries travel on separate buses
- **Reactive I/O** -- Spring WebFlux with virtual threads enabled

## Architecture

### Module structure

| Module | Purpose |
|--------|---------|
| `domain-insurance-delegated-authority-core` | Saga workflows, commands, handlers, and the application service |
| `domain-insurance-delegated-authority-interfaces` | Request contracts for a status change and a closing reason |
| `domain-insurance-delegated-authority-infra` | SDK client factory and downstream base-path configuration |
| `domain-insurance-delegated-authority-web` | Spring Boot application, REST API, actuator, and OpenAPI |
| `domain-insurance-delegated-authority-sdk` | Java client generated from the OpenAPI document |

### Dependency flow

```
web --> interfaces --> core --> infra --> core-insurance-delegated-authority-sdk
sdk (generated from openapi.yml)
```

### Saga map

| Saga | Core calls | Compensation |
|------|------------|--------------|
| `RegisterBinderSaga` | `createBinder` on `BindersApi` | Deletes the record through `deleteBinder` |
| `UpdateBinderSaga` | Reads the current record, then writes field changes through `updateBinder` on `BindersApi`. Status is left unchanged. | Writes the previous record back through `updateBinder` |
| `UpdateBinderStatusSaga` | Reads the current record, then updates its status through `updateBinder` | Writes the previous status back through the core |
| Query | `getBinderById` on `BindersApi` | None. A read has nothing to undo. |

Status values accepted by the core: `DRAFT`, `ACTIVE`, `SUSPENDED`, `EXPIRED`.

## Setup

### Prerequisites

- Java 25
- Apache Maven 3.9+
- `core-insurance-delegated-authority-sdk` 26.10.6
- Apache Kafka on `localhost:9092` when event publishing is left at the default

### Environment

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_ADDRESS` | `localhost` | Bind address |
| `SERVER_PORT` | `8075` | HTTP port |
| `ENDPOINT_CORE_INSURANCE_DELEGATED_AUTHORITY` | `http://localhost:8080` | Base URL of `core-insurance-delegated-authority` |

### Build and test

```bash
mvn clean verify
```

### Run

```bash
ENDPOINT_CORE_INSURANCE_DELEGATED_AUTHORITY=http://localhost:8080 mvn spring-boot:run -pl domain-insurance-delegated-authority-web
```

Swagger UI (non-production): [http://localhost:8075/swagger-ui.html](http://localhost:8075/swagger-ui.html)

## API

| Method | Path | Behavior |
|--------|------|----------|
| `POST` | `/api/v1/binders` | Runs `RegisterBinderSaga` |
| `GET` | `/api/v1/binders/{binderId}` | Queries the core |
| `PUT` | `/api/v1/binders/{binderId}/status` | Runs `UpdateBinderStatusSaga` |
| `PUT` | `/api/v1/binders/{binderId}` | Runs `UpdateBinderSaga` |
| `POST` | `/api/v1/binders/{binderId}/expire` | Expire binder. Runs `UpdateBinderStatusSaga` when the core lifecycle allows `EXPIRED`. |
| `DELETE` | `/api/v1/binders/{binderId}` | Deletes the record through `core-insurance-delegated-authority` when that core allows removal |

The status request body is `status` plus an optional `statusReason`.

Expire runs the status saga and is refused unless the binder is active or suspended. Remove deletes a draft only. A later status is kept.
A closing command body is an optional `statusReason`.

