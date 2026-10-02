# RNPC-Inventory-and-KYC — handoff

Everything a new chat needs to pick this up. Written 2 October 2026.

---

## 1. The project

**Repo:** github.com/SpectreSpammer/RNPC-Inventory-and-KYC, branch `master`
**Stack:** Spring Boot 3.3.11, Java 21, Thymeleaf, MySQL/MariaDB
**Local:** XAMPP MariaDB 10.4.32, database `rnpc`, app on port 9090, IntelliJ
**Production:** Railway, port 8080, rnpc-inventory-and-kyc-production.up.railway.app
**Production status:** PAUSED. The Railway trial ran out. Deliberate decision to leave it
paused and keep building locally, since nobody but the owner uses that URL.

It is a repair shop management system for RN PC Repair Shop, an appointment-only
PC and cellphone repair business. Admin side: parts inventory, customers, walk-in
tickets, repairs, orders, appointments, sales report, support tickets. Customer side:
dashboard, appointments, shop and checkout, repair tracking, support.

**Project docs:** `CLAUDE.md` is the source of truth. `AGENTS.md` is a copy of it that
differs only in the title and the tool name on line 3, and is re-synced on every change.
Both are kept current as part of each batch, never left to the end.

---

## 2. How we work together

The owner runs **Claude Code** (Sonnet, for the heavy code batches) inside IntelliJ.
This chat (Opus, for planning and review) does:

- read-only analysis prompts before any code is written
- deciding the batch plan
- writing the prompt for each batch
- reviewing what Claude Code reports back
- writing the test checklist after each batch
- writing the commit prompt
- producing designs

**The loop, every time:**

1. I write a prompt → owner pastes it into Claude Code
2. Claude Code reports back → owner pastes the report here
3. I review it, flag anything worth knowing, and write a test checklist
4. Owner tests in the browser and reports results
5. I write the commit prompt
6. Push when a few commits have built up

This has worked well. Keep it. The owner tests everything manually in the browser and
reports honestly, including partial failures, which is why several real bugs got caught.

### Designs

There is **no design system artifact** set up. Designs are made as Design canvases in
this chat, then the owner downloads the artboard PNGs into `gpt/admin/` in the repo, and
Claude Code reads the PNGs directly. That works better than giving Claude Code a canvas link.

Earlier mockups in `gpt/admin/` came from a different tool (ChatGPT), not from this chat.

Canvases made so far:
- Parts inventory + Add forms — https://claude.ai/artifact/8ZAMRdBq47qLnvmcg8m64n
- Public landing page + sign-in — https://claude.ai/artifact/169dDma1Gac3yKritLtU7i
- Remaining admin pages (Laptop, Clients, Tickets) — https://claude.ai/artifact/F5HEHwSvxvBs9SVuZti7Pm
- Ticket created page — https://claude.ai/artifact/8NhqFeiXf8ya7MDCR9U1zF

The app's visual language, for any new design: navy sidebar `#0f1b2f`, page background
`#f4f6f9`, white cards with `#e4e9f0` borders and ~12px radius, cyan accent (use `#0b7d9f`
or darker so white text passes 4.5:1), heading text `#16202e`, secondary `#5b6878`.
Plus Jakarta Sans. Status pills: green in stock, amber low stock, amber pending.

---

## 3. Standing rules in every Claude Code prompt

These go at the end of nearly every prompt and exist because each one caused a problem once:

```
Don't use shell heredocs with regex backreferences.
UTF-8 no BOM. Do not run the app. Do not touch the database.
Don't commit. Stop and list changed files.
```

Plus, scoped per batch: *Don't touch the parts pages, laptop, cellphone or /computer.*

**Commit prompts always say "stage by explicit path"** and list the files. Never
`git add .` or `git add -A`. Push prompts always say *never force-push* and *if anything
is uncommitted, stop and tell me*.

---

## 4. Gotchas that have actually bitten

- **`pom.xml` has no explicit `project.build.sourceEncoding`.** A literal non-ASCII
  character in a `.java` file is not guaranteed to survive `javac` under the platform
  default. This caused trouble three times. The workaround used is building characters
  from code points (`Character.toChars(0x00E1)`). **Setting the encoding in `pom.xml`
  would fix the root cause and has not been done yet.**
- **`ddl-auto=update`** adds columns automatically but cannot rename, change types or
  drop. Every destructive schema change is written as by-hand SQL recorded in CLAUDE.md
  under "Database changes not in migrations" and run manually in phpMyAdmin and Railway.
- **Railway ordering.** Run "after deploy" SQL only once the deployment shows ACTIVE, not
  when the push goes out. The old build keeps serving during the build and breaks if its
  columns are dropped early. This caught us once with laptop.
- **Thymeleaf:** no `T(...)` in templates, no `\'` inside `${}`, never `th:if` and
  `th:replace` on the same element.
- **Stop the app before `mvn clean compile`.**
- **Branch protection on `master` is bypassed on purpose.** Solo repo, so the PR, signing
  and Code Scanning requirements don't apply. Noted in CLAUDE.md; Claude Code no longer
  reports the warning. **The ruleset should be deleted on GitHub** — still outstanding.
- **MariaDB crashed once** after a forced Windows restart (Aria log corruption). Recovered
  with `aria_chk -o --sort_buffer_size=256M ..\data\mysql\*.MAI` plus deleting
  `aria_log.*` and `aria_log_control`. **There is still no backup routine for the local
  database.** Worth a weekly phpMyAdmin export outside `C:\xampp`.

---

## 5. What has been completed

### Parts
- **Security:** admin-only rule on all parts routes (`ADMIN_ONLY_PARTS_PATHS`). Anonymous
  → `/login`, non-admin → `/`, scripted requests (`X-Requested-With`) → 401/403.
- **`/computer` converted** to layout-app, and all 20 PC part create/edit forms moved onto
  the shared `fragments/parts-form.html` + `parts-form.css`.
- **Laptop parts redesign, 7 batches.** 12 part types (LCD/Screen, Keyboard, Battery,
  Charger, RAM, Storage, Casing, Hinges, DC Jack/Port, Wi-Fi Card, Touchpad, Other), each
  with its own create/edit form and spec fields; type-driven list page with chips, search,
  sort and a view modal; the old untyped model retired. Hand-run SQL applied to both
  databases. Complete and deployed.
- **Cellphone parts redesign, 6 batches.** Same shape, 10 types (LCD/Screen, Battery,
  Charging board, Back glass, Housing/frame, Flex cable, Camera, Fingerprint, Sensor,
  Other). Note **SCREEN's slug is `lcd`**, so the route is `/cellphone/lcd/create`. SQL
  applied to both databases. Complete.
- **Back-navigation fix.** Editing a part and returning now lands on the type chip you
  were on, copying `/computer`'s existing `?category=` mechanism as `?type=`. The chip key
  was changed from the enum name to the slug so the URL and the chips use one identifier.

### Customers and tickets
- **Batch 1 — security.** `/client/**`, `/ticket/create` and the numeric `/ticket/{id}`
  slip routes are admin-only. `/ticket/view` stays open: it is the customer's own repair
  history. Slip routes matched on `{id:[0-9]+}` so the two don't collide. This was a real
  exposure: client names, numbers, emails and addresses were readable signed out.
- **Batch 2 + 2b — contact numbers.** One normaliser (`util/PhoneNumbers`). Stored and
  displayed as `0917 123 4567`. Matched on digits alone, with `+63` read as `0`. Duplicate
  numbers rejected, naming the existing client. The five copied DTO patterns collapsed to
  one. Lookups return the oldest match instead of throwing. A shared
  `static/js/phone-format.js` formats the field as you type, caret-safe, on all six forms
  that take a number. Existing rows reformatted by hand-run SQL.
- **Batch 3 — customers list.** `/client` converted to layout-app with `ClientView`,
  search over name/number/email/job order (digit-matched), four sorts, a 25-per-page
  pager, job order chips capped at two plus "+N more", and the eye action going to
  `/repair?clientId={id}`. Built from one repairs query rather than two per client.
- **Batch 4 — add and edit client forms.** Moved onto `parts-form.html`. Email and address
  made optional and stored as null when blank. Photo validation added: max 5 MB, bytes must
  decode via ImageIO as PNG or JPEG, max 16 million pixels. Two blank-row display gaps
  fixed in `orderConfirmation.html` and `appointmentView.html`.
- **Batch 5 — walk-in ticket form.** Moved onto `parts-form.html` with a `.pf-split` grid
  variant. Email, address and brand made optional. **When a contact number matches an
  existing client, blank fields on that client are filled in from what was typed, but an
  existing value is never overwritten.**
- **Ticket created page redesign.** `ticketPrint.html` moved onto layout-app following the
  approved design. Added a `receivedBy` field to `RepairRecord`. The slip now reads
  "Not recorded" for a blank serial and "To be assessed" when there is no estimate, instead
  of "Php 0.0" which read as free. Print isolation rebuilt against layout-app's markup.

### Cross-cutting fixes
- **Topbar avatar.** `hasProfilePhoto` was only set by `ProfileController`, so every other
  page fell back to the initial letter. Fixed in `GlobalNavAttributes`, a `@ControllerAdvice`
  that already runs on every request.
- **Avatar sizing.** The photo rendered at full resolution and covered the page. Fixed in
  the shared `app-styles` fragment with percentage sizing and `object-fit: cover`, and the
  duplicate rule removed from `profile.css`.
- **303 redirect fix.** `removePhoto` returned a 302, and browsers follow a 302 for a
  DELETE *as a DELETE*, hitting a GET-only edit route and getting 405. Every removePhoto
  showed a false "could not be removed" error even though the photo was deleted. Fixed in
  **twelve** controllers with a `util/Redirects` helper returning 303 See Other. The
  confirm wording now says the removal is immediate, and the success path removes the
  preview without a reload.

---

## 6. Where things stand right now

**Uncommitted:** the ticket-created page redesign. Commit message agreed:

```
feat(tickets): redesign the ticket created page
```

Body should note the layout-app conversion, the new `receivedBy` field on `RepairRecord`,
the slip's "Not recorded" and "To be assessed" changes, the rebuilt print isolation, and
that the slip fragment still renders standalone for the repair list's print modal.
Stage the two design PNGs in `gpt/admin/` too.

**Not yet verified:** the print preview on the redesigned page. That is the part that
cannot be checked by reading code and the part that reaches customers, so it must be
tested before committing. Also check `SHOW COLUMNS FROM rnpc_repair_records LIKE
'received_by';` after restarting, and that an older repair with no `receivedBy` doesn't
leave a blank row.

---

## 7. What is left

### Next up: the repair pages
`/repair` list, detail and edit are all still on the old design. The owner hit this
clicking "Open the repair record" from the new ticket page. There is **no approved design
yet** — one should be made in the new chat, the same way the ticket page was.

Start with this read-only analysis prompt:

```
Read-only analysis for converting the repair pages. Don't
change anything.

There is no approved design for these. Don't invent one in
this analysis.

REPORT:
1. Every repair page, route and template today. For each:
   the controller method, the template, which shell it uses,
   and what it does.
2. The RepairRecord entity and its DTO: every field, every
   validation rule and allow-list, and where dropdown options
   come from. Note the receivedBy field just added.
3. How repairs relate to clients, tickets, appointments and
   orders. Which flows create a repair record and how they
   differ.
4. Everything else that reads repair records: the admin
   dashboard, the customer dashboard, notifications, the
   sales report, the client list's job order chips, search.
   What breaks if fields change.
5. The status values, where they are defined, and every
   place status is read or set. The customer-visible wording
   too.
6. Which parts of the parts and customers work carries over,
   and where it does not fit. Say plainly whether repairs
   have a natural equivalent of type chips, given they have a
   status.
7. What the list should filter and sort on.
8. A batch plan in the same style as the earlier work.

Don't change any files.
```

### Then
- **Customers batch 6:** deleting a client with orders or appointments still fails with a
  foreign key error. The delete modal also warns about repair records but not orders or
  appointments.
- **Customers batch 7:** the dashboard's "View Tickets" link sends `?status=PENDING` and
  `/ticket/view` ignores it.
- **Landing page and sign-in.** Designs already exist in
  `gpt/Landingpage/claude-redesign/`: landing page desktop, landing page home, sign in,
  and sign in error state.
- **Remaining old pages:** orders checkout and confirmation, appointment pages (converted
  by Codex earlier but never verified), notifications, login.

### Smaller open items
- **Profile photos are not resized on upload.** `ProfilePhotoService` accepts up to 16
  million pixels. The CSS now contains them visually, but full-resolution images are stored
  and sent on every page load. Worth resizing on upload, as the sidebar logo was.
- **`pom.xml` source encoding** — see gotchas.
- **Delete the branch protection ruleset** on GitHub.
- **A backup routine for the local database.**
- **`show-sql=true` in production** makes the Railway logs very noisy.
- **No volume on the Railway app service** — part and profile photos are wiped on redeploy.
- **An open question never settled:** `@Email` accepts `dorry@gmai`, because `gmai` is a
  legal hostname. `jahul` with no `@` is correctly rejected. Whether to require a dot in
  the domain is the owner's call. Recommendation was to tighten it, since it catches typos
  like `@gmai` and the shop will never have an intranet address as a customer.

---

## 8. The VA test sheet

A test checklist workbook was made for the owner's virtual assistant:
**`RNPC-Test-Checklist.xlsx`**, three sheets (Admin 84 cases, User 38, Landing Page 27).
Each sheet has two orange worked examples under the header, one passed and one failed,
showing the level of detail wanted. The VA fills Status, Actual result, Severity,
Screenshot, Remarks, Tested on and Date. Status and Severity are dropdowns, the Status
cell colours itself, and counters at the top of each sheet tally progress.

Screenshots are pasted into the cell with Insert → Pictures → Place in Cell, which needs
Microsoft 365 or Excel for the web — worth confirming what the VA has before sending.

**Not yet sent.** Worth sending so he can test what already exists while the rest is built.

---

## 9. Tone and preferences

Reply in English even when the owner writes in Tagalog or Taglish; don't mirror the
language. He tests thoroughly and reports honestly, so take failure reports at face value
and investigate rather than assuming he mis-tested. He prefers being asked before a
judgement call is made on his behalf — several prompts deliberately include "tell me before
changing it, I want to decide". That has caught real design decisions that would otherwise
have been guessed.
