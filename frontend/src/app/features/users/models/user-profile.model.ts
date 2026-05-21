export interface UserProfile {
  id: number;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  role: 'ADMIN' | 'CA_USER' | 'USER';
  organizationId: number | null;
  organizationName: string | null;
  twoFactorEnabled: boolean;
}