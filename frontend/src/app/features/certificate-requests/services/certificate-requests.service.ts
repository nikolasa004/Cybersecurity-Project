import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../../core/config/api.config';

import { CertificateRequest } from '../models/certificate-request.model';
import { CreateCertificateRequest } from '../models/create-certificate-request.model';

@Injectable({
  providedIn: 'root'
})
export class CertificateRequestsService {

  private http = inject(HttpClient);

  private readonly userUrl =
    `${API_BASE_URL}/users/certificate-requests`;

createRequest(
  csrFile: File,
  payload: CreateCertificateRequest
): Observable<CertificateRequest> {

  const formData = new FormData();

  formData.append('csrFile', csrFile);

  const requestBlob = new Blob(
    [
      JSON.stringify({
        issuerCertificateId: payload.issuerCertificateId,
        validFrom: payload.validFrom,
        validTo: payload.validTo
      })
    ],
    { type: 'application/json' }
  );

  formData.append('request', requestBlob);

  return this.http.post<CertificateRequest>(
    this.userUrl,
    formData
  );
}

  getMyRequests(): Observable<CertificateRequest[]> {
    return this.http.get<CertificateRequest[]>(
      `${this.userUrl}`
    );
  }

  private readonly caUrl =
  `${API_BASE_URL}/ca/certificate-requests`;

getPendingRequests(): Observable<CertificateRequest[]> {
  return this.http.get<CertificateRequest[]>(
    `${this.caUrl}/pending`
  );
}

approveRequest(requestId: number) {
  return this.http.post(
    `${this.caUrl}/${requestId}/approve`,
    {}
  );
}

rejectRequest(
  requestId: number,
  rejectionReason: string
) {
  return this.http.post<{ message: string }>(
    `${this.caUrl}/${requestId}/reject`,
    {
      rejectionReason
    }
  );
}
}