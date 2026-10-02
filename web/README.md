# HR Bots — Web app

React (Vite) front end for the HR Bots Spring Boot API. It uses the same
REST endpoints the Android app will use.

## Run it

You need **Node.js 20.19 or newer** (check with `node -v`).

1. Start MongoDB and the backend from the `hr-bot` folder:
   ```
   .\mvnw.cmd spring-boot:run
   ```
   Wait for `Started HrBotApplication`. The API runs on http://localhost:8080.
2. In a second terminal, start the web app:
   ```
   cd web
   npm install
   npm run dev
   ```
3. Open http://localhost:5173

In development, Vite forwards every `/api/...` request to `localhost:8080`
(see `vite.config.js`), so no CORS setup is needed.

## Demo accounts

Created by `DemoDataSeeder` the first time the backend starts:

| Role     | Email               | Password    |
|----------|---------------------|-------------|
| HR admin | admin@hrbots.test   | password123 |
| Employee | ada@hrbots.test     | password123 |

## Pages

| Page | Who | API used |
|---|---|---|
| Login / Register | everyone | `POST /api/auth/login`, `POST /api/auth/register` |
| My dashboard | employee | `GET /api/dashboard/me` |
| Leave requests | employee | `POST /api/leave`, `GET /api/leave/mine` |
| Onboarding bot | employee | `POST /api/onboarding/ask`, `GET /api/onboarding/faqs`, `PATCH /api/onboarding/complete` |
| Policy assistant | employee | `POST /api/policy/ask`, `GET /api/policy/documents` |
| Admin overview | HR admin | `GET /api/dashboard/admin` |
| Leave approvals | HR admin | `GET /api/leave?status=`, `PATCH /api/leave/{id}/decision` |
| Employees | HR admin | `GET /api/employees` |
| FAQs & policies | HR admin | `POST /api/onboarding/faqs`, `POST /api/policy/documents` |

## Project structure

```
web/
├── index.html
├── package.json
├── vite.config.js          # dev server + /api proxy to :8080
├── .env.example            # VITE_API_URL for hosting separately
└── src/
    ├── main.jsx            # app entry
    ├── App.jsx             # routes
    ├── styles.css          # all styling (light + dark, phone layout)
    ├── api/client.js       # every API call + JWT handling
    ├── context/AuthContext.jsx
    ├── components/         # Layout, route guards, ChatBox, small UI helpers
    └── pages/              # employee pages + pages/admin/
```

## Build for production

```
npm run build
```

Output goes to `web/dist`. If the web app is hosted on a different address
than the API, copy `.env.example` to `.env` and set `VITE_API_URL` to the
API's URL before building.
