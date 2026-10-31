// One place for every call to the Spring Boot API.
// The JWT from /api/auth/login is kept in localStorage and sent as
// "Authorization: Bearer <token>" on every request.

const BASE_URL = import.meta.env.VITE_API_URL || '';
const TOKEN_KEY = 'hrbots_token';
// "staff" or "candidate": which /me endpoint to call when the app reloads
const KIND_KEY = 'hrbots_kind';

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  kind: () => localStorage.getItem(KIND_KEY) || 'staff',
  set: (token, kind = 'staff') => {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(KIND_KEY, kind);
  },
  clear: () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(KIND_KEY);
  },
};

// Login/register calls answer 401 for a wrong password; that must not
// log the current user out.
const isLoginCall = (path) =>
  path.startsWith('/api/auth/') || path === '/api/candidates/login' || path === '/api/candidates/register';

// Called when the API answers 401 (missing/expired token).
// AuthContext sets this so the app can log the user out.
let onUnauthorized = () => {};
export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler;
}

export class ApiError extends Error {
  constructor(status, message, fieldErrors) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors || {};
  }
}

async function request(method, path, body) {
  // FormData (file uploads) must NOT get a JSON Content-Type: the browser
  // sets multipart/form-data with the right boundary by itself.
  const isForm = body instanceof FormData;
  const headers = isForm ? {} : { 'Content-Type': 'application/json' };
  const token = tokenStore.get();
  if (token) headers.Authorization = `Bearer ${token}`;

  let response;
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : isForm ? body : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, 'Cannot reach the server. Is the Spring Boot backend running?');
  }

  const text = await response.text();
  const data = text ? safeJson(text) : null;

  if (!response.ok) {
    if (response.status === 401 && !isLoginCall(path)) {
      onUnauthorized();
    }
    // GlobalExceptionHandler returns { status, message, fieldErrors? }
    const message = (data && data.message) || defaultMessage(response.status);
    throw new ApiError(response.status, message, data && data.fieldErrors);
  }
  return data;
}

function safeJson(text) {
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

function defaultMessage(status) {
  if (status === 401) return 'Please log in again.';
  if (status === 403) return 'You do not have permission to do that.';
  if (status === 404) return 'Not found.';
  if (status === 413) return 'File is too large (max 5 MB).';
  return `Request failed (${status}).`;
}

const get = (path) => request('GET', path);
const post = (path, body) => request('POST', path, body);
const patch = (path, body) => request('PATCH', path, body);
const put = (path, body) => request('PUT', path, body);
const del = (path) => request('DELETE', path);

// Downloads a file (e.g. a candidate's CV) with the login token attached,
// then hands it to the browser as a normal download.
async function download(path, fallbackName) {
  const response = await fetch(`${BASE_URL}${path}`, {
    headers: { Authorization: `Bearer ${tokenStore.get()}` },
  });
  if (!response.ok) throw new ApiError(response.status, defaultMessage(response.status));
  const disposition = response.headers.get('Content-Disposition') || '';
  const match = /filename\*=UTF-8''([^;]+)|filename="?([^";]+)"?/i.exec(disposition);
  const name = match ? decodeURIComponent(match[1] || match[2]) : fallbackName;
  const url = URL.createObjectURL(await response.blob());
  const a = document.createElement('a');
  a.href = url;
  a.download = name;
  a.click();
  URL.revokeObjectURL(url);
}

// ---- Endpoints (match the controllers in src/main/java/.../controller) ----

export const api = {
  // AuthController
  login: (email, password) => post('/api/auth/login', { email, password }),
  register: (fullName, email, password, department) =>
    post('/api/auth/register', { fullName, email, password, department }),
  me: () => get('/api/auth/me'),

  // DashboardController
  myDashboard: () => get('/api/dashboard/me'),
  adminDashboard: () => get('/api/dashboard/admin'),

  // LeaveRequestController
  createLeave: (leaveType, startDate, endDate, reason) =>
    post('/api/leave', { leaveType, startDate, endDate, reason }),
  myLeave: () => get('/api/leave/mine'),
  allLeave: (status) => get(status ? `/api/leave?status=${status}` : '/api/leave'),
  decideLeave: (id, decision, note) => patch(`/api/leave/${id}/decision`, { decision, note }),

  // OnboardingController
  askOnboarding: (message) => post('/api/onboarding/ask', { message }),
  listFaqs: () => get('/api/onboarding/faqs'),
  createFaq: (question, answer, keywords, category) =>
    post('/api/onboarding/faqs', { question, answer, keywords, category }),
  completeOnboarding: () => patch('/api/onboarding/complete'),

  // PolicyController
  askPolicy: (question) => post('/api/policy/ask', { question }),
  listPolicies: () => get('/api/policy/documents'),
  createPolicy: (title, sections) => post('/api/policy/documents', { title, sections }),

  // EmployeeController (HR admin only)
  listEmployees: () => get('/api/employees'),

  // SettingsController: my alerts, tune and Islamic features
  mySettings: () => get('/api/settings/me'),
  saveSettings: (changes) => put('/api/settings/me', changes),

  // TodoController: each employee's own to-do list
  myTodos: () => get('/api/todos'),
  createTodo: (title, dueDate) => post('/api/todos', { title, dueDate }),
  updateTodo: (id, changes) => patch(`/api/todos/${id}`, changes),
  deleteTodo: (id) => del(`/api/todos/${id}`),
  // TodoController (HR admin only)
  assignTodo: (employeeId, title, dueDate, notes) =>
    post('/api/todos/assign', { employeeId, title, dueDate, notes }),
  employeeTodos: (employeeId) => get(`/api/todos/employee/${employeeId}`),


  // CandidateController (job applicants)
  candidateRegister: (fullName, email, password, phone) =>
  post('/api/candidates/register', { fullName, email, password, phone }),
  candidateLogin: (email, password) => post('/api/candidates/login', { email, password }),
  candidateMe: () => get('/api/candidates/me'),

    // JobController: public job board + candidate applying
    openJobs: () => get('/api/jobs'),
    openJob: (id) => get(`/api/jobs/${id}`),

    applyForJob: (id, file, coverNote) => {
    const form = new FormData();
    form.append('cv', file);
    if (coverNote) form.append('coverNote', coverNote);
    return post(`/api/jobs/${id}/apply`, form);
  },
  myApplications: () => get('/api/applications/mine'),

  // JobController / JobApplicationController (HR admin only)
  manageJobs: () => get('/api/jobs/manage'),
  createJob: (job) => post('/api/jobs', job),
  updateJob: (id, job) => put(`/api/jobs/${id}`, job),
  closeJob: (id) => patch(`/api/jobs/${id}/close`),
  reopenJob: (id) => patch(`/api/jobs/${id}/reopen`),
  jobApplications: (id) => get(`/api/jobs/${id}/applications`),
  decideApplication: (id, decision, note) =>
    patch(`/api/applications/${id}/decision`, { decision, note }),
  downloadCv: (id, fileName) => download(`/api/applications/${id}/cv`, fileName || 'cv'),
};

// Enum values, kept in the same order as the Java enums.
export const LEAVE_TYPES = ['ANNUAL', 'SICK', 'CASUAL', 'MATERNITY', 'PATERNITY', 'UNPAID'];
export const LEAVE_STATUSES = ['PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'];
export const APPLICATION_STATUSES = [
  'SHORTLISTED',
  'UNDER_REVIEW',
  'AUTO_REJECTED',
  'INTERVIEW',
  'HIRED',
  'REJECTED',
];
export const FAQ_CATEGORIES = ['PAYROLL', 'BENEFITS', 'EQUIPMENT', 'WORKPLACE', 'GENERAL'];
