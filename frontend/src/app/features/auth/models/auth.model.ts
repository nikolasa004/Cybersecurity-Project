export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  role: string;
  organizationId: number | null;
  organizationName?: string;
}

export interface TokenRefreshResponse {
  accessToken: string;
  refreshToken: string;
}