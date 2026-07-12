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

---
