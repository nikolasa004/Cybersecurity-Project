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

export interface RegisterUserRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  confirmPassword: string;
  organizationId: number;
}

export interface ActivationChallengeRequest {
  token: string;
  publicKey: string;
}

export interface ActivationChallengeResponse {
  encryptedChallenge: string;
}

export interface ActivationCompleteRequest {
  token: string;
  decryptedChallenge: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ForgotPasswordChallengeRequest {
  token: string;
}

export interface ForgotPasswordCompleteRequest {
  token: string;
  decryptedChallenge: string;
  newPassword: string;
  confirmPassword: string;
}