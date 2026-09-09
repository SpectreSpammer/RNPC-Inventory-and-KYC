# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Spring Boot 3.3.11 (Java 21) server-rendered inventory management app for RNPC. Thymeleaf templates + MySQL, with a parallel (currently unused) mssql-jdbc dependency. Manages three parts catalogs — computers, laptops, cellphone parts — plus an in-progress username/password login system.

## Common commands

```
./mvnw.cmd spring-boot:run          # run the app (Windows; use ./mvnw on POSIX shells)
./mvnw.cmd clean package            # build
./mvnw.cmd test                     # run all tests
./mvnw.cmd test -Dtest=InventoryApplicationTests   # run a single test class
```

The app runs on port **9090** (`server.port` in `application.properties`), not the Spring Boot default 8080.

Database: MySQL at `jdbc:mysql://localhost:3306/rnpc`, user `root`, no password (local dev only — see `src/main/resources/application.properties`). `spring.jpa.hibernate.ddl-auto=update`, so schema changes to `@Entity` classes apply automatically on next run; there are no migration scripts. A MySQL server with a `rnpc` database must be running locally before starting the app.

On every startup, `DataInitializer` seeds two demo users if they don't already exist: `admin`/`12345` (ADMIN) and `nand159`/`12345` (CUSTOMER). Credentials are stored and compared in plaintext (`UserRepository.findByUsernameAndPassword`) — there is no password hashing.

## Architecture

Standard layered Spring MVC, repeated three times (once per product category) plus a login/user slice:

```
Controller (@Controller, server-rendered) → Service (business logic + file I/O) → Repository (Spring Data JPA) → Entity
                                          ↘ DTO (validation + form binding) ↗
```

- **Controllers** (`controller/`) — one per product type: `ComputerPartsController` (`/computer`), `LaptopPartsController` (`/laptop`), `CellphonePartsController` (`/cellphone`). Each follows the identical route pattern: `GET /` (list), `GET /create` + `POST /create`, `GET /edit/{id}` + `PUT /update/{id}`, `DELETE /delete/{id}`. PUT/DELETE from HTML forms work via `spring.mvc.hiddenmethod.filter.enabled=true` and the `HiddenHttpMethodFilter` bean in `WebConfig` (forms POST with a hidden `_method` field). When adding a new product category, copy this controller/service/dto/entity/repository quartet rather than generalizing it — the codebase intentionally duplicates this pattern per category instead of sharing a generic CRUD layer.
- **Services** (`service/`) — hold the create/update/delete logic and, for products, handle image upload/deletion directly (no dedicated storage abstraction). Uploaded files are timestamp-prefixed (`System.currentTimeMillis() + "_" + originalFilename`) and written to `public/images/` relative to the process working directory (i.e. project root, *not* `src/main/resources/static` or `/public`). There is no `addResourceLocations` / static-resource-handler config anywhere in the app registering that filesystem directory for HTTP serving — if uploaded images 404 in the browser, check `WebConfig` / `application.properties` for a static-locations mapping before assuming the upload itself failed.
- **DTOs** (`dto/`) — form-backing objects with Bean Validation annotations (`@NotEmpty`, `@Pattern` allow-lists for brand/category/storage-size dropdowns, `@Min`, `@Size`) plus a `MultipartFile imageFile`. Controllers validate with `@Valid` + `BindingResult`; the image-required check is done manually in the controller (`imageFile.isEmpty()`) rather than via a validation annotation, and only on create, not update.
- **Entities** (`entity/`): `Computers` maps to table `rnpc_products`; `LaptopParts` and `CellphoneParts` map to their own tables. `User` maps to `users`, with a `Role` enum (`ADMIN`, `CUSTOMER`) stored via `@Enumerated(EnumType.STRING)`.
- **Login**: `LoginController` currently has its entire body commented out (work in progress on the `users-login-task` branch) — `/login`, `/logout`, `/admin`, and the per-username customer dashboard routes are all inactive. `UserService`/`UserRepository`/`User` and the `login.html` template exist and are ready to be wired up. `UserService.getRedirectUrl` hardcodes `http://localhost:9090/...` in the redirect target rather than using a relative path.

## Root-level `.txt` files

`Controller.txt`, `Dto.txt`, `Models.txt`, `create.txt`, `edit.txt`, `index.txt`, `repository.txt` at the repo root are working/reference notes, not part of the build (no `src/` files depend on them).
