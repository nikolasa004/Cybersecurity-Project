import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/config/api.config';
import { CertificateListItem } from '../models/certificate-list.model';
import { IssuerOption } from '../models/issuer-option.model';
import { IssueCertificateRequest } from '../models/issue-certificate-request.model';

@Injectable({
  providedIn: 'root'
})
export class CertificatesService {
  private http = inject(HttpClient);
  private readonly baseUrl = `${API_BASE_URL}/admin/certificates`;

  getAll(): Observable<CertificateListItem[]> {
    return this.http.get<CertificateListItem[]>(this.baseUrl);
  }

  getAvailableIssuers(targetType: 'ROOT' | 'INTERMEDIATE' | 'END_ENTITY'): Observable<IssuerOption[]> {
    return this.http.get<IssuerOption[]>(`${this.baseUrl}/issuers`, {
      params: { targetType }
    });
  }

  issue(payload: IssueCertificateRequest): Observable<CertificateListItem> {
    return this.http.post<CertificateListItem>(`${this.baseUrl}/issue`, payload);
  }
}