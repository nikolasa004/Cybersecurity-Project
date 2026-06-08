export interface CrlEntryResponse {
  serialNumber: string;
  revocationReason: string | null;
  revokedAt: string | null;
}

export interface CrlResponse {
  issuerSerialNumber: string;
  revokedCertificates: CrlEntryResponse[];
}