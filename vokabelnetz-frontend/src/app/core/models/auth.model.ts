export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  displayName?: string;
  uiLanguage?: string;
  sourceLanguage?: string;
  timezone?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: AuthUser;
}

export interface AuthUser {
  id: number;
  email: string;
  displayName?: string;
  role: string;
  eloRating: number;
  currentStreak: number;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface Session {
  id: number;
  deviceInfo: string;
  ipAddress: string;
  createdAt: string;
  lastUsedAt: string;
  isCurrent: boolean;
}
