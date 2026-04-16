import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/config/api.config';
import { CaUser } from '../models/ca-user.model';
import { CreateCaUserRequest } from '../models/create-ca-user-request.model';

@Injectable({
  providedIn: 'root'
})
export class CaUsersService {
  private http = inject(HttpClient);
  private readonly baseUrl = `${API_BASE_URL}/admin/ca-users`;

  getAll(): Observable<CaUser[]> {
    return this.http.get<CaUser[]>(this.baseUrl);
  }

  create(payload: CreateCaUserRequest): Observable<CaUser> {
    return this.http.post<CaUser>(this.baseUrl, payload);
  }
}