# CodeArena AI

A full-stack, competitive-programming judge (think LeetCode / Codeforces) with a
built-in **AI coding tutor** — Socratic hints and a mock interviewer powered by
Groq. Solve problems in six languages, compete in rated contests, climb a live
leaderboard, and get guidance that nudges you toward the answer without spoiling it.

100% free to run locally. No paid services required — the AI works with a free
Groq API key, and gracefully falls back to an offline hint engine when no key is set.

---

## ✨ Features

- **Multi-language judge** — Python, JavaScript, Java, C++, C, and Go, sandboxed with Docker (with a local-process fallback).
- **Run & Submit** — test against sample cases, then judge against hidden tests, LeetCode-style with per-case Input / Expected / Your Output.
- **Rated contests** — timed contests with a live standings feed and multiplayer Elo rating updates.
- **Live leaderboard** — real-time updates over Server-Sent Events (SSE).
- **User profiles** — solved-by-difficulty breakdown, acceptance rate, global rank, submission activity heatmap, editable bio.
- **AI coding tutor (Groq)**
  - **Socratic hints** — three escalating levels (clarify → technique → plain-English plan) that never reveal full code.
  - **Mock interviewer** — a chat interviewer that makes you think aloud and probes approach, complexity, and edge cases.
  - **Typewriter reveal** for AI responses, with an offline heuristic fallback.
- **Auth** — JWT with expiry + sliding refresh, PBKDF2-hashed passwords, USER/ADMIN roles, password-change token invalidation.
- **Admin panel** — CRUD for problems and contests.

---

## 🧱 Tech stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 4.1, Java 17, Spring Data JPA |
| Database | H2 (file mode) — swap to PostgreSQL with one property |
| Auth | JJWT (HS256), PBKDF2 password hashing |
| AI | Groq API (OpenAI-compatible), model `openai/gpt-oss-120b` |
| Judge | Docker (per-language images) + local fallback |
| Frontend | React 18, Vite 5, Tailwind CSS 3, Monaco Editor, Framer Motion, React Three Fiber |
| Realtime | Server-Sent Events (SSE) |

---

## 📦 Prerequisites

- **Java 17** (JDK)
- **Node.js 18+** and npm
- **Docker Desktop** (optional, for sandboxed judging of all languages)
- **A free Groq API key** (optional, for live AI) — get one at <https://console.groq.com>

> No global Maven needed — the backend ships with the Maven Wrapper (`mvnw`).

---

## 🚀 Getting started

### 1. Backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The API starts on <http://localhost:8080>. On first run it seeds 12 problems, a
few demo users, and a sample contest into an H2 file database stored at
`~/.codearena/db` (outside the repo, so it persists across restarts).

### 2. Frontend

```powershell
cd frontend
npm install
npm run dev
```

The app opens on <http://localhost:5173> and proxies `/api` to the backend.

---

## 🤖 Enabling the AI (Groq)

The AI features work out of the box in **offline heuristic mode**. To enable live
Groq responses, add your key to a **git-ignored** local file:

1. Copy the template:
   ```powershell
   cd backend
   Copy-Item application-local.properties.example application-local.properties
   ```
2. Open `backend/application-local.properties` and paste your key:
   ```properties
   codearena.ai.groq.api-key=gsk_your_key_here
   ```
3. Restart the backend.

`application-local.properties` is listed in `.gitignore`, so your key is never
committed. Verify it's active at <http://localhost:8080/api/ai/status> →
`{"enabled": true}`.

> Alternatively, set the `GROQ_API_KEY` environment variable instead of the file.

---

## ✅ Testing

Run the backend test suite (42 tests: hint engine, rate limiter, AI service,
controller edge cases, context load) on an isolated in-memory profile:

```powershell
cd backend
.\mvnw.cmd test
```

---


## 🗂️ Project structure

```
.
├── backend/                 # Spring Boot API
│   ├── src/main/java/com/codearena
│   │   ├── ai/              # Groq client, heuristic hints, rate limiter
│   │   ├── controller/      # REST + SSE endpoints
│   │   ├── service/         # judging, contests, ratings, auth, profile
│   │   ├── judge/           # Docker & local judge engines
│   │   ├── model/           # JPA entities
│   │   └── security/        # JWT + password hashing
│   └── src/test/java/...    # JUnit 5 + Mockito tests
└── frontend/                # React + Vite SPA
    └── src/{pages,components,context,api,lib}
```

---

## 🐘 Using PostgreSQL (for deployment)

The app runs on an embedded **H2 file database** by default — zero setup, great
for local development. For a real deployment, switch to **PostgreSQL** by
activating the `postgres` profile. **No code changes and no local install
needed** — you only provide a connection via environment variables.

The PostgreSQL JDBC driver is already bundled, and
`backend/src/main/resources/application-postgres.properties` is ready to go.

### Option A — a free hosted database (recommended, nothing to install)

Providers like **Neon** (<https://neon.tech>), **Supabase**
(<https://supabase.com>) or **Railway** give you a free Postgres instance and a
connection string. Then just point the app at it:

```powershell
$env:SPRING_PROFILES_ACTIVE = "postgres"
$env:DB_URL = "jdbc:postgresql://<host>:5432/<database>?sslmode=require"
$env:DB_USERNAME = "<user>"
$env:DB_PASSWORD = "<password>"
cd backend
.\mvnw.cmd spring-boot:run
```

> Note the `jdbc:postgresql://` prefix — copy the host/db/user/password from your
> provider's dashboard into the `DB_*` variables.

### Option B — run Postgres in Docker (no system install)

```powershell
docker run --name codearena-pg -e POSTGRES_DB=codearena -e POSTGRES_USER=codearena -e POSTGRES_PASSWORD=secret -p 5432:5432 -d postgres:16
```

Then start the backend with the profile (defaults already match the container):

```powershell
$env:SPRING_PROFILES_ACTIVE = "postgres"
$env:DB_PASSWORD = "secret"
cd backend
.\mvnw.cmd spring-boot:run
```

### Option C — a locally installed PostgreSQL

1. Create the database and user:
   ```sql
   CREATE DATABASE codearena;
   CREATE USER codearena WITH ENCRYPTED PASSWORD 'secret';
   GRANT ALL PRIVILEGES ON DATABASE codearena TO codearena;
   ```
2. Set `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` and run with `SPRING_PROFILES_ACTIVE=postgres` as above.

On first start, Hibernate creates all tables (`ddl-auto=update`) and the seeder
populates demo data. To go back to H2, simply unset `SPRING_PROFILES_ACTIVE`.

---

## ⚙️ Configuration reference

Key properties in `backend/src/main/resources/application.properties`:

| Property | Default | Purpose |
|---|---|---|
| `codearena.judge.prefer` | `docker` | `docker` (sandbox all langs) or `local` |
| `codearena.jwt.ttl-hours` | `24` | Access-token lifetime |
| `codearena.jwt.secret` | dev default | HS256 secret — override via `JWT_SECRET` |
| `codearena.ai.provider` | `groq` | `groq` for live AI, else heuristic |
| `codearena.ai.model` | `openai/gpt-oss-120b` | Groq model id |
| `codearena.admin.key` | dev default | `X-Admin-Key` for admin API |
| `SPRING_PROFILES_ACTIVE` | _(none → H2)_ | set to `postgres` to use PostgreSQL |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | — | PostgreSQL connection (postgres profile) |

---

## 📄 License

Released under the MIT License. See `LICENSE` (add one if you plan to publish).
