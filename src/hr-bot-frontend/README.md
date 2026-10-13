# HR Bot — Frontend

React (Vite) web app for the HR Bot Spring Boot API.

## Run it

1. Start Docker Desktop and the Spring Boot backend (`./mvnw spring-boot:run` in the `hr-bot` folder). It must be on port 8080.
2. In this folder:

   ```bash
   npm install
   npm run dev
   ```

3. Open http://localhost:5173

During development, Vite forwards every `/api/...` request to `http://localhost:8080` (see `vite.config.js`), so there is no CORS setup to worry about.

## How login works

- `src/api/client.js` — every API call goes through here. It attaches `Authorization: Bearer <token>` and turns errors into readable messages. A 401 logs the user out.
- `src/auth/AuthContext.jsx` — holds the logged-in user. The JWT is saved in `localStorage`; on page load it calls `GET /api/auth/me` to restore the session.
- `src/components/ProtectedRoute.jsx` — blocks pages for logged-out users or the wrong role. The backend still enforces the real rules in `SecurityConfig`.

## Pages

| Route | Who | API |
|---|---|---|
| `/login`, `/register` | Everyone | `/api/auth/login`, `/api/auth/register` |
| `/dashboard` | Employee, HR admin | `/api/dashboard/me` |
| `/leave` | Employee, HR admin | `/api/leave`, `/api/leave/mine` |
| `/onboarding` | Employee, HR admin | `/api/onboarding/ask`, `/faqs`, `/complete` |
| `/policy` | Employee, HR admin | `/api/policy/ask` |
| `/admin` | HR admin | `/api/dashboard/admin` |
| `/admin/leave` | HR admin | `/api/leave?status=`, `/api/leave/{id}/decision` |
| `/admin/employees` | HR admin | `/api/employees` |
| `/admin/jobs` | HR admin | Placeholder (recruitment comes next) |

## Demo logins

The backend's `DemoDataSeeder` creates these when the database is empty:

- HR admin: `admin@hrbots.test` / `password123`
- Employee: `ada@hrbots.test` / `password123`

## Folder layout

```
src/
  api/client.js          API calls + JWT handling
  auth/AuthContext.jsx   logged-in user state
  components/            layout and route guards
  pages/                 one file per screen
  styles.css             all styling
```
