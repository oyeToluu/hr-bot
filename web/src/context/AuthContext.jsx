import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { api, setUnauthorizedHandler, tokenStore } from '../api/client';

const AuthContext = createContext(null);

// Two kinds of user share one login token slot:
//   employee  - staff (EMPLOYEE / HR_ADMIN), logs in at /login
//   candidate - job applicant, logs in at /careers/login
// Only one is set at a time.
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

  // Staff login and register return { token, employee }
  const saveSession = ({ token, employee: emp }) => {
    tokenStore.set(token, 'staff');
    setCandidate(null);
    setEmployee(emp);
    return emp;
  };

  // Candidate login and register return { token, candidate }
  const saveCandidateSession = ({ token, candidate: cand }) => {
    tokenStore.set(token, 'candidate');
    setEmployee(null);
    setCandidate(cand);
    return cand;
  };

  const login = async (email, password) => saveSession(await api.login(email, password));

  const register = async (fullName, email, password, department) =>
    saveSession(await api.register(fullName, email, password, department));

  const candidateLogin = async (email, password) =>
    saveCandidateSession(await api.candidateLogin(email, password));

  const candidateRegister = async (fullName, email, password, phone) =>
    saveCandidateSession(await api.candidateRegister(fullName, email, password, phone));

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
        candidateLogin,
        candidateRegister,
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
