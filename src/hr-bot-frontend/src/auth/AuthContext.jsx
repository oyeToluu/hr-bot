import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { authApi, setUnauthorizedHandler, tokenStore } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  // "loading" while we check a saved token on page load.
  const [status, setStatus] = useState(tokenStore.get() ? 'loading' : 'ready');

  const logout = useCallback(() => {
    tokenStore.clear();
    setUser(null);
    setStatus('ready');
  }, []);

  // Any 401 from the API (expired or tampered token) logs the user out.
  useEffect(() => {
    setUnauthorizedHandler(logout);
  }, [logout]);

  // On first load, if a token was saved, ask the backend who it belongs to.
  useEffect(() => {
    if (!tokenStore.get()) return;
    authApi
      .me()
      .then((employee) => setUser(employee))
      .catch(() => tokenStore.clear())
      .finally(() => setStatus('ready'));
  }, []);

  const handleAuthResponse = (res) => {
    tokenStore.set(res.token);
    setUser(res.employee);
    return res.employee;
  };

  const login = async (email, password) => handleAuthResponse(await authApi.login(email, password));
  const register = async (form) => handleAuthResponse(await authApi.register(form));

  const value = {
    user,
    isLoading: status === 'loading',
    isAuthenticated: !!user,
    isAdmin: user?.role === 'HR_ADMIN',
    login,
    register,
    logout,
    setUser,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}
