# API

Base URL: `http://localhost:3000/api`

- `GET /health`
- `POST /auth/signup` `{email,password}`
- `POST /auth/login` `{email,password}`
- `POST /auth/logout` (authenticated)
- `GET /auth/me` (authenticated)
- `GET /dashboard` (authenticated)
- `POST /sessions`, `GET /sessions`, `GET /sessions/:id`, `PATCH /sessions/:id` (authenticated)
- `POST /sessions/:id/start|pause|resume|finish|extend` (authenticated; use individual route suffix)
- `POST /sessions/:id/events` (authenticated)
- `POST /interventions/evaluate`, `GET /interventions`, `POST /interventions/:id/respond` (authenticated)
- `GET /insights/overview`, `GET /insights/weekly` (authenticated)
- `GET /settings`, `PATCH /settings`, `POST /consent` (authenticated)
- `POST /feedback` (authenticated)
- `GET /experiments`, `POST /experiments`, `POST /experiments/:id/assign`, `GET /experiments/:id/results` (authenticated)
- `GET /account/export`, `DELETE /account` (authenticated)
