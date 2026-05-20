import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/config/api.config';
import { UserProfile } from '../models/user-profile.model';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private http = inject(HttpClient);
  private readonly baseUrl = `${API_BASE_URL}/users`;

  getCurrentUserProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.baseUrl}/me`);
  }

  get2faSetup(): Observable<{ secretKey: string, qrCodeUrl: string }> {
    return this.http.get<{ secretKey: string, qrCodeUrl: string }>(`${this.baseUrl}/me/2fa/setup`);
  }

  enable2fa(code: string): Observable<string> {
    return this.http.post<string>(`${this.baseUrl}/me/2fa/enable`, null, {
      params: { code },
      responseType: 'text' as 'json'
    });
  }

  disable2fa(): Observable<string> {
    return this.http.post<string>(`${this.baseUrl}/me/2fa/disable`, null, { 
      responseType: 'text' as 'json' 
    });
  }
}