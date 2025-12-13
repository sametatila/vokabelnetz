// Auth models
export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  displayName: string;
  nativeLanguage: 'TURKISH' | 'ENGLISH';
}

export interface AuthResponse {
  success: boolean;
  data: {
    accessToken: string;
    refreshToken: string;
    user: AuthUser;
  };
}

export interface AuthUser {
  id: number;
  email: string;
  displayName: string;
  role: string;
  emailVerified: boolean;
  nativeLanguage: 'TURKISH' | 'ENGLISH';
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: {
    code: string;
    message: string;
  };
}
