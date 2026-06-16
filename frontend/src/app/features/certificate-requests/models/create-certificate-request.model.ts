export interface CreateCertificateRequest {
  issuerCertificateId: number;
  validFrom: string;
  validTo: string;
}