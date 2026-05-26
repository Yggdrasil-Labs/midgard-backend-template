# Midgard Template Standardization Implementation Plan

状态：archived

归档位置：`docs/exec-plans/completed/`

原始位置：`docs/plans/2026-03-27-midgard-template-standardization.md`

相关设计文档：

- [2026-03-27-midgard-template-standardization-design.md](/home/yangyang/workspace/codes/Yggdrasil-Labs/midgard-backend-template/docs/exec-plans/completed/2026-03-27-midgard-template-standardization-design.md)

说明：

- 本文档是旧计划体系下的历史实现计划。
- 为了统一到新的 `exec-plans` 结构，已迁移为归档文档。
- 原始任务中的命令、提交节点与阶段划分保留不变，便于回看当时的执行思路。
- 下方正文属于历史记录，不应直接作为当前执行模板复用。
- 若需要新计划，请从 `docs/exec-plans/active/_template.md` 创建。

---

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Refactor the backend template so its packages, naming, and sample REST structure align with the agreed Midgard backend conventions.

**Architecture:** Keep the existing six-module COLA layout, but migrate all Java code to the `io.yggdrasil.labs.midgard` root package and reorganize module-internal packages around domain-first conventions. Apply naming normalization, endpoint cleanup, and small code hygiene fixes while preserving the sample `customer` domain behavior.

**Tech Stack:** Java 17, Maven multi-module build, Spring Boot, COLA components, MyBatis-Plus, JUnit 5

---

### Task 1: Baseline Verification

**Files:**
- Read: `pom.xml`
- Read: `adapter/pom.xml`
- Read: `app/pom.xml`
- Read: `domain/pom.xml`
- Read: `infrastructure/pom.xml`
- Read: `start/pom.xml`

**Step 1: Capture the current module layout**

Run: `rg --files`
Expected: See the six modules and current Java/test file layout.

**Step 2: Record package rename scope**

Run: `rg -n "package com\\.yggdrasil\\.labs|import com\\.yggdrasil\\.labs" .`
Expected: A complete list of package declarations and imports that must migrate.

**Step 3: Verify current build status before refactor**

Run: `./mvnw -q test`
Expected: Current baseline passes, or any pre-existing failure is documented before changes begin.

**Step 4: Commit the baseline checkpoint**

```bash
git add docs/plans/2026-03-27-midgard-template-standardization-design.md docs/plans/2026-03-27-midgard-template-standardization.md
git commit -m "docs: add midgard template standardization design and plan"
```

### Task 2: Migrate the Root Package

**Files:**
- Modify: `adapter/src/main/java/**`
- Modify: `app/src/main/java/**`
- Modify: `client/src/main/java/**`
- Modify: `domain/src/main/java/**`
- Modify: `infrastructure/src/main/java/**`
- Modify: `start/src/main/java/**`
- Modify: `**/src/test/java/**`

**Step 1: Write the failing structural check**

Run: `rg -n "package com\\.yggdrasil\\.labs|import com\\.yggdrasil\\.labs" adapter app client domain infrastructure start`
Expected: Existing matches prove the old root package is still present.

**Step 2: Move package directories to the new root**

Implementation:

- Move Java source and test directories from `com/yggdrasil/labs/...` to `io/yggdrasil/labs/midgard/...`
- Update every `package` declaration
- Update every internal `import`
- Update every `package-info.java`

**Step 3: Run compile to catch missing imports**

Run: `./mvnw -q -DskipTests compile`
Expected: Compile succeeds, or remaining broken imports are listed and fixed immediately.

**Step 4: Commit**

```bash
git add adapter app client domain infrastructure start
git commit -m "refactor: migrate Java packages to io.yggdrasil.labs.midgard"
```

### Task 3: Reorganize Adapter and App Packages

**Files:**
- Modify: `adapter/src/main/java/io/yggdrasil/labs/midgard/**`
- Modify: `app/src/main/java/io/yggdrasil/labs/midgard/**`

**Step 1: Write the failing structure check**

Run: `find adapter/src/main/java app/src/main/java -type f | sort`
Expected: Files still show legacy folders such as `controller`, `query`, or flat app service placement.

**Step 2: Reorganize packages**

Implementation:

- Move controller classes into `adapter.web.customer`
- Move web DTOs into `adapter.web.customer.dto`
- Move web convertors into `adapter.web.customer.convertor`
- Move app service interfaces and implementations into `app.customer.application`
- Move `query` package contents into `app.customer.executor` or `app.customer.dto.qry` as appropriate
- Move app DTOs into `app.customer.dto.cmd`, `app.customer.dto.qry`, and `app.customer.dto.co`

**Step 3: Update imports and package-info files**

Implementation:

- Fix all package declarations
- Fix all imports in adapter, app, and dependent modules
- Keep responsibilities unchanged while moving files

**Step 4: Run targeted compile**

Run: `./mvnw -q -pl adapter,app,start -am -DskipTests compile`
Expected: Adapter and app modules compile after relocation.

**Step 5: Commit**

```bash
git add adapter app start
git commit -m "refactor: reorganize adapter and app package structure"
```

### Task 4: Normalize Naming Conventions

**Files:**
- Modify: `app/src/main/java/io/yggdrasil/labs/midgard/app/customer/application/**`
- Modify: `app/src/main/java/io/yggdrasil/labs/midgard/app/customer/dto/**`
- Modify: `domain/src/main/java/io/yggdrasil/labs/midgard/domain/customer/**`
- Modify: `infrastructure/src/main/java/io/yggdrasil/labs/midgard/infrastructure/persistence/customer/**`

**Step 1: Write the failing name check**

Run: `rg -n "ApplicationService|ListCustomerQuery|repository|converter|query" adapter app domain infrastructure`
Expected: Output shows the legacy naming that must be normalized.

**Step 2: Rename classes and packages**

Implementation:

- `CustomerApplicationService` -> `CustomerAppService`
- `CustomerApplicationServiceImpl` -> `CustomerAppServiceImpl`
- `ListCustomerQuery` -> `ListCustomerQry`
- `repository` package -> `repo`
- `converter` package -> `convertor`
- `CustomerWebConverter` -> `CustomerWebConvertor`
- `CustomerInfraConverter` -> `CustomerInfraConvertor`

**Step 3: Update all call sites**

Implementation:

- Fix controller, executors, repository implementations, tests, and package-info files
- Ensure names remain semantically aligned with `CmdExe` and `QryExe`

**Step 4: Run compile and targeted tests**

Run: `./mvnw -q -pl app,domain,infrastructure -am test`
Expected: Modules and tests pass with renamed types.

**Step 5: Commit**

```bash
git add app domain infrastructure adapter
git commit -m "refactor: normalize service dto and convertor naming"
```

### Task 5: Tighten Domain and Infrastructure Boundaries

**Files:**
- Modify: `domain/src/main/java/io/yggdrasil/labs/midgard/domain/customer/model/Customer.java`
- Modify: `domain/src/main/java/io/yggdrasil/labs/midgard/domain/customer/repo/CustomerRepository.java`
- Modify: `infrastructure/src/main/java/io/yggdrasil/labs/midgard/infrastructure/persistence/customer/**`
- Modify: `app/src/main/java/io/yggdrasil/labs/midgard/app/customer/**`

**Step 1: Inspect current dependency directions**

Run: `rg -n "Autowired|@Transactional|RepositoryImpl|BizException" app domain infrastructure adapter`
Expected: Identify field injection and any layering smells worth correcting during the refactor.

**Step 2: Apply minimal boundary cleanup**

Implementation:

- Replace field injection with constructor injection in touched classes
- Keep transaction concerns in app layer only
- Ensure domain keeps only domain-facing interfaces and rules
- Keep infrastructure responsible for persistence implementation only

**Step 3: Run focused tests**

Run: `./mvnw -q -pl domain,infrastructure,app -am test`
Expected: No regressions after boundary cleanup.

**Step 4: Commit**

```bash
git add app domain infrastructure adapter
git commit -m "refactor: tighten layer boundaries and injection style"
```

### Task 6: Standardize REST Endpoint Style

**Files:**
- Modify: `adapter/src/main/java/io/yggdrasil/labs/midgard/adapter/web/customer/CustomerController.java`
- Modify: `adapter/src/main/java/io/yggdrasil/labs/midgard/adapter/web/customer/dto/CreateCustomerRequest.java`
- Modify: related tests if needed

**Step 1: Write the failing endpoint check**

Review current mappings in `CustomerController`.
Expected: See non-REST paths such as `/customer/list` and `/customer/add`.

**Step 2: Update request mappings**

Implementation:

- Change base path to `/api/customers`
- Map list to `GET /api/customers`
- Map create to `POST /api/customers`
- Keep validation annotations and transport conversion explicit

**Step 3: Run app module tests or startup smoke check**

Run: `./mvnw -q -pl start -am test`
Expected: Build succeeds and no mapping-related wiring breaks.

**Step 4: Commit**

```bash
git add adapter start app
git commit -m "refactor: standardize sample customer REST endpoints"
```

### Task 7: Update Bootstrapping and Documentation

**Files:**
- Modify: `start/src/main/java/io/yggdrasil/labs/midgard/start/Application.java`
- Modify: `README.md`
- Modify: package-info files touched during migration
