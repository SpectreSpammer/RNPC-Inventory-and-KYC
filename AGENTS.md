# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Working agreement

**Do not run the app, do not touch the database, and do not commit.** The owner runs and reviews the
app themselves through IntelliJ and keeps control of when it starts, when the DB is hit, and what
enters git history. Write the change, verify it statically, report, and stop.

Note that `./mvnw.cmd test` is **not** safe under that rule: `InventoryApplicationTests.contextLoads`
boots a full Spring context and connects to the dev database. Run only narrowly scoped unit tests by
name (`-Dtest=SalesReportServiceTest`) that mock their collaborators and need no context.

To verify work without running anything: compile, read the templates, and where behaviour genuinely
needs proving, write a mock-based unit test or build a throwaway harness outside the repo. Then hand
the owner the steps to check it ("stop the app in IntelliJ, rebuild, restart, reload /sales").

On Railway, "after deploy" SQL means **once the new deployment shows ACTIVE**, not when the push
goes out: the old build keeps serving traffic throughout the build, and breaks if its columns are
dropped early.

## File encoding: UTF-8, no BOM

**Every text file in this repo must be saved as UTF-8 without a BOM.** Spring Boot reads Thymeleaf
templates as UTF-8 and `spring-boot-starter-parent` compiles sources as UTF-8, so anything else is
silently wrong until it reaches a browser.

This has already broken twice, both times a single cp1252 byte written where UTF-8 was expected:

- `templates/support/detail.html` - four bare `0xB7` (cp1252 `·` MIDDLE DOT), which rendered the
  ticket header as `SUP-00003 � Appointment`. **Fixed** by re-encoding to `0xC2 0xB7`.
- `templates/admin/dashboard.html` - one bare `0x97` (cp1252 `—` EM DASH) used as the null-date
  placeholder in the recent-orders table, which rendered `�` for any order with a null
  `createdAt`. **Fixed** by re-encoding to `0xE2 0x80 0x94`.

A lone high byte like this is *not valid UTF-8 at all*, so the decoder substitutes U+FFFD. Correctly
encoded punctuation is fine and already used elsewhere - `sales/salesReport.html` contains `—`, `–`,
`·`, `×` and `›` as proper multi-byte sequences.

Two things to watch: an editor set to the Windows ANSI codepage will reintroduce this silently, and
two of the four occurrences in `detail.html` were inside `#temporals.format(...)` **pattern
strings**, where an HTML entity such as `&middot;` is not a safe substitute. To check a file:
`python -c "open('path','rb').read().decode('utf-8')"` - it raises if the file is broken.

## Project overview

Spring Boot 3.3.11 (Java 21) server-rendered shop-management app for RN PC, a PC and cellphone
repair shop. Thymeleaf + MySQL/MariaDB, Google-only sign-in.

It is much more than an inventory CRUD app. It covers: a ten-category parts inventory, a PC builder
with saved builds, customer orders with checkout and a build timeline, appointments, repair records
with warranty tracking, support tickets, notifications, per-user search, a sales report, and
separate customer and admin dashboards.

## Common commands

```
./mvnw.cmd clean package            # build (Windows; use ./mvnw on POSIX shells)
./mvnw.cmd -o compile               # fast compile check - the usual way to verify a change
./mvnw.cmd test -Dtest=SalesReportServiceTest   # a single, context-free test class
```

`./mvnw.cmd spring-boot:run` starts the app, but see the working agreement above - don't.

The app listens on `${PORT:9090}` - 9090 locally, and whatever Railway injects when deployed.

## Database

`spring.datasource.url` is `jdbc:mysql://${MYSQLHOST:localhost}:${MYSQLPORT:3306}/${MYSQLDATABASE:rnpc}`
with `${MYSQLUSER:root}` / `${MYSQLPASSWORD:}`. Locally that resolves to XAMPP's bundled MariaDB with
no password; on Railway the MySQL plugin injects those five env vars, so the same properties file
works unmodified in both places.

`spring.jpa.hibernate.ddl-auto=update`, so `@Entity` changes apply on next start. There are no
migration scripts, and no rollback - be deliberate about entity edits.

Secrets (e.g. the Xendit key) live in `application-secrets.properties`, which is gitignored and
imported optionally, so the app still starts when it's missing.

## Database changes not in migrations

There are **no migration scripts**. `ddl-auto=update` creates everything below automatically on a
fresh start, so a local dev database needs nothing done by hand. The DDL is recorded here for a
managed database where `ddl-auto` is off or schema changes are reviewed - and as the record of what
has been applied by hand so far.

`ddl-auto=update` only ever **adds**; it never drops or alters an existing column. So a column whose
type or nullability changes in an entity will *not* be updated on an existing database.

**Two columns added to the existing `rnpc_orders`** (`Order.buildStage` / `fulfilmentMethod`, both
`@Enumerated(STRING)` with no `@Column`, so Hibernate's default `VARCHAR(255)`, both nullable -
`buildStage` is null until an admin marks the order paid):

```sql
ALTER TABLE rnpc_orders
    ADD COLUMN build_stage       VARCHAR(255) NULL,
    ADD COLUMN fulfilment_method VARCHAR(255) NULL;
```

**Two columns added to the existing `rnpc_appointments`** (by `d48f703`; `Appointment.serviceType` /
`deviceCategory`, both explicitly `VARCHAR(32)` and nullable - the table itself predates these):

```sql
ALTER TABLE rnpc_appointments
    ADD COLUMN service_type    VARCHAR(32) NULL,
    ADD COLUMN device_category VARCHAR(32) NULL;
```

**Three new tables for support tickets.** `data` is `LONGBLOB` - set explicitly via
`columnDefinition`, and also what Hibernate would infer for a `@Lob byte[]` on MySQL:

```sql
CREATE TABLE rnpc_support_tickets (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    topic       VARCHAR(32)  NOT NULL,
    message     TEXT         NOT NULL,
    status      VARCHAR(32)  NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NULL,
    version     BIGINT       NULL,
    PRIMARY KEY (id),
    KEY idx_support_ticket_user (user_id),
    CONSTRAINT fk_support_ticket_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE rnpc_support_replies (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    ticket_id   BIGINT       NOT NULL,
    author_id   BIGINT       NOT NULL,
    message     TEXT         NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_support_reply_ticket FOREIGN KEY (ticket_id) REFERENCES rnpc_support_tickets (id),
    CONSTRAINT fk_support_reply_author FOREIGN KEY (author_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE rnpc_support_attachments (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    ticket_id     BIGINT       NOT NULL,
    filename      VARCHAR(255) NOT NULL,
    content_type  VARCHAR(255) NOT NULL,
    size          BIGINT       NOT NULL,
    data          LONGBLOB     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_support_attachment_ticket FOREIGN KEY (ticket_id) REFERENCES rnpc_support_tickets (id)
) ENGINE=InnoDB;
```

`users.id` is `BIGINT AUTO_INCREMENT`, which is what all three foreign keys reference. With 10 MB
attachments going into a `LONGBLOB`, the practical ceiling is the server's `max_allowed_packet`
(16 MB on XAMPP's MariaDB 10.4), not the column type.

**`rnpc_laptop_parts`, redesigned** (the laptop redesign, batches 1-7 - see Laptop parts below). The
table predates the redesign: `laptop_part_id`, `brand`, `part_name`, `stocks`, `price`,
`description`, `created_at` and `image_file_name` are the original columns, and `ddl-auto` added the
other 45, all nullable. On a fresh database `ddl-auto` creates the whole table as below. This is
Hibernate 6.5's own DDL for the entity (MySQL and MariaDB dialects are identical), reordered by
group; `FLOAT(53)` is a `DOUBLE`, `BIT` a boolean:

```sql
CREATE TABLE rnpc_laptop_parts (
    laptop_part_id         INTEGER NOT NULL AUTO_INCREMENT,
    part_type              VARCHAR(32) NULL,
    part_condition         VARCHAR(32) NULL,
    brand                  VARCHAR(255) NULL,
    part_name              VARCHAR(255) NULL,
    compatible_models      VARCHAR(500) NULL,
    part_number            VARCHAR(64) NULL,
    warranty_days          INTEGER NULL,
    stocks                 INTEGER NOT NULL,
    price                  FLOAT(53) NOT NULL,
    description            TEXT NULL,
    created_at             DATETIME(6) NULL,
    image_file_name        VARCHAR(255) NULL,
    lcd_connector          VARCHAR(255) NULL,
    lcd_mounting           VARCHAR(255) NULL,
    lcd_panel_type         VARCHAR(255) NULL,
    lcd_refresh_rate_hz    INTEGER NULL,
    lcd_resolution         VARCHAR(255) NULL,
    lcd_size_inches        FLOAT(53) NULL,
    lcd_surface            VARCHAR(255) NULL,
    lcd_touch              BIT NULL,
    kb_backlit             BIT NULL,
    kb_color               VARCHAR(255) NULL,
    kb_layout              VARCHAR(255) NULL,
    kb_with_frame          BIT NULL,
    kb_with_palmrest       BIT NULL,
    battery_capacity_wh    FLOAT(53) NULL,
    battery_cells          INTEGER NULL,
    battery_chemistry      VARCHAR(255) NULL,
    battery_voltage        FLOAT(53) NULL,
    charger_connector_tip  VARCHAR(255) NULL,
    charger_current_a      FLOAT(53) NULL,
    charger_includes_cord  BIT NULL,
    charger_output_voltage FLOAT(53) NULL,
    charger_wattage        INTEGER NULL,
    ram_capacity_gb        INTEGER NULL,
    ram_speed_mts          INTEGER NULL,
    ram_type               VARCHAR(255) NULL,
    storage_capacity_gb    INTEGER NULL,
    storage_form_factor    VARCHAR(255) NULL,
    storage_interface      VARCHAR(255) NULL,
    storage_type           VARCHAR(255) NULL,
    casing_color           VARCHAR(255) NULL,
    casing_panel           VARCHAR(255) NULL,
    hinge_side             VARCHAR(255) NULL,
    dc_jack_tip_size       VARCHAR(255) NULL,
    dc_jack_type           VARCHAR(255) NULL,
    wifi_bluetooth_version VARCHAR(255) NULL,
    wifi_form_factor       VARCHAR(255) NULL,
    wifi_standard          VARCHAR(255) NULL,
    touchpad_color         VARCHAR(255) NULL,
    touchpad_connector     VARCHAR(255) NULL,
    touchpad_with_bracket  BIT NULL,
    PRIMARY KEY (laptop_part_id)
) ENGINE=InnoDB;
```

Two things about that table are deliberate:

- `part_type` and `part_condition` are `VARCHAR(32)`, set via `columnDefinition`, **not** a native
  MySQL `ENUM`. Hibernate 6 can generate `ENUM('LCD', ...)` for `@Enumerated(STRING)`, and
  `ddl-auto` cannot alter that when a `PartType` constant is added.
- The entity maps **every** redesign column nullable, `part_type` and `part_condition` included.
  `ddl-auto` adding a `NOT NULL` column to a populated table fills it with `''`, which an enum
  cannot load, so those two are made `NOT NULL` by hand instead (below).

**Applied by hand** - `ddl-auto` never drops or alters. The entity no longer maps `category` and
`storage_size` (the pre-redesign model, retired in batch 7) or the seven `fan_*` / `mb_*` columns
(the Fan / Heatsink and Motherboard types, removed in batch 5). Hibernate ignores unmapped columns,
so they are harmless until dropped. The `fan_*` / `mb_*` columns only exist on a database that ran a
batch 1-4 build (the local one); a database first deployed at batch 7 or later, like Railway, never
has them:

```sql
-- Only where they exist (local): the removed Fan / Heatsink and Motherboard types.
ALTER TABLE rnpc_laptop_parts
    DROP COLUMN fan_assembly,
    DROP COLUMN fan_connector_pins,
    DROP COLUMN fan_voltage,
    DROP COLUMN mb_onboard_cpu,
    DROP COLUMN mb_gpu,
    DROP COLUMN mb_onboard_ram,
    DROP COLUMN mb_tested_status;

-- Everywhere the table predates the redesign: the pre-redesign model.
ALTER TABLE rnpc_laptop_parts
    DROP COLUMN category,
    DROP COLUMN storage_size;

-- Must return 0 before the next statement.
SELECT COUNT(*) FROM rnpc_laptop_parts WHERE part_type IS NULL OR part_condition IS NULL;

ALTER TABLE rnpc_laptop_parts
    MODIFY COLUMN part_type      VARCHAR(32) NOT NULL,
    MODIFY COLUMN part_condition VARCHAR(32) NOT NULL;
```

Order matters: `category` / `storage_size` may only be dropped, and the `NOT NULL` applied, once
the running build is batch 7 or later. An older build still maps those two columns (dropping them
breaks every `/laptop` query), and its untyped `/laptop/create` form inserts a null `part_type`.
The `fan_*` / `mb_*` drops are safe on any batch 5+ build. On Railway, wait for the deployment to
show ACTIVE - see the working agreement.

**Applied (September 2026).** Both databases are done; the statements above are kept as the record
of what ran, not as work outstanding:

- **Local (XAMPP MariaDB):** dropped `fan_assembly`, `fan_connector_pins`, `fan_voltage`,
  `mb_onboard_cpu`, `mb_gpu`, `mb_onboard_ram`, `mb_tested_status`, `category` and `storage_size`;
  `part_type` and `part_condition` are `NOT NULL`.
- **Railway (MySQL):** dropped `category` and `storage_size`; `part_type` and `part_condition` are
  `NOT NULL`. The `fan_*` / `mb_*` columns never existed there - the entity stopped mapping them
  before the redesign's first deploy, so `ddl-auto` never created them.

## Sign-in and authorization

Google-only OAuth2/OIDC. There is no username/password login, no local sign-up, and no passwords
stored (`users.password` remains in the schema but is unused).

- `CustomOidcUserService` is the class that actually runs, because Google's registration requests
  the `openid` scope. It rejects a missing or unverified email, auto-creates a CUSTOMER `User` on
  first sign-in (username = email, full name from the Google profile, `googleId` = subject),
  backfills `googleId` on pre-seeded accounts, and sets `nameAttributeKey` to `"email"` so
  `authentication.getName()` is the email everywhere.
- `CustomOAuth2UserService` is the same logic for a plain (non-OIDC) provider. Nothing triggers it
  today; it's there for a future provider.
- `DataInitializer` pre-registers `rnpcandcellphonerepairshop@gmail.com` as ADMIN via
  `UserService.ensureAdminEmail`, so that email gets ADMIN on its first Google sign-in instead of
  defaulting to CUSTOMER. It also seeds the eight PC-component catalogs.

**Authorization is hand-rolled in application code, with one exception in the filter chain.**
`SecurityConfig` still ends in `anyRequest().permitAll()` and disables CSRF app-wide (no form in
the app carries a CSRF token). Every controller re-implements its own `isSignedIn`/`isAdmin` pair
checking for `ROLE_ADMIN`, and `GlobalNavAttributes` (a `@ControllerAdvice`) exposes
`navAuthenticated`/`navIsAdmin` to every template. **This is the single most important thing to
know before touching any page: adding a new controller does NOT make it protected.** There's no
`thymeleaf-extras-springsecurity6` dependency, so `sec:authorize` is not available in templates -
use `${navIsAdmin}`.

### The parts routes are the filter-chain exception

The eleven parts controllers had no admin check of their own, which left every route on them open
to anonymous users - not only the list pages but `POST /cpu/create`, `PUT /cpu/update/{id}` and
`DELETE /cpu/delete/{id}`. They are now denied in the filter chain instead, via
`ADMIN_ONLY_PARTS_PATHS` in `SecurityConfig`:

```
/computer, /computer/**, /cpu/**, /motherboard/**, /gpu/**, /ram/**, /storage/**,
/psu/**, /case/**, /cooler/**, /laptop/**, /cellphone/**        -> hasRole("ADMIN")
```

That rule is ordered **before** the catch-all `permitAll` - Spring matches in order, so moving it
after would silently disable it. `hasRole("ADMIN")` matches the `ROLE_ADMIN` authority
`CustomOidcUserService` already grants; Spring adds the `ROLE_` prefix itself. Enforcing here
rather than per method covers every HTTP verb and every handler added to those controllers later,
without depending on roughly seventy individual guards.

These controllers still have **no** `isAdmin` method of their own - the filter chain is the only
thing protecting them, so don't remove or narrow that rule on the assumption a controller check
exists behind it.

**Where a blocked request goes.** `exceptionHandling` supplies both halves, and the two cases are
genuinely different:

- **Anonymous** (an `AuthenticationException`) -> the authentication entry point redirects to
  `/login`.
- **Signed in but not an admin** (an `AccessDeniedException`) -> the access denied handler
  redirects to `/`.

Without those handlers both cases render Spring's default 403 page.

**Scripted-request carve-out.** Both handlers consult one shared `wantsMachineResponse(request)`
predicate; when it returns true they send `401`/`403` instead of redirecting, so a `fetch` never
receives an HTML sign-in page it would render into the DOM or follow and report as success. It is
one predicate rather than a `DelegatingAuthenticationEntryPoint` because there is no delegating
equivalent for the denied side, and the two paths must not disagree about what counts as scripted.

It returns true for either:

- `X-Requested-With: XMLHttpRequest`, or
- an `Accept` header that mentions `application/json` **and** does not mention `text/html` (a
  browser navigation sends `text/html`, so it never matches).

⚠️ **A bare `fetch(url)` gets neither** - its default `Accept` is `*/*` and `X-Requested-With` is
an explicit opt-in. A fetch that wants a status code back has to ask:

```js
fetch(url, { method: 'DELETE', headers: { 'X-Requested-With': 'XMLHttpRequest' } })
```

The `removePhoto` handler in all ten `*EditParts.html` templates does exactly this and checks
`res.ok`. Before that it sent no header, so an expired session returned a 302 that `fetch` followed
into a 200 plus login-page HTML, and the handler called `window.location.reload()` as though the
delete had succeeded.

**These handlers only see Spring Security's own exceptions.** `SupportController` throws
`ResponseStatusException(UNAUTHORIZED / FORBIDDEN)` from inside the controller. That is a Spring
**MVC** exception, resolved by `DispatcherServlet`, and it never reaches `ExceptionTranslationFilter`
- so those routes still return bare 401/403 and are unaffected by the redirects above.

## Architecture

Standard layered Spring MVC, server-rendered throughout:

```
Controller (@Controller) → Service (business logic + file I/O) → Repository (Spring Data JPA) → Entity
                         ↘ DTO (validation + form binding) ↗
```

### Parts inventory - per-category slices

`CpuPartsController` (`/cpu`), `GpuPartsController` (`/gpu`), `MotherboardPartsController`
(`/motherboard`), `RamPartsController` (`/ram`), `StoragePartsController` (`/storage`),
`PsuPartsController` (`/psu`), `CasePartsController` (`/case`), `CoolerPartsController` (`/cooler`),
`CellphonePartsController` (`/cellphone`), and `LaptopPartsController` (`/laptop`).

**Laptop is no longer one of the near-identical slices.** Since the laptop redesign it is one
controller over twelve part types, with per-type forms and validation groups - see Laptop parts
below. Everything else in this section describes the other nine.

Each has the identical route set: `GET /` (list), `GET /create` + `POST /create`,
`GET /edit/{id}` + `PUT /update/{id}`, `DELETE /delete/{id}`, and most also
`DELETE /removePhoto/{id}`. PUT/DELETE from HTML forms work via
`spring.mvc.hiddenmethod.filter.enabled=true` plus the `HiddenHttpMethodFilter` bean in `WebConfig`
(forms POST with a hidden `_method` field).

**When adding a parts category, copy the controller/service/dto/entity/repository quintet rather
than generalizing it** - the codebase intentionally duplicates this pattern per category instead of
sharing a generic CRUD layer.

⚠️ **A new category also needs its path added to `ADMIN_ONLY_PARTS_PATHS` in `SecurityConfig`.**
These controllers carry no admin check of their own, so a new one is world-writable until that
entry exists - see the filter-chain exception under Sign-in and authorization.

DTOs carry the Bean Validation (`@NotEmpty`, `@Pattern` allow-lists for dropdowns, `@Min`, `@Size`)
plus a `MultipartFile imageFile`. Controllers validate with `@Valid` + `BindingResult`. The photo
is optional everywhere except Cellphone, whose create handler still rejects an empty `imageFile`
by hand.

`PartsController` is separate and owns just `GET /computer`, a read-only combined view of all eight
PC-component catalogs. There is no `ComputerPartsController` and no `Computers` entity.

### The parts inventory page (`/computer`)

`products/allParts.html` is converted to `fragments/layout-app.html` (sidebar keyed
`active='pc-parts'`), matching `gpt/admin/computer.png`. Styling is in `static/css/parts.css`.

`PartsController.showPartsList` supplies, beyond the existing `parts` and `categories`:
`currentUsername`, `currentRole`, `unreadNotifications` and `recentNotifications` for the shared
topbar, plus `lowStockLimit`, which **reuses `AdminDashboardService.LOW_STOCK_LIMIT`** rather than
restating "5 or fewer" - keep it that way so the page and the admin dashboard can't disagree. The
route is admin-only via the filter chain, so the controller populates the admin inbox
unconditionally and carries no `isAdmin` check of its own.

**The page is still entirely client-side.** The whole catalog is inlined into the page as
`ALL_PARTS` and filtered, sorted and paginated in the browser; the conversion added **no new query
and no new repository call**. Every count on the page is derived from `ALL_PARTS` rather than
passed in, so the stat cards and chip counts cannot drift from the rows actually rendered.

What the page offers:

- **Stock-filter cards** - All parts / Low stock / Out of stock, which double as the stock filter.
  They are `<button aria-pressed>` with a Showing/Show label, written as bespoke markup because
  `stat-card.html` renders only a static card or a link, neither of which can toggle.
- **Category chips with counts**, which recount whenever the stock filter changes, so a chip never
  offers a category with nothing behind it.
- **Attribute filter** - one select beside the chips, shown only once a category is picked. It
  replaced an 8-section accordion, but **the facet logic is unchanged and lives in the template's
  own script**: `CATEGORY_FIELD_MAP` (the one facet per category), `coolerSupportedSockets` (parses
  a compound string like `"LGA1851/1700/1200, AM5/AM4"` into individual sockets against
  `CANONICAL_SOCKETS`), `caseTowerClassLabel`, `storageCategoryLabel`, and
  `ATTRIBUTE_VALUE_EXCLUSIONS` (Intel is excluded from the GPU brand list). Options come from the
  chosen category under the current stock filter, and a selection invalidated by a category or
  stock change resets rather than sticking.
- **Search** on brand or model, **sort** by Category, Name, Price (both directions) or Stock, and
  **pagination at 25 rows per page** with a condensed pager (first, last and a window around the
  current page) - 609 parts is 25 pages, and rendering them all wraps the footer.
- **Empty state** with a Clear filters button resetting category, stock, search and facet.

`?category=` is honoured on load, which is how the create and edit pages' Cancel buttons return to
a filtered view (`@{/computer(category='CPU Cooler')}`). Don't break it.

#### The view modal

Matches `gpt/admin/View part - centred modal@1x.png`. One shared modal populated on click, since
the rows are rendered by JS; it reads the part from `ALL_PARTS` and fetches nothing.

- **Key specs** are three tinted tiles chosen per category via `KEY_SPECS_BY_CATEGORY`. CPU pairs
  cores and threads into one synthetic tile. A designated key that is missing or empty is replaced
  by the next available field rather than rendering an empty tile; `Brand` and `Model` are excluded
  from that fallback because they are already the modal's title and subtitle.
- **Unit suffixes are display-only** - no entity or stored value changes. Most field keys already
  declare their unit (`Base Clock(Ghz)`, `Max GPU Length(mm)`, `Seq. Read(MB/s)`, `Speed(MT/s)`),
  so the unit is taken from the key, the parenthetical is stripped off the label, and `Ghz` is
  normalised to `GHz`. Only three bare-number keys need `EXPLICIT_UNITS`: PSU `Wattage` and GPU
  `Recommended PSU` take `W`, PSU `Warranty` takes `years`.
  ⚠️ **`Warranty` means two different things.** `PsuParts.warrantyYears` is an `int` and renders
  "3 years", but `StorageParts.warranty` is a **String already reading "5 years"** - it is
  deliberately absent from `EXPLICIT_UNITS`, and `withUnit` only appends to a value that is
  actually numeric. That double guard is what stops "5 years years" and "16GB GB".
- Empty and null values render as **"Not set"** in muted grey, not a blank cell.
- `Notes` and `Notable Features` render as a **paragraph block**, not a grid cell - they're prose.
- The photo area shows the image, or a labelled placeholder tile carrying the category.
- The footer's **Edit part** links to the existing `/{cat}/edit/{id}`, and **Delete** reuses the
  shared confirmation modal (which posts `_method=delete`, replacing the old inline `confirm()`).
  ⚠️ That handoff hides the view modal and opens the confirmation on its `hidden.bs.modal` event
  **deferred by a `setTimeout(..., 0)`**. Without the deferral Bootstrap's own hidden-cleanup runs
  last and strips `.modal-open` from `<body>`, leaving the page behind scrollable while the
  confirmation is open.

### Laptop parts (`/laptop`)

One table (`rnpc_laptop_parts`), one entity (`LaptopParts`), one DTO, one controller - over twelve
part types. The redesign replaced a single untyped form whose "part name" was really a type
(`LCD|Keyboard|Trackpad|Ram|SSD|M.2`) and whose "category" was a laptop type (Notebook, Gaming
Laptop, ...). Both old fields, their templates (`laptopCreateParts.html`, `laptopEditParts.html`)
and `GET/POST /laptop/create` are gone.

**`LaptopParts.PartType`** - label, URL slug and a Bean Validation group per constant, in this order
(also the order of the Add dropdowns, the chips and the Type sort):

| Type (slug) | Spec fields - **bold** = required | Compatible models |
|---|---|---|
| LCD / Screen (`lcd`) | **size** (11.6-17.3 in), **resolution**, **panel type**, **connector** (30-pin eDP, 40-pin eDP, 40-pin LVDS), refresh rate, surface, touch, mounting | required |
| Keyboard (`keyboard`) | **layout** (US, UK), backlit, color, with palmrest, with frame | required |
| Battery (`battery`) | **capacity Wh**, **voltage V**, cells, chemistry | required |
| Charger (`charger`) | **wattage**, **output voltage**, current, **connector tip**, includes cord | optional |
| RAM (`ram`) | **type**, **capacity GB**, speed MT/s | optional |
| Storage (`storage`) | **type**, **capacity GB** (1000 = 1TB), **form factor**, interface | optional |
| Casing (`casing`) | **panel** (A cover (lid), B bezel, C palmrest, D bottom), color | required |
| Hinges (`hinges`) | **side** (Left, Right, Pair) | required |
| DC Jack / Port (`dc-jack`) | **type**, tip size | required |
| Wi-Fi Card (`wifi-card`) | **standard**, **form factor**, Bluetooth version | optional |
| Touchpad (`touchpad`) | connector (text), with bracket, color | required |
| Other (`other`) | none - its forms have no specs section | optional |

There is no Fan / Heatsink or Motherboard type: the shop does not stock them - board work is a
repair service - and both were removed in batch 5. Their columns are listed for dropping under
"Database changes not in migrations".

**Common fields**, on every type: brand (dropdown; `LaptopPartsDto.BRAND_PATTERN` is the one source
for both the allow-list and the options), part name (free text, e.g. "Inspiron 15 3000 battery"),
compatible models, part number (optional; battery codes go here), **condition**
(`LaptopParts.PartCondition`: New, OEM pull, Refurbished - column `part_condition`, because
`CONDITION` is reserved in MySQL), warranty days (optional), notes, stock (`@Min(0)`), price and an
optional photo. Notes is the entity's `notes` field, mapped onto the pre-existing `description`
column (`ddl-auto` cannot rename), and bound on the forms as the DTO's `description`.

**Routes.** `GET/POST /laptop/{slug}/create` resolves the type with `PartType.fromSlug`; an unknown
slug is a 404. The type comes **only** from the path: the DTO has no `partType` field, so a posted
one is ignored. `GET /laptop/edit/{id}` and `PUT /laptop/update/{id}` keep their id-only URLs and
dispatch on the **stored** type, so an edit can never change it. `FORM_TEMPLATES` in the controller
maps each type to its template prefix: `products/laptop/<prefix>Create.html` / `<prefix>Edit.html`,
24 templates, all on `fragments/parts-form.html`. They are deliberately identical in shape (Part
details, "<Type> specs", Stock and price, Notes, Photo) - when changing one, change them all.

**Validation** runs through an injected `SmartValidator`, not `@Valid`, because the group depends on
the type: `validate(dto, result, Default.class, type.getGroup())`. `Default` holds the shared rules
(brand, lengths, stock, price); every type group in `PartType.Groups` extends `Typed`, which makes
condition required. String dropdowns have exact `@Pattern` allow-lists that mirror the templates'
options (with an empty alternative for optional ones); numeric dropdowns are `@Positive` instead.
Sanity ranges, both ends inclusive: battery capacity 10-150 Wh, battery voltage 3-20 V, charger
output voltage 5-48 V, charger current 0.5-10 A.

**Service.** `LaptopPartsService` stores blank text as null and, before every save, nulls each spec
field that belongs to another type - a tampered or stale form can never leave another type's specs
on a part.

**`LaptopPartView`** builds everything the list page and View modal show, in Java: the type label
and tile code, the Key spec line (e.g. `15.6" FHD IPS, 30-pin eDP`, `42 Wh, 11.4 V`,
`DDR4, 8GB, 3200 MT/s`; blank parts skipped, "Not set" when empty), the spec rows with units and
Yes/No, and three key-spec tiles. Tiles are the type's first three spec rows unless `TILE_ROWS` picks
others (LCD: Size, Resolution, Connector; Charger: Wattage, Output voltage, Connector tip; Keyboard:
Layout, Backlit, With palmrest). Types with fewer than three specs are padded with Condition,
Warranty, Part number. A spec shown as a tile is not repeated in the "<Type> specs" list, and the
modal hides that list when it ends up empty.

**List page** (`products/laptopParts.html`, styled from `parts.css`) works like `/computer`: the
controller serializes `LaptopPartView` rows into `ALL_PARTS` and the page filters, sorts and pages
them client-side. Stock cards (`LOW_STOCK_LIMIT` reused), one chip per type in enum order with its
count (shown at 0, recounted by the stock filter), search over brand, part name, part number and
compatible models, the same sort options as `/computer`, 25 rows a page, a Clear filters empty
state, and an Add dropdown in both the header and the no-parts empty state.

**A null `part_type` should not exist** - the untyped form is gone and the column is `NOT NULL`
once the hand-run SQL is applied. If one appears anyway it is treated as Other everywhere
(`LaptopPartView.typeOf`, `LaptopPartsController.storedType`): listed and viewed as Other, edited
with the Other form, and saved back as `OTHER`. Nothing throws.

Tests (context-free, run by name): `LaptopPartsDtoValidationTest`, `LaptopPartViewTest`,
`LaptopPartsServiceTest`.

### Feature slices

- **PC builder** - `BuildController` (`/build`), `SavedBuildController` (`/build/my-builds`, with
  duplicate/load/delete). Build slots are `CPU`, `GPU`, `MOTHERBOARD`, `RAM`, `STORAGE_SSD`,
  `STORAGE_HDD`, `PSU`, `CASE`, `COOLER`. The seven *required* slots are defined once in
  `OrderService.REQUIRED_BUILD_SLOTS` (GPU and the second storage slot are optional); reuse
  `OrderService.countFilledBuildSlots` / `hasAllBuildCategories` / `isFullBuild` rather than
  re-listing categories.
- **Orders** - `OrderController` (`/order`). Checkout re-reads brand/model/price from the live
  catalog in `OrderService.resolveSelections`, so a tampered client-side price can never be
  persisted. Xendit is **out of the flow**: a checkout submit is the customer's payment claim
  (bank + reference number + optional receipt image), and an admin verifies it manually and marks
  the order PAID. `XenditService` exists but nothing calls it.
- **Appointments** - `AppointmentController` (`/appointment`), with a customer view, an admin view
  and a calendar. `Appointment.ServiceType` (`DESKTOP_REPAIR`, `LAPTOP_REPAIR`, `CELLPHONE_REPAIR`,
  `PC_LAPTOP_CLEANING`, `GENERAL_CONSULTATION`) is the app's richest service taxonomy, and
  `DeviceCategory` refines the device. Appointments have a payment flow (method, reference,
  receipt, verification, refund status) but **no amount field**, so they carry no revenue.
- **Repairs** - `RepairRecordController` (`/repair`). `RepairRecord.deviceType` is an allow-list of
  `Cellphone|Laptop|Desktop`; `cost` is the revenue. A repair created from an appointment is hidden
  from Repair History until that appointment is CONFIRMED - see `RepairRecord.isVisible()`, which
  `RepairRecordService.getAllRepairRecords` filters on.
- **Support tickets** - `SupportController`, `SupportTicketService`, and three entities. This slice
  does *not* follow the per-category quintet pattern; it's one controller over three tables. See
  its own section below.
- **Notifications** - `NotificationController` (`/notification`), plus a topbar dropdown.
  `Notification.EntityType` is `ORDER`, `REPAIR`, `APPOINTMENT`, `SUPPORT_TICKET`; each maps to a
  `/{thing}/{id}/modal` fragment endpoint that the UI fetches into a shared modal. **Adding an
  `EntityType` means adding its URL to two JS maps** - in `fragments/layout-app.html` and in
  `notifications/notificationIndex.html`.
- **Search** - `SearchController` (`/search`, `/search/suggest`), signed-in customers only (admins
  are redirected). `SearchService` filters only the four already-per-user lists (orders, repairs,
  appointments, saved builds) in memory; it never calls a get-all method or a repository directly.
- **Sales report** - `SalesReportController` (`/sales`), admin-only. `SalesReportService` makes one
  pass over all orders and repairs and buckets everything by day. Revenue counts only PAID orders
  (on `verifiedAt`, falling back to `createdAt`) and COMPLETED/RELEASED repairs (on `repairDate`).
  It also derives a per-service breakdown (`SalesServiceRow`: a full build is one `Custom PC Build`
  line, a parts-only order is one line per item category, a repair is one line per device type) and
  the outstanding balances that are deliberately *not* revenue (AWAITING_PAYMENT orders owed to the
  shop, PENDING refunds owed by it). See its own section below.
- **Dashboards** - `DashboardController` owns `/`: anonymous visitors get
  `forward:/index.html` (the static landing page), admins are redirected to `/admin/dashboard`
  (`AdminDashboardController`), and customers get `dashboard.html`. The customer Build Progress
  card has exactly three states resolved server-side into one `buildProgressState` attribute -
  `ACTIVE_ORDER` (a PAID order with a live build stage), else `SAVED_BUILD`, else `EMPTY` - so the
  template branches once with `th:switch`.
- **Clients** - `ClientController` (`/client`). A `Client` is the billing/contact record; a `User`
  is the sign-in account. They're linked via `ClientService.linkToUser` at checkout, which is how a
  customer's own orders become visible to them.
- **Legacy tickets** - `TicketController` (`/ticket`) is a separate, older print-a-ticket feature,
  unrelated to the support tickets above.

### Support tickets

`SupportController` is not annotated with a class-level `@RequestMapping`; every route carries its
full path, and customer and admin routes live side by side in the one class:

| Route | Who | Notes |
|---|---|---|
| `GET /support` | customer | Form + their own tickets. Signed-out → `redirect:/login`; **an admin is redirected to `/admin/support`**. `?all=true` shows every ticket instead of the latest 5 |
| `POST /support` | customer | An admin hitting this gets `403` - staff must not file tickets |
| `GET /admin/support` | admin | Full inbox. Non-admin → `redirect:/` |
| `GET /support/tickets/{id}` | either | `SupportTicketService.accessible(...)` scopes it - a customer can only open their own, an admin any |
| `GET /support/tickets/{id}/modal` | either | Returns `support/detail :: ticketDetail`, the same fragment the full page uses, for the notification modal |
| `POST /support/tickets/{id}/reply` | admin | `403` for anyone else |
| `GET /support/tickets/{ticketId}/attachments/{id}` | either | Streams the blob with `Content-Disposition: attachment`, `nosniff`, `no-store` |

Unlike most controllers here, the guards throw `ResponseStatusException` (401/403) rather than
redirecting - `requireSignedIn(...)`, plus `admin(...)` checks per route.

Three entities, all created by `ddl-auto=update` on first run:

- **`SupportTicket`** → `rnpc_support_tickets`, with index `idx_support_ticket_user` on `user_id`.
  `Long id`; `@ManyToOne User user`; `Topic topic` and `Status status` as `VARCHAR(32)`;
  `String message` as `TEXT`; `LocalDateTime createdAt` (not null) / `updatedAt` (nullable);
  `@Version Long version`. `getReference()` derives `SUP-%05d` from the id - it is **not** a
  column. `Topic` = ORDER, REPAIR, APPOINTMENT, WARRANTY, GENERAL; `Status` = OPEN, IN_PROGRESS,
  ANSWERED, CLOSED, each with a display label.
- **`SupportReply`** → `rnpc_support_replies`. `Long id`; `@ManyToOne SupportTicket ticket`;
  `@ManyToOne User author`; `String message` as `TEXT`; `LocalDateTime createdAt`. Replies are
  admin-only, so `author` is always a staff account.
- **`SupportAttachment`** → `rnpc_support_attachments`. `Long id`; `@ManyToOne SupportTicket
  ticket`; `filename`, `contentType`, `long size`; and `byte[] data` as
  `@Lob @Basic(fetch = LAZY) @Column(columnDefinition = "LONGBLOB")`. **Attachment bytes live in
  the database**, unlike every other upload in this app, which goes to `public/images/`.
  `SupportAttachmentRepository.Summary` is a projection (`id`, `filename`, `size`) so listing a
  ticket's attachments never loads the blobs - use it rather than fetching the entity.

`SupportTicketService` holds the rules: at most 5 files, 10 MB each; **magic-byte sniffing**
(`detectType`) accepts only PNG, JPEG and PDF, so the declared content type is never trusted;
filenames are stripped of paths and control characters and capped at 200 chars. `respond(...)`
compares `dto.getVersion()` against the ticket's `@Version` and rejects a stale edit, and refuses
to set ANSWERED without a reply body. Creating a ticket calls `notifyAdmin`, replying calls
`notifyCustomer`, both with `Notification.EntityType.SUPPORT_TICKET`.

`MaxUploadSizeExceededException` is handled inside the controller, flashing `uploadError` and
redirecting to `/support` - this is why `max-request-size` is 51MB while `max-file-size` is 10MB.

### Sales report

The page (`sales/salesReport.html`) is converted to `layout-app`, and its styling lives in
`static/css/sales-report.css` - it used to be a standalone Bootstrap page with ~700 lines of inline
`<style>`.

`SalesReportService` makes **one pass** over all orders and all repairs and buckets everything by
day into `SalesDayRow`. Beyond revenue it derives two things from that same pass, with no extra
query and no new entity:

- **Per-service breakdown** (`SalesServiceRow`: `name`, `source`, `revenue`, `count`, carried as a
  list on each day row). There is no service catalog in this app, so a "service" is derived: a
  **full build order is one line** (`Custom PC Build`, using `OrderService.isFullBuild`), a
  **parts-only order is one line per `OrderItem` category**, and a **repair is one line per
  `deviceType`**. **Invariant: service lines sum to exactly the same total as Orders Revenue +
  Repairs Revenue.** A build counted as both one build *and* eight components would break the card
  silently - keep that invariant if you extend this.
- **Outstanding balances**, which are deliberately *not* revenue: `unpaidRev`/`unpaidCount` for
  AWAITING_PAYMENT orders (owed to the shop, bucketed on `createdAt`) and `refundRev`/`refundCount`
  for cancelled orders whose `refundStatus` is still PENDING (owed by the shop, bucketed on
  `verifiedAt` since there is no `cancelledAt` column).

All of this lives on **`SalesDayRow`, not `SalesMonthRow`**, because the page's `filteredRows()`
re-aggregates from day rows for both the yearly and monthly views - one source of truth, so a month
can never disagree with the days inside it.

An order in `CANCELLATION_REQUESTED` is currently counted as neither revenue nor outstanding.

The page filters client-side over the whole payload; move it server-side once volume grows.

### Entities and tables

Almost every table is prefixed `rnpc_` (`rnpc_orders`, `rnpc_order_items`, `rnpc_repair_records`,
`rnpc_appointments`, `rnpc_clients`, `rnpc_notifications`, `rnpc_saved_builds`,
`rnpc_saved_build_items`, `rnpc_support_tickets`, `rnpc_support_replies`,
`rnpc_support_attachments`, and one `rnpc_*_parts` table per category). The exception is `User`,
which maps to plain `users`, with a `Role` enum (`ADMIN`, `CUSTOMER`) stored via
`@Enumerated(EnumType.STRING)`.

`User.getEmployeeId()` returns the Google subject id, falling back to the internal `User.id` for an
account that has never signed in with Google. That's what `Order.verifiedByEmployeeId` and the
appointment equivalents record against; there is no separate HR/employee table.

## Templates and the design system

`fragments/layout-app.html` is the current shared shell, converted from an approved mockup. It
provides the `assets`, `scripts`, `sidebar(active, currentUsername, currentRole)`,
`topbar(currentUsername, currentRole, unreadNotifications, recentNotifications)`, `notifDetailModal`,
`app-styles` and `app-script` fragments. The sidebar renders a customer or staff menu off
`${navIsAdmin}`. Supporting fragments: `page-header`, `stat-card`, `appointment-ui`.

The brand mark is the image at `static/img/rn-pc-logo.png` (it used to be the letters "RN"), and
the shop name reads "RN PC", not "RNPC". The topbar avatar shows the uploaded profile photo when
`${hasProfilePhoto}` is true and otherwise the first letter of the username - see the
`hasProfilePhoto` caveat under Profile photos.

Current nav layout, which is not symmetric between the two menus:

- **Customer:** Dashboard, Build a PC (accordion: Build a PC / My Builds), Orders, Appointment
  (accordion), Repairs, **Contact Support** (`/support`), My Profile.
- **Admin:** Dashboard, then Operations (Orders, Appointments accordion, Repairs, Tickets
  accordion), Inventory (Parts accordion, PC Builder), Customers, Reports (Sales Report), System
  (**My Profile**).
- **Support Tickets** (`/admin/support`) sits inside the admin **Tickets** accordion, alongside the
  legacy New Ticket / View Tickets entries - so that accordion mixes the support system and the
  legacy repair-intake system.
- **Notifications was removed from both sidebars.** It is still reachable at `/notification` via
  the topbar bell dropdown's "View All", and `notifications/notificationIndex.html` is still an
  unconverted Bootstrap page. Don't assume a missing sidebar entry means a missing route.

**The migration to it is well past half done.** Converted: `dashboard.html`, `admin/dashboard.html`,
all five `appointments/`, both `orders/` index pages, `build/`, `repairs/repairIndex.html`,
`sales/salesReport.html`, `search/searchResults.html`, `profile/profileEdit.html`, all three
`support/`, and **42 of the 53** `products/` templates (below). Still unconverted: the other 11
`products/` pages, `orders/orderCheckout.html` and `orderConfirmation.html`, the three other
`repairs/` pages, all three `clients/`, both `tickets/`, `login/login.html`, and
`notifications/notificationIndex.html`.
When converting a page, follow one that's already done rather than inventing a new structure.

`products/` holds 53 templates: 29 at the top level and 24 in `products/laptop/`.

- **Converted (42):** `allParts.html`; the 16 create and edit forms of the eight PC categories
  (`cpuCreateParts.html` / `cpuEditParts.html` and so on, all on `fragments/parts-form.html`, CPU
  being the reference pair); `laptopParts.html`; and the 24 per-type laptop forms in
  `products/laptop/`.
- **Still old Bootstrap (11):** the eight PC list pages (`cpuParts.html` at `/cpu`, `gpuParts.html`
  at `/gpu`, and so on) and `cellphoneParts.html` / `cellphoneCreateParts.html`, all on the old
  `fragments/nav.html` shell; and `cellphoneEditParts.html`, which has **no shell at all** - no
  sidebar, no topbar, just a centered Bootstrap form.

Page-specific CSS goes in `static/css/<page>.css` (see `profile.css`, `sales-report.css`,
`support.css`, `parts.css`); shared tokens live in `theme.css`. **No inline `style` attributes** -
the converted pages use modifier classes, and SVG/`progress` attributes for genuinely dynamic bars.

`/order` is one controller method returning two templates: `orders/orderIndex.html` for customers
and `orders/adminOrderIndex.html` for admins. They share the shell, stat-card row, search box and
table structure but differ in stat semantics (Total Spent vs Revenue), columns (the admin adds
Customer and Verified at) and actions (the admin gets Mark Paid, Update Stage, Mark Refunded,
Approve/Deny Cancellation). Keep both in sync when changing shared behaviour.

### Thymeleaf gotchas already hit here

- **Avoid `T(...)` static calls in templates.** They caused a stale-class failure on the admin order
  page. Precompute in the controller and pass a map instead - that's why `buildStageLabels` and
  `stageOptionsByOrder` exist.
- `th:if` does **not** gate a `th:replace` on the same element - wrap it in a `th:block`.
- Keep a whole ternary inside one `${...}` and avoid escaped quotes in a branch; both have caused
  SpEL parse errors here.
- `card.html` / `table-card.html` / `empty-state.html` take content via cross-file `~{::#id}`
  fragment expressions, which is why several converted pages reproduce their classes directly
  instead of calling them.

## Uploads and static resources

Product, repair, order and appointment images are written by the services directly (no storage
abstraction) to `public/images/`, resolved **relative to the process working directory** - i.e. the
project root, not `src/main/resources/static`. Filenames are timestamp-prefixed
(`System.currentTimeMillis() + "_" + originalFilename`). `WebConfig.addResourceHandlers` maps
`/images/**` to `file:public/images/`, which is what makes them load in the browser.

Two deliberate exceptions: support-ticket attachments live in the database (above), and profile
photos, below.

`spring.servlet.multipart.max-file-size=10MB`, `max-request-size=51MB` (five support attachments
plus overhead).

### Profile photos

`ProfilePhotoService` stores **exactly one PNG per user**, named after the account id:
`./private/profile-photos/<userId>.png`. The directory is gitignored (`/private/profile-photos/`)
and the path is overridable with `app.profile-photo-directory`, which is what you'd point at a
mounted volume for a multi-instance deploy - the default is relative to the working directory, so
it is **not** shared between instances.

The upload is never stored as received. `save(...)` rejects anything over 5 MB, decodes it with
`ImageIO`, requires the detected format to be `png` or `jpeg` (the declared content type is not
trusted), rejects images over 16 million pixels, then **re-encodes to PNG** and writes via a temp
file plus an atomic move. That re-encode is the point: the original filename and all EXIF are
discarded, so nothing the user supplied is ever served back.

Reading goes through `GET /profile/photo` on `ProfileController`, which serves the signed-in user's
own photo as `image/png` with `Cache-Control: no-store` and `X-Content-Type-Options: nosniff`.
There is no route to fetch another account's photo.

`ProfileController` also answers `GET /profile` as an alias for `/profile/edit`.

⚠️ **`hasProfilePhoto` is only set by `ProfileController`** (in its private `profileModel(...)`).
`layout-app`'s topbar avatar reads that flag, so the photo appears **only on the profile page** -
every other page falls back to the first letter of the username. Any controller that renders the
shared shell and wants the avatar has to set it too.

## Known dead code

- `UserService.getRedirectUrl` hardcodes `http://localhost:9090/...` and has **no callers**.
- `XenditService`, `Order.paymentSessionId` / `paymentLinkUrl` and `XenditSessionResponse` are
  leftovers from the removed payment-gateway flow.
- `CustomOAuth2UserService` never fires while Google is the only provider.
- The `mssql-jdbc` dependency in `pom.xml` is unused.
- `topbar`'s avatar reads `${hasProfilePhoto}`, but only `ProfileController` sets it - on every
  other page the avatar falls back to the initial letter.

## Root-level `.txt` files

`Controller.txt`, `Dto.txt`, `Models.txt`, `create.txt`, `edit.txt`, `index.txt`, `repository.txt`
at the repo root are working/reference notes, not part of the build (no `src/` file depends on
them). `gpt/` holds design mockups the recent UI work is built from - also not part of the build.
