# Pocket Ledger — Backend

A multi-user personal finance API, built with Spring Boot. It's the backend
for [Pocket Ledger](https://app.pocketledgerapp.com), a real app I use myself
day to day to track my own expenses — not a demo project.

This started as a solo, single-user Cloudflare Worker (Hono + D1). This repo
is the rewrite: a proper multi-tenant Spring Boot backend, deployed on AWS,
that any number of people can actually sign up and use independently.

> **A note on authorship:** everything in this repo is my own work — I wrote
> every line of the backend myself, with Claude (Anthropic) reviewing and
> catching real bugs along the way. The
> [frontend](https://github.com/abhishekpanja554/pocket_ledger) was a
> deliberate exception: I had Claude build it entirely so I could put my own
> learning time into Spring Boot instead.

---

## What it does

- **Auth**: registration, login, email verification, forgot/reset password —
  session-based, with Spring Security handling CSRF and cookie scoping across
  the frontend's separate subdomain.
- **Transactions**: CRUD with fingerprint-based duplicate detection, so
  re-importing the same bank statement never creates duplicate rows.
- **Preferences**: a single atomic settings resource covering categories,
  accounts, tags, rules, goals, budgets, and Drive-sync configuration.
- **Documents**: file upload/download backed by S3, with per-file status
  tracking (stored vs. needs review).
- **Statement parsing**: CSV/TSV and Excel (`.xlsx`/`.xlsm`) bank statement
  imports, mapped into transactions.
- **Google Drive sync**: a daily scheduled job (and a manual trigger
  endpoint) that reads a shared Drive folder, downloads new files, and runs
  them through AI receipt extraction (Google Gemini) to create transactions
  automatically — with an explicit no-guessing rule: if the model can't
  confidently read the date, merchant, amount, and type, it stores the file
  for manual review instead of fabricating data.

## API surface

| Resource | Endpoints |
| --- | --- |
| Auth | `POST /api/auth/{register,login,verify-email,resend-verification,forgot-password,reset-password}`, `GET /api/auth/me` |
| State | `GET /api/state` — the full app-state snapshot the frontend renders from |
| Transactions | `POST /api/transactions`, `PATCH /api/transactions/{id}`, `DELETE /api/transactions/{id}` |
| Preferences | `PUT /api/preferences` |
| Documents | `POST /api/documents` (multipart), `GET /api/documents/{id}/file`, `DELETE /api/documents/{id}` |
| Drive sync | `POST /api/sync/run` (manual trigger; a `@Scheduled` job also runs this daily at 8 AM IST) |

Every response is wrapped in a consistent `{status, data}` /
`{status, errorDetails}` envelope.

## Stack

Java, Spring Boot, Spring Security, Spring Data JPA (Hibernate), Flyway
migrations, PostgreSQL, AWS S3 (documents), Google Drive API, Google Gemini
(receipt extraction), Apache POI (Excel parsing), Commons CSV.

Deployed on AWS: ECS Fargate behind an Application Load Balancer, RDS for
Postgres, S3 for file storage, IAM roles scoped per-service rather than a
shared static key.

## Running locally

```bash
./mvnw spring-boot:run
```

Needs a local Postgres reachable at the URL below (or override via env var),
plus Drive/Gemini credentials since a couple of beans construct eagerly at
startup:

| Env var | Purpose | Local default |
| --- | --- | --- |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | Postgres connection | `jdbc:postgresql://localhost:5432/pocket_ledger`, `abhis`, _(empty)_ |
| `GOOGLE_APPLICATION_CREDENTIALS` | Path to a Drive-enabled service account key | — |
| `GOOGLE_API_KEY` | Gemini API key (a dummy value is enough to get past startup if you're not exercising extraction) | — |
| `S3_BUCKET` / `AWS_REGION` | Document storage | a real bucket / `ap-south-1` |
| `CORS_ALLOWED_ORIGIN` / `FRONTEND_URL` | Frontend origin, for CORS + email links | `http://localhost:5173` |
| `COOKIE_DOMAIN` | Cookie `Domain` scope, only needed when frontend/backend are on different subdomains of the same domain | _(empty — host-only cookie)_ |

## Tests

```bash
./mvnw test
```

Also needs `GOOGLE_APPLICATION_CREDENTIALS` exported for the same
eager-bean-construction reason as above.

## Related

- [Frontend repo](https://github.com/abhishekpanja554/pocket_ledger) — React
  + TypeScript, deployed on Vercel.
