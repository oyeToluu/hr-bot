# HR Bot — Backend Code Drop-In

This isn't a full project — it's the `com/hrbotproject/hr_bot/*` subpackages meant
to merge into the `hr-bot` project you already created in VS Code (the one with
`pom.xml`, `compose.yaml`, and `HrBotApplication.java`).

## How to merge

1. Copy everything under `src/main/java/com/hrbotproject/hr_bot/` from this zip
   into your existing project's same path, **except** don't overwrite
   `HrBotApplication.java` — leave your existing one as-is.
2. **pom.xml needs two blocks added** — see `pom-reference.xml` in this zip,
   marked `ADDED for JUnit test work` and `ADDED for JWT auth`. Summary:
   - `spring-boot-starter-security` (main) + `spring-boot-starter-security-test` (test)
   - `jjwt-api`, `jjwt-impl` (runtime), `jjwt-jackson` (runtime) — version 0.13.0
   - `junit-jupiter`, `mockito-junit-jupiter`, `assertj-core` (test)
3. **application.properties needs 2 lines added** — see
   `application-properties-additions.txt` in this zip. This sets `app.jwt.secret`
   (must be 32+ bytes for HS256) and `app.jwt.expiration-ms`.

Resulting structure inside your project:
```
src/main/java/com/hrbotproject/hr_bot/
├── HrBotApplication.java        (yours, unchanged)
├── model/                       Employee, LeaveRequest, OnboardingFaq, PolicyDocument
├── repository/                  Spring Data MongoDB repositories
├── service/                     business logic for all 5 MVP features
├── controller/                  REST endpoints (incl. AuthController)
├── security/                    JWT auth: filter, service, UserDetails, SecurityConfig
├── dto/                         request/response records
├── config/                      Mongo auditing
├── exception/                   global error handler
└── data/                        demo data seeder (runs on startup)
```

## Run it

```bash
./mvnw spring-boot:run
```

Because `spring-boot-docker-compose` is on your classpath, Spring Boot detects
`compose.yaml`, starts the Mongo container, and auto-wires the connection —
no manual config needed. On first startup, `DemoDataSeeder` populates:
- 2 employees, both password `password123`:
  `admin@hrbots.test` (HR_ADMIN), `ada@hrbots.test` (EMPLOYEE)
- 4 onboarding FAQs
- 1 policy document ("Employee Handbook 2026", 3 sections)

## Authentication

Every `/api/**` route except `/api/auth/**` now requires
`Authorization: Bearer <token>`. Get a token:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ada@hrbots.test","password":"password123"}'
```

Response: `{ "token": "eyJ...", "employee": { ...no password field... } }`.
Use that token on everything else:

```bash
curl http://localhost:8080/api/leave/mine \
  -H "Authorization: Bearer eyJ..."
```

New employees sign up via `/api/auth/register` (always creates role
`EMPLOYEE` — there's no way to self-assign `HR_ADMIN` through the API,
by design).

### What changed from the earlier no-auth version

The whole point of adding auth is that endpoints stop trusting whatever
`employeeId` the client claims to be — they now read it off the verified
JWT instead. That meant real signature changes, not just decoration:

| Endpoint | Before | Now |
|---|---|---|
| `POST /api/leave` | body had `employeeId` | employeeId comes from the token; body no longer has that field |
| `PATCH /api/leave/{id}/decision` | body had `reviewerEmployeeId` | reviewer comes from the token; body is just `{decision, note?}` |
| `GET /api/leave/mine` | `?employeeId=...` query param | no param — always "my own" |
| `PATCH /api/onboarding/complete` | `?employeeId=...` query param | no param |
| `GET /api/dashboard/me` | `?employeeId=...` query param | no param |
| `POST /api/policy/documents` | trusted `uploadedByEmployeeId` in body | overwritten server-side from the token |
| `POST /api/employees` | created an employee (no password) | **removed** — use `/api/auth/register` instead, since that's the only path that sets a password |

## API Reference

### Auth
| Method | Route | Access | Notes |
|---|---|---|---|
| POST | `/api/auth/register` | Public | `{ fullName, email, password, department? }` → always role EMPLOYEE |
| POST | `/api/auth/login` | Public | `{ email, password }` → `{ token, employee }` |
| GET | `/api/auth/me` | Authed | current employee, from the token |

### Employees (HR only)
| Method | Route | Notes |
|---|---|---|
| GET | `/api/employees/{id}` | |
| GET | `/api/employees` | list all |

### Leave Request Bot
| Method | Route | Access | Notes |
|---|---|---|---|
| POST | `/api/leave` | Authed | `{ leaveType, startDate, endDate, reason? }` |
| GET | `/api/leave/mine` | Authed | caller's own history |
| GET | `/api/leave?status=PENDING` | HR only | all requests, optional filter |
| PATCH | `/api/leave/{id}/decision` | HR only | `{ decision: "APPROVED"|"REJECTED", note? }` |

### Onboarding Bot
| Method | Route | Access | Notes |
|---|---|---|---|
| POST | `/api/onboarding/ask` | Authed | `{ message }` — keyword-matched FAQ answer |
| GET | `/api/onboarding/faqs` | Authed | list all |
| POST | `/api/onboarding/faqs` | HR only | `{ question, answer, keywords?, category? }` |
| PATCH | `/api/onboarding/complete` | Authed | mark caller's own onboarding done |

### Policy Q&A Assistant
| Method | Route | Access | Notes |
|---|---|---|---|
| POST | `/api/policy/ask` | Authed | `{ question }` — keyword search over handbook sections |
| GET | `/api/policy/documents` | Authed | list documents |
| POST | `/api/policy/documents` | HR only | `{ title, sections: [{heading, content, keywords}] }` |

### Dashboards
| Method | Route | Access | Notes |
|---|---|---|---|
| GET | `/api/dashboard/me` | Authed | caller's profile + recent leave requests |
| GET | `/api/dashboard/admin` | HR only | headcount, onboarding gaps, leave counts |

## Spring Boot 4 / Spring Security 7 details worth knowing

I checked these against current docs rather than assume, since this stack
moved fast enough that guessing would likely be wrong:

- **CSRF is now enabled by default for API endpoints too** (previously only
  form-based apps). `SecurityConfig` explicitly disables it
  (`.csrf(csrf -> csrf.disable())`) since this is a stateless JWT API —
  without that line, every POST/PATCH silently returns 403.
- `authorizeRequests()` is fully removed; only `authorizeHttpRequests()` works.
- jjwt's builder/parser API changed significantly at 0.12+. `JwtService` uses
  the current fluent style (`Jwts.builder()...signWith(key)`,
  `Jwts.parser().verifyWith(key).build().parseSignedClaims(token)`), not the
  older `setSubject()`/`parseClaimsJws()` methods.
- `@WebMvcTest` / `@DataMongoTest` package paths (verified against the 4.1.1
  API docs specifically, since they moved again between 4.0 and 4.1):
  - `@WebMvcTest` / `@AutoConfigureMockMvc` → `org.springframework.boot.webmvc.test.autoconfigure.*`
  - `@DataMongoTest` → `org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest`
- `@MockBean` is **removed**. Use `@MockitoBean` from
  `org.springframework.test.context.bean.override.mockito.MockitoBean`.
- `SecurityMockMvcRequestPostProcessors.user(UserDetails)` (used in
  `LeaveRequestControllerTest` to inject our custom `EmployeePrincipal`)
  is unchanged from earlier Spring Security versions.

## Known limitations (worth naming at defense, not fixing before it)

- **`@WebMvcTest` doesn't load `SecurityConfig`.** It only scans
  `@Controller`/`@ControllerAdvice`-type beans, not general `@Configuration`
  classes. So `LeaveRequestControllerTest` runs against Spring Boot's
  *default* fallback security (just "authenticated or not"), not our real
  `hasRole("HR_ADMIN")` rules. Testing the actual role restrictions would
  need a full `@SpringBootTest` integration test instead.
- **Policy Q&A and Onboarding bots use keyword matching**, not semantic
  search/embeddings — deliberate MVP scoping, noted since your original
  project form mentions Python (a natural fit for an embeddings upgrade).
- **CORS is wide open** (`allowedOriginPatterns("*")`) for local dev/coursework
  convenience — tighten before deploying anywhere public.
- **`app.jwt.secret` sits in plaintext `application.properties`**, same as
  the Mongo root credentials in `compose.yaml` — fine for coursework, worth
  moving to an env var before this goes in a public repo.
- **No password reset / account lockout / refresh tokens.** Out of scope for
  MVP; flag as a known gap if asked.

## Not verified in this sandbox

I don't have access to Maven Central from here, so I couldn't run a live
`mvn compile` / `mvn test`. I checked every file's package declaration against
its folder path and brace balance by script, grepped for stale references
after the auth refactor, and reviewed the code by hand — but please run
`./mvnw compile` yourself as the first real test and paste me any errors.

## JUnit tests

Under `src/test/java/com/hrbotproject/hr_bot/`:

- **`service/`** — pure Mockito unit tests (no Spring context, fast) for
  all 5 services: leave balance math, decision flow, keyword-matching bots,
  dashboard aggregation, duplicate-email rejection, etc.
- **`controller/LeaveRequestControllerTest`** — `@WebMvcTest` + `MockMvc`,
  using `.with(user(principal))` to simulate an authenticated employee.
- **`repository/EmployeeRepositoryTest`** — `@DataMongoTest` against a real
  Mongo connection. If it fails to connect, run `docker compose up -d`
  first, then `./mvnw test` again — I couldn't verify in this sandbox
  whether Spring's docker-compose support auto-starts for test-slice
  contexts the same way it does for the full app.

### Run the tests

```bash
./mvnw test
```
