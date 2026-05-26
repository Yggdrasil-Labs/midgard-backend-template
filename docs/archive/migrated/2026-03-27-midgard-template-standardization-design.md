# Midgard Template Standardization Design

状态：archived

归档位置：`docs/exec-plans/completed/`

原始位置：`docs/plans/2026-03-27-midgard-template-standardization-design.md`

相关实现计划：

- [2026-03-27-midgard-template-standardization.md](/home/yangyang/workspace/codes/Yggdrasil-Labs/midgard-backend-template/docs/exec-plans/completed/2026-03-27-midgard-template-standardization.md)

说明：

- 本文档是旧计划体系下产出的历史设计文档。
- 已迁移到新的 completed 归档体系中，作为模板标准化工作的背景与设计依据。
- 主体内容保留原貌，仅增加归档元信息与交叉链接。

---

## Background

The current repository already follows a six-module COLA-style split:

- `adapter`
- `client`
- `app`
- `domain`
- `infrastructure`
- `start`

However, the internal package structure still reflects an earlier template style instead of the agreed backend conventions from the Mealmate engineering specification. The most visible mismatch is the root package, which is still `com.yggdrasil.labs...` instead of the agreed `io.yggdrasil.labs.midgard`.

This repository is a backend template, not a concrete business project. The goal of this work is to make the template architecture and naming conventions consistent, predictable, and ready to be copied into future services.

## Goals

- Standardize the root package to `io.yggdrasil.labs.midgard`
- Keep the existing six-module COLA split
- Reorganize module-internal packages around domain-first structure
- Normalize naming for app services, DTOs, repositories, and convertors
- Align REST endpoint style with the agreed backend conventions
- Tighten code style and layer responsibilities without introducing new business features

## Non-Goals

- Replacing the sample `customer` domain with real Mealmate domains
- Introducing new middleware or infrastructure components
- Expanding the template into a full platform framework
- Adding compatibility layers for old package names or endpoint paths

## Target Architecture

The repository will keep the existing Maven module layout:

```text
midgard-backend-template
├── adapter
├── client
├── app
├── domain
├── infrastructure
└── start
```

The key change is inside each module. All Java code should move under:

```text
io.yggdrasil.labs.midgard
```

Using the current `customer` sample as the example domain, the target package shape is:

```text
io.yggdrasil.labs.midgard
├── adapter.web.customer
│   ├── CustomerController
│   ├── dto
│   │   ├── CreateCustomerRequest
│   │   └── CustomerResponse
│   └── convertor
├── client.customer
│   ├── CustomerClient
│   └── dto
├── app.customer
│   ├── application
│   │   ├── CustomerAppService
│   │   └── CustomerAppServiceImpl
│   ├── executor
│   │   ├── CreateCustomerCmdExe
│   │   └── ListCustomerQryExe
│   └── dto
│       ├── cmd
│       ├── qry
│       └── co
├── domain.customer
│   ├── model
│   ├── service
│   └── repo
├── infrastructure.persistence.customer
│   ├── dataobject
│   ├── mapper
│   ├── convertor
│   └── CustomerRepositoryImpl
└── start
```

## Design Decisions

### 1. Root Package

All code moves from `com.yggdrasil.labs...` to `io.yggdrasil.labs.midgard...`.

Reasoning:

- Matches the agreed engineering convention
- Reflects that this is a Yggdrasil Labs template under the Midgard name
- Removes the hard-coded `mealmate` business identity from the template

### 2. Domain-First Package Organization

Packages should be grouped by domain inside each layer rather than by generic technical buckets only.

Examples:

- Keep `adapter.web.customer` instead of flattening everything into `adapter.web.controller`
- Keep `infrastructure.persistence.customer` instead of broad `persistence.impl`

Reasoning:

- Easier to scale when a second or third domain is added
- Better locality for files that change together
- Clearer ownership and navigation for future contributors

### 3. Naming Normalization

The repository will adopt the following naming adjustments:

- `CustomerApplicationService` -> `CustomerAppService`
- `CustomerApplicationServiceImpl` -> `CustomerAppServiceImpl`
- `ListCustomerQuery` -> `ListCustomerQry`
- package `repository` -> `repo`
- package `query` -> `qry`
- suffix `Converter` -> `Convertor`

Reasoning:

- Reduces mixed vocabulary in the template
- Aligns code with the naming table in the backend conventions
- Makes the app-layer naming consistent with `CmdExe` and `QryExe`

### 4. REST Endpoint Style

Current endpoint patterns such as:

- `GET /customer/list`
- `POST /customer/add`

will be moved toward REST-style paths:

- `GET /api/customers`
- `POST /api/customers`

Reasoning:

- Consistent with the agreed API rules
- Easier to extend with future resource operations
- Better fit for generated API documentation and client expectations

### 5. Compatibility Policy

This is a template repository, so internal consistency is more important than preserving old sample API compatibility.

Decision:

- Do not keep old package aliases
- Do not keep dual endpoint mappings
- Do not add transitional wrappers solely for backward compatibility

Reasoning:

- Compatibility layers would preserve outdated patterns inside the template
- Future projects should inherit the new structure, not the migration burden

## Layer Responsibilities

The following responsibilities should be preserved and clarified during refactoring:

- `adapter`: request mapping, protocol DTOs, validation, protocol-to-app conversion
- `app`: use-case orchestration, transaction boundary, executor coordination
- `domain`: business rules, entities, value semantics, repository interfaces
- `infrastructure`: persistence implementation, external adapters, generated mappers
- `client`: external-facing contracts and shared DTOs
- `start`: Spring Boot bootstrap and environment assembly

Additional rules:

- `adapter` must not call `infrastructure` directly
- `domain` must not depend on outer layers
- transaction annotations belong in the app layer only

## Code Quality Improvements Included

This refactor should include a few lightweight quality improvements while touching the affected files:

- Replace field injection with constructor injection
- Update package-info declarations to the new packages
- Remove outdated template comments and inconsistent wording
- Keep controller logic thin and focused on transport concerns
- Make DTO boundaries clearer between web, app, and client layers

These changes are intentionally limited to improvements that naturally fit the structural refactor.

## Validation Strategy

The refactor is considered successful when all of the following are true:

1. The project compiles after package and class migration
2. Existing tests pass after import and naming updates
3. The Spring Boot application still starts with the new root scan path
4. Key classes are located in the expected target packages
5. README or related docs no longer reference the old package root where relevant

## Risks

### Wide Rename Surface

Package migration will affect imports, package declarations, scan paths, generated mapper references, tests, and package-info files.

Mitigation:

- Perform migration in small, verifiable steps
- Compile after each major rename wave

### Generated or Convention-Based Integrations

Persistence code may rely on package names or generated artifacts.

Mitigation:

- Inspect mapper references and generated package expectations during implementation
- Verify with module compile and tests before completion

### Accidental Overreach

Structural cleanup can easily drift into business redesign.

Mitigation:

- Keep the sample `customer` domain intact
- Restrict changes to naming, placement, and layer hygiene

## Expected Outcome

After this work, the repository should behave as a cleaner backend template with:

- a standardized `io.yggdrasil.labs.midgard` root package
- domain-oriented package organization inside each module
