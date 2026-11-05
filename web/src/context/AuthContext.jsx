import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { api, deviceStore, setUnauthorizedHandler, tokenStore } from '../api/client';

const AuthContext = createContext(null);

// Two kinds of user share one login token slot:
//   employee  - staff (EMPLOYEE / HR_ADMIN), logs in at /login
//   candidate - job applicant, logs in at /careers/login
// Only one is set at a time.
//
// Login, sign-up and code checks all return { status, ... }:
//   status "OK"        -> logged in ({ token, employee } or { token, candidate })
//   status "CODE_SENT" -> a 6-digit code was emailed; the page shows the code box
export function AuthProvider({ children }) {
  const [employee, setEmployee] = useState(null);
  const [candidate, setCandidate] = useState(null);
  // true until we've checked whether a saved token is still valid
  const [loading, setLoading] = useState(true);

  const logout = useCallback(() => {
    tokenStore.clear();
    setEmployee(null);
    setCandidate(null);
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(logout);

    if (!tokenStore.get()) {
      setLoading(false);
      return;
    }
    const restore =
      tokenStore.kind() === 'candidate'
        ? api.candidateMe().then(setCandidate)
        : api.me().then(setEmployee);
    restore.catch(logout).finally(() => setLoading(false));
  }, [logout]);

  // Remembers this device, and starts a session when the result says "OK"
  const handle = (result) => {
    if (result.deviceToken) deviceStore.set(result.email, result.deviceToken);
    if (result.status === 'OK' && result.employee) {
      tokenStore.set(result.token, 'staff');
      setCandidate(null);
      setEmployee(result.employee);
    } else if (result.status === 'OK' && result.candidate) {
      tokenStore.set(result.token, 'candidate');
      setEmployee(null);
      setCandidate(result.candidate);
    }
    return result;
  };

  const login = async (email, password) => handle(await api.login(email, password));

  const register = async (fullName, email, password, department, inviteCode) =>
    handle(await api.register(fullName, email, password, department, inviteCode));

  const registerCompany = async (companyName, industry, fullName, email, password) =>
    handle(await api.registerCompany(companyName, industry, fullName, email, password));

  const candidateLogin = async (email, password) => handle(await api.candidateLogin(email, password));

  const candidateRegister = async (fullName, email, password, phone) =>
    handle(await api.candidateRegister(fullName, email, password, phone));

  const verifyCode = async (email, purpose, code) => handle(await api.verifyCode(email, purpose, code));

  const resetPassword = async (email, code, newPassword) =>
    handle(await api.resetPassword(email, code, newPassword));

  const isAdmin = employee?.role === 'HR_ADMIN';

  return (
    <AuthContext.Provider
      value={{
        employee,
        setEmployee,
        candidate,
        isAdmin,
        loading,
        login,
        register,
        registerCompany,
        candidateLogin,
        candidateRegister,
        verifyCode,
        resetPassword,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}