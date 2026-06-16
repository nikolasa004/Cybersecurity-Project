import { NavigationItem } from '../models/navigation-item';
import { UserRole } from '../models/user-role';

export const NAVIGATION_BY_ROLE: Record<UserRole, NavigationItem[]> = {
  ADMIN: [
    { label: 'Dashboard', route: '/admin' },
    { label: 'Organizations', route: '/admin/organizations' },
    { label: 'CA Users', route: '/admin/ca-users' },
    { label: 'Certificates', route: '/admin/certificates' },
    { label: 'Issue Certificate', route: '/admin/certificates/issue' },
    { label: 'Revocation', route: '/admin/revocation-tools' }
  ],
  CA_USER: [
    { label: 'Dashboard', route: '/ca' },
    { label: 'Certificates', route: '/ca/certificates' },
    { label: 'Issue Certificate', route: '/ca/certificates/issue' },
    { label: 'Certificate Requests', route: '/ca/certificate-requests' },
    { label: 'Downloads', route: '/ca/downloads' },
    { label: 'Revocation', route: '/ca/revocation-tools' }
  ],
  USER: [
    { label: 'Dashboard', route: '/user' },
    { label: 'Request Certificate', route: '/user/request-certificate' },
    { label: 'Upload CSR', route: '/user/upload-csr' },
    { label: 'My Certificates', route: '/user/certificates' },
    { label: 'Download', route: '/user/downloads' },
    { label: 'Revocation', route: '/user/revocation-tools' }
  ]
};