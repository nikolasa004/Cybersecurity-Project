export interface OcspStatusResponse {
  serialNumber: string;
  status: 'GOOD' | 'REVOKED' | 'UNKNOWN';
  revokedAt: string | null;
  revocationReason: string | null;
}