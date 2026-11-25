import type { Rol } from './tipos'

// Las cuentas demo tienen varios roles (admin = USER + ADMIN). La navbar y las acciones
// se deciden con el rol "más alto" (PRD Frontend §8.4 y §12.4): ADMIN > STAFF > USER
export function rolPrincipal(roles: readonly Rol[]): Rol {
  if (roles.includes('ADMIN')) return 'ADMIN'
  if (roles.includes('STAFF')) return 'STAFF'
  return 'USER'
}
