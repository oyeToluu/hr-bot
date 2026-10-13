// One place for every call to the Spring Boot API.
// It adds the JWT to each request and turns error responses into
// readable messages, so pages never deal with fetch details.

const TOKEN_KEY = 'hrbot_token';

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

// AuthContext registers a handler here so a 401 anywhere logs the user out.
let onUnauthorized = () => {};
export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler;
}

export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

async function request(path, { method = 'GET', body, auth = true } = {}) {
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  const token = tokenStore.get();
  if (auth && token) headers.Authorization = `Bearer ${token}`;

  let response;
  try {
    response = await fetch(`/api${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, 'Cannot reach the server. Is the Spring Boot app running on port 8080?');
  }

  // Expired, invalid or missing token on a protected route.
  if (response.status === 401 && auth) {
    onUnauthorized();
    throw new ApiError(401, 'Your session has expired. Please log in again.');
  }

  const text = await response.text();
  const data = text ? safeJson(text) : null;

  if (!response.ok) {
    throw new ApiError(response.status, readableError(response.status, data));
  }
  return data;
}

function safeJson(text) {
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function readableError(status, data) {
  if (data && typeof data === 'object') {
    // GlobalExceptionHandler sends { message: "Validation failed", fieldErrors: { field: msg } }
    if (data.fieldErrors && Object.keys(data.fieldErrors).length) {
      return Object.entries(data.fieldErrors)
        .map(([field, msg]) => `${field}: ${msg}`)
        .join(' · ');
    }
    if (data.message) return data.message;
    if (data.error) return data.error;
  }
  if (typeof data === 'string' && data.length < 200) return data;
  if (status === 401) return 'Invalid email or password.';
  if (status === 403) return "You don't have permission to do that.";
  if (status >= 500) return 'Something went wrong on the server. Please try again.';
  return `Request failed (${status}).`;
}

export const api = {
  get: (path, opts) => request(path, { ...opts, method: 'GET' }),
  post: (path, body, opts) => request(path, { ...opts, method: 'POST', body }),
  put: (path, body, opts) => request(path, { ...opts, method: 'PUT', body }),
  patch: (path, body, opts) => request(path, { ...opts, method: 'PATCH', body }),
  delete: (path, opts) => request(path, { ...opts, method: 'DELETE' }),
};

// ---- Endpoints, grouped by backend controller ----
export const authApi = {
  login: (email, password) => api.post('/auth/login', { email, password }, { auth: false }),
  register: (form) => api.post('/auth/register', form, { auth: false }),
  me: () => api.get('/auth/me'),
};

export const dashboardApi = {
  mine: () => api.get('/dashboard/me'),
  admin: () => api.get('/dashboard/admin'),
};

export const leaveApi = {
  create: (req) => api.post('/leave', req), // { leaveType, startDate, endDate, reason }
  mine: () => api.get('/leave/mine'),
  all: (status) => api.get(status ? `/leave?status=${status}` : '/leave'),
  decide: (id, decision, note) => api.patch(`/leave/${id}/decision`, { decision, note }),
};

export const onboardingApi = {
  ask: (message) => api.post('/onboarding/ask', { message }),
  faqs: () => api.get('/onboarding/faqs'),
  complete: () => api.patch('/onboarding/complete'),
};

export const policyApi = {
  ask: (question) => api.post('/policy/ask', { question }),
  documents: () => api.get('/policy/documents'),
};

export const employeeApi = {
  list: () => api.get('/employees'),
};
