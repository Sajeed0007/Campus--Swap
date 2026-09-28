import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { ErrorResponse } from '../types';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

/** Endpoints where a 401 is an expected outcome, not an expired session. */
const AUTH_ENDPOINTS = ['/api/auth/login', '/api/auth/register'];

/**
 * Error thrown for every failed request. Extends Error so existing
 * `err instanceof Error ? err.message : ...` checks keep working, while also
 * exposing the HTTP status and field-level validation errors.
 */
export class ApiError extends Error {
  readonly status?: number;
  readonly validationErrors?: Record<string, string>;

  constructor(message: string, status?: number, validationErrors?: Record<string, string>) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.validationErrors = validationErrors;
  }
}

const axiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

axiosInstance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

axiosInstance.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ErrorResponse>) => {
    if (!error.response) {
      if (error.request) {
        return Promise.reject(new ApiError('Network error. Please check your connection.'));
      }
      return Promise.reject(error);
    }

    const { status, data } = error.response;
    const requestUrl = error.config?.url ?? '';
    const isAuthRequest = AUTH_ENDPOINTS.some((endpoint) => requestUrl.includes(endpoint));

    // A 401 means "expired or invalid session" only when we actually held a token
    // and the call was not a login/register attempt. Redirecting on a failed login
    // would reload the page and discard the error message the user needs to see.
    if (status === 401 && !isAuthRequest) {
      const hadToken = !!localStorage.getItem('token');
      localStorage.removeItem('token');
      localStorage.removeItem('user');

      const alreadyOnLogin = window.location.pathname === '/login';
      if (hadToken && !alreadyOnLogin) {
        window.location.assign('/login');
      }
    }

    const message =
      data?.message ||
      (status === 401 ? 'Invalid email or password' : 'An unexpected error occurred');

    return Promise.reject(new ApiError(message, status, data?.validationErrors));
  }
);

export default axiosInstance;
