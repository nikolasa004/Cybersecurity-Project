import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/config/api.config';
import { Organization } from '../models/organization.model';
import { CreateOrganizationRequest } from '../models/create-organization-request.model';

@Injectable({
  providedIn: 'root'
})
export class OrganizationsService {
  private http = inject(HttpClient);
  private readonly baseUrl = `${API_BASE_URL}/admin/organizations`;

  getAll(): Observable<Organization[]> {
    return this.http.get<Organization[]>(this.baseUrl);
  }

  create(payload: CreateOrganizationRequest): Observable<Organization> {
    return this.http.post<Organization>(this.baseUrl, payload);
  }
}