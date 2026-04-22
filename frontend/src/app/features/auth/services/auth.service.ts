import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { LoginResponse, TokenRefreshResponse } from '../models/auth.model';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly AUTH_URL = 'http://localhost:8081/api/auth';
  
  private userSubject = new BehaviorSubject<any>(null);

  constructor(private http: HttpClient, private router: Router) {}

  login(credentials: any): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.AUTH_URL}/login`, credentials).pipe(
      tap(response => {
        this.storeTokens(response);
      })
    );
  }

  refreshToken(): Observable<TokenRefreshResponse> {
    const refreshToken = localStorage.getItem('refreshToken');
    return this.http.post<TokenRefreshResponse>(`${this.AUTH_URL}/refresh-token`, { refreshToken }).pipe(
      tap(response => {
        localStorage.setItem('accessToken', response.accessToken);
        localStorage.setItem('refreshToken', response.refreshToken);
      })
    );
  }

  logout(): void {
    const refreshToken = localStorage.getItem('refreshToken');
    this.finalizeLogout();

    this.http.post(`${this.AUTH_URL}/logout`, { refreshToken }).subscribe({
      next: () => console.log('Backend session cleared'),
      error: () => console.log('Backend session already expired or unreachable')
    });
  }

  finalizeLogout(): void {
    localStorage.clear();
    this.userSubject.next(null);
    this.router.navigate(['/auth/login']);
  }

  private storeTokens(response: LoginResponse): void {
    localStorage.setItem('accessToken', response.accessToken);
    localStorage.setItem('refreshToken', response.refreshToken);
    localStorage.setItem('role', response.role);
    if (response.organizationId) {
      localStorage.setItem('organizationId', response.organizationId.toString());
    }
  }

  getAccessToken(): string | null {
    return localStorage.getItem('accessToken');
  }

  getRole(): string | null {
    return localStorage.getItem('role');
  }

  isLoggedIn(): boolean {
    return !!this.getAccessToken();
  }
}