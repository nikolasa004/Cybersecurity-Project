export interface CertificateListItem {
  id: number;
  serialNumber: string;
  type: 'ROOT' | 'INTERMEDIATE' | 'END_ENTITY';
  status: 'ACTIVE' | 'REVOKED' | 'EXPIRED';
  subjectCommonName: string | null;
  subjectOrganization: string | null;
  issuerCommonName: string | null;
  organizationName: string | null;
  ca: boolean;
  validFrom: string;
  validTo: string;
  signatureAlgorithm: string | null;
  certificateData: string | null;
  revocationReason: string | null;
  revokedAt: string | null;
}