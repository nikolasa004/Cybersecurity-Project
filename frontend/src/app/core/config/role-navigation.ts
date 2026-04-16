import { NavigationItem } from '../models/navigation-item';
import { UserRole } from '../models/user-role';

export const NAVIGATION_BY_ROLE: Record<UserRole, NavigationItem[]> = {
  ADMIN: [
    { label: 'Dashboard', route: '/admin' },
    { label: 'Organizations', route: '/admin/organizations' },
    { label: 'CA Users', route: '/admin/ca-users' },
    { label: 'Certificates', route: '/admin/certificates' },
    { label: 'Issue Certificate', route: '/admin/certificates/issue' },
    { label: 'Revocation', route: '/admin/revocation' },
    { label: 'Profile', route: '/admin/profile' }
  ],
  CA_USER: [
    { label: 'Dashboard', route: '/ca' },
    { label: 'My Organization', route: '/ca/organization' },
    { label: 'Certificates', route: '/ca/certificates' },
    { label: 'Issue Certificate', route: '/ca/certificates/issue' },
    { label: 'Downloads', route: '/ca/downloads' },
    { label: 'Profile', route: '/ca/profile' }
  ],
  USER: [
    { label: 'Dashboard', route: '/user' },
    { label: 'Request Certificate', route: '/user/request-certificate' },
    { label: 'Upload CSR', route: '/user/upload-csr' },
    { label: 'My Certificates', route: '/user/certificates' },
    { label: 'Download', route: '/user/downloads' },
    { label: 'Revoke Certificate', route: '/user/revoke' },
    { label: 'Profile', route: '/user/profile' }
  ]
};