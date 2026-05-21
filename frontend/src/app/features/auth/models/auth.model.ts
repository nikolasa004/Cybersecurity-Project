export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  role: string;
  organizationId: number | null;
  organizationName?: string;
  isTwoFactorRequired?: boolean;
}

export interface TokenRefreshResponse {
  accessToken: string;
  refreshToken: string;
}