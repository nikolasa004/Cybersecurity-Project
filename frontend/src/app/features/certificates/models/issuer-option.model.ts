export interface IssuerOption {
  id: number;
  label: string;
  type: 'ROOT' | 'INTERMEDIATE' | 'END_ENTITY';
  serialNumber: string;
  validTo: string;
}