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
  private readonly adminUrl = `${API_BASE_URL}/admin/certificates`;
  private readonly caUrl = `${API_BASE_URL}/ca/certificates`;

  getAll(isAdmin: boolean): Observable<CertificateListItem[]> {
    const url = isAdmin ? this.adminUrl : this.caUrl;
    return this.http.get<CertificateListItem[]>(url);
  }

  getAvailableIssuers(isAdmin: boolean, targetType?: string): Observable<IssuerOption[]> {
    const url = isAdmin ? `${this.adminUrl}/issuers` : `${this.caUrl}/issuers`;
    const params: any = {};
    if (targetType) params.targetType = targetType;
    
    return this.http.get<IssuerOption[]>(url, { params });
  }

  issue(payload: IssueCertificateRequest, isAdmin: boolean): Observable<CertificateListItem> {
    const url = isAdmin ? `${this.adminUrl}/issue` : `${this.caUrl}/issue`;
    return this.http.post<CertificateListItem>(url, payload);
  }

  getUserCertificates(): Observable<CertificateListItem[]> {
    return this.http.get<CertificateListItem[]>(
      `${API_BASE_URL}/user/certificates`
    );
  }

  downloadWithKey(isAdmin: boolean,
     certificateId: number,
  password: string): Observable<ArrayBuffer> {
    const base = isAdmin ? this.adminUrl : this.caUrl;

    return this.http.post(
      `${base}/download-with-key/${certificateId}`,
      { password},
      { responseType: 'arraybuffer' }
    );
  }
}