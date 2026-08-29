/**
 * SplitSmart API client layer
 *
 * Wraps axios with:
 * - Base URL configuration (reads VITE_API_BASE_URL from env)
 * - JWT Bearer token injection on every request
 * - Automatic token refresh on 401 responses (single retry)
 * - Idempotency-Key header injection for POST / PATCH / PUT requests
 * - Consistent error normalization into ApiError
 */

import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from "axios";
import { v4 as uuidv4 } from "uuid";

// ─── Constants ───────────────────────────────────────────────────────────────

const API_BASE_URL =
  (import.meta as unknown as { env: Record<string, string> }).env
    ?.VITE_API_BASE_URL ?? "http://localhost:8080";

const TOKEN_KEY   = "splitsmart_access_token";
const REFRESH_KEY = "splitsmart_refresh_token";

// ─── Token helpers ────────────────────────────────────────────────────────────

export const tokenStore = {
  getAccess:  ()      => localStorage.getItem(TOKEN_KEY) ?? "",
  setAccess:  (t: string) => localStorage.setItem(TOKEN_KEY, t),
  getRefresh: ()      => localStorage.getItem(REFRESH_KEY) ?? "",
  setRefresh: (t: string) => localStorage.setItem(REFRESH_KEY, t),
  clear:      ()      => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_KEY);
  },
};

// ─── ApiError ────────────────────────────────────────────────────────────────

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
    public readonly details?: unknown,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

// ─── Axios instance ───────────────────────────────────────────────────────────

const api: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15_000,
  headers: { "Content-Type": "application/json" },
});

// ─── Request interceptor — attach JWT + idempotency key ───────────────────────

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = tokenStore.getAccess();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  // Inject idempotency key for mutating requests
  const method = (config.method ?? "get").toLowerCase();
  if (["post", "patch", "put"].includes(method)) {
    config.headers["X-Idempotency-Key"] ??= uuidv4();
  }

  return config;
});

// ─── Response interceptor — 401 refresh + error normalization ────────────────

let isRefreshing = false;
let refreshQueue: Array<(token: string) => void> = [];

const processQueue = (token: string) => {
  refreshQueue.forEach((cb) => cb(token));
  refreshQueue = [];
};

api.interceptors.response.use(
  (res: AxiosResponse) => res,
  async (error) => {
    const original = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    // ── 401 token refresh flow ────────────────────────────────────────────────
    if (error.response?.status === 401 && !original._retry) {
      original._retry = true;

      if (isRefreshing) {
        // Queue the request until the ongoing refresh completes
        return new Promise<AxiosResponse>((resolve) => {
          refreshQueue.push((newToken) => {
            original.headers.Authorization = `Bearer ${newToken}`;
            resolve(api(original));
          });
        });
      }

      isRefreshing = true;
      try {
        const refreshToken = tokenStore.getRefresh();
        if (!refreshToken) throw new Error("No refresh token");

        const { data } = await axios.post<{ token: string }>(
          `${API_BASE_URL}/api/v1/auth/refresh`,
          { refreshToken },
        );

        tokenStore.setAccess(data.token);
        processQueue(data.token);
        original.headers.Authorization = `Bearer ${data.token}`;
        return api(original);
      } catch (_) {
        tokenStore.clear();
        window.dispatchEvent(new Event("splitsmart:session-expired"));
        return Promise.reject(
          new ApiError(401, "SESSION_EXPIRED", "Your session has expired. Please log in again."),
        );
      } finally {
        isRefreshing = false;
      }
    }

    // ── Normalize axios errors into ApiError ──────────────────────────────────
    if (axios.isAxiosError(error)) {
      const status  = error.response?.status ?? 0;
      const payload = error.response?.data as Record<string, unknown> | undefined;
      const code    = (payload?.code as string) ?? "UNKNOWN_ERROR";
      const message = (payload?.message as string) ?? error.message;
      return Promise.reject(new ApiError(status, code, message, payload?.details));
    }

    return Promise.reject(error);
  },
);

// ─── Typed request helpers ────────────────────────────────────────────────────

export const apiGet = <T>(url: string, config?: AxiosRequestConfig) =>
  api.get<T>(url, config).then((r) => r.data);

export const apiPost = <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
  api.post<T>(url, data, config).then((r) => r.data);

export const apiPatch = <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
  api.patch<T>(url, data, config).then((r) => r.data);

export const apiPut = <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
  api.put<T>(url, data, config).then((r) => r.data);

export const apiDelete = <T>(url: string, config?: AxiosRequestConfig) =>
  api.delete<T>(url, config).then((r) => r.data);

export default api;
