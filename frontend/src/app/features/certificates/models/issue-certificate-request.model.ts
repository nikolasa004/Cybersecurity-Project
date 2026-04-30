export interface IssueCertificateRequest {
  type: 'ROOT' | 'INTERMEDIATE' | 'END_ENTITY';
  issuerCertificateId?: number | null;
  organizationId?: number | null;
  commonName: string;
  country?: string | null;
  state?: string | null;
  locality?: string | null;
  organization?: string | null;
  organizationalUnit?: string | null;
  email?: string | null;
  subjectAlternativeName: string | null;
  validFrom: string;
  validTo: string;
  ca: boolean;
  keyCertSign: boolean;
  digitalSignature: boolean;
  keyEncipherment: boolean;
}