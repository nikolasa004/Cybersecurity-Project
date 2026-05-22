import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/config/api.config';
import {
  RegisterUserRequest,
  ActivationChallengeRequest,
  ActivationChallengeResponse,
  ActivationCompleteRequest,
  ForgotPasswordRequest,
  ForgotPasswordChallengeRequest,
  ForgotPasswordCompleteRequest
} from '../models/auth.model';

@Injectable({
  providedIn: 'root'
})
export class PublicUserAuthService {
  private http = inject(HttpClient);
  private readonly baseUrl = `${API_BASE_URL}/auth`;

  register(payload: RegisterUserRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/register`, payload);
  }

  createActivationChallenge(payload: ActivationChallengeRequest): Observable<ActivationChallengeResponse> {
    return this.http.post<ActivationChallengeResponse>(`${this.baseUrl}/activation/challenge`, payload);
  }

  completeActivation(payload: ActivationCompleteRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/activation/complete`, payload);
  }

  requestPasswordReset(payload: ForgotPasswordRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/forgot-password/request`, payload);
  }

  createPasswordResetChallenge(payload: ForgotPasswordChallengeRequest): Observable<ActivationChallengeResponse> {
    return this.http.post<ActivationChallengeResponse>(`${this.baseUrl}/forgot-password/challenge`, payload);
  }

  completePasswordReset(payload: ForgotPasswordCompleteRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/forgot-password/complete`, payload);
  }
}