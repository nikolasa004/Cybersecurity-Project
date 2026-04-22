export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  role: string;
  organizationId: number | null;
}

export interface TokenRefreshResponse {
  accessToken: string;
  refreshToken: string;
}