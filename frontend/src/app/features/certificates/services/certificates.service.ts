import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/config/api.config';
import { CertificateListItem } from '../models/certificate-list.model';
import { IssuerOption } from '../models/issuer-option.model';
import { IssueCertificateRequest } from '../models/issue-certificate-request.model';
import { RevokeCertificateRequest } from '../models/revoke-certificate-request.model';
import { OcspStatusResponse } from '../models/ocsp-status.model';
import { CrlResponse } from '../models/crl.model';

@Injectable({
  providedIn: 'root'
})
export class CertificatesService {
  private http = inject(HttpClient);

  private readonly adminUrl = `${API_BASE_URL}/admin/certificates`;
  private readonly caUrl = `${API_BASE_URL}/ca/certificates`;
  private readonly userUrl = `${API_BASE_URL}/user/certificates`;
  private readonly revocationUrl = `${API_BASE_URL}/revocation`;

  getAll(isAdmin: boolean): Observable<CertificateListItem[]> {
    const url = isAdmin ? this.adminUrl : this.caUrl;
    return this.http.get<CertificateListItem[]>(url);
  }

  getAvailableIssuers(isAdmin: boolean, targetType?: string): Observable<IssuerOption[]> {
    const url = isAdmin ? `${this.adminUrl}/issuers` : `${this.caUrl}/issuers`;
    const params: any = {};
    if (targetType) {
      params.targetType = targetType;
    }

    return this.http.get<IssuerOption[]>(url, { params });
  }

  issue(payload: IssueCertificateRequest, isAdmin: boolean): Observable<CertificateListItem> {
    const url = isAdmin ? `${this.adminUrl}/issue` : `${this.caUrl}/issue`;
    return this.http.post<CertificateListItem>(url, payload);
  }

  getUserCertificates(): Observable<CertificateListItem[]> {
    return this.http.get<CertificateListItem[]>(this.userUrl);
  }

  revokeAsAdmin(certificateId: number, payload: RevokeCertificateRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.adminUrl}/revoke/${certificateId}`, payload);
  }

  revokeAsCa(certificateId: number, payload: RevokeCertificateRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.caUrl}/revoke/${certificateId}`, payload);
  }

  revokeAsUser(certificateId: number, payload: RevokeCertificateRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.userUrl}/revoke/${certificateId}`, payload);
  }

  getOcspStatus(serialNumber: string): Observable<OcspStatusResponse> {
    return this.http.get<OcspStatusResponse>(`${this.revocationUrl}/ocsp/${serialNumber}`);
  }

  getCrl(issuerSerialNumber: string): Observable<CrlResponse> {
    return this.http.get<CrlResponse>(`${this.revocationUrl}/crl/${issuerSerialNumber}`);
  }

  downloadWithKey(isAdmin: boolean, certificateId: number, password: string): Observable<ArrayBuffer> {
    const base = isAdmin ? this.adminUrl : this.caUrl;

    return this.http.post(
      `${base}/download-with-key/${certificateId}`,
      { password },
      { responseType: 'arraybuffer' }
    );
  }

  downloadPem(
    certificateId: number,
    role: 'ADMIN' | 'CA_USER' | 'USER'
  ): Observable<{ pemContent: string; fileName: string }> {
    let url = `${this.userUrl}/download-pem/${certificateId}`;

    if (role === 'ADMIN') {
      url = `${this.adminUrl}/download-pem/${certificateId}`;
    } else if (role === 'CA_USER') {
      url = `${this.caUrl}/download-pem/${certificateId}`;
    }

    return this.http.get<{ pemContent: string; fileName: string }>(url);
  }
}