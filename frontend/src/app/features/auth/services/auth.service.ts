import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, switchMap, map, tap } from 'rxjs';
import { LoginResponse, TokenRefreshResponse } from '../models/auth.model';
import { Router } from '@angular/router';
import { CurrentUserService } from '../../users/services/current-user.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly AUTH_URL = 'http://localhost:8081/api/auth';

  private userSubject = new BehaviorSubject<any>(null);

  private http = inject(HttpClient);
  private router = inject(Router);
  private currentUserService = inject(CurrentUserService);

  login(credentials: any): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.AUTH_URL}/login`, credentials).pipe(
      tap(response => {
        this.storeTokens(response);
      }),
      switchMap((response) =>
        this.currentUserService.load(true).pipe(
          map(() => response)
        )
      )
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
    this.currentUserService.clear();
    this.router.navigate(['/auth/login']);
  }

  private storeTokens(response: LoginResponse): void {
    localStorage.setItem('accessToken', response.accessToken);
    localStorage.setItem('refreshToken', response.refreshToken);
    localStorage.setItem('role', response.role);

    if (response.organizationId) {
      localStorage.setItem('organizationId', response.organizationId.toString());
      localStorage.setItem('organizationName', response.organizationName || 'System Admin');
    }
  }

  getUserOrganizationId(): number | null {
    const orgId = localStorage.getItem('organizationId');
    return orgId ? parseInt(orgId, 10) : null;
  }

  getUserOrganizationName(): string | null {
    return localStorage.getItem('organizationName');
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